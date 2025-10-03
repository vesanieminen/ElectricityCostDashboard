package com.vesanieminen.froniusvisualizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataEnd;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataStart;

@RestController()
@RequestMapping("/api")
public class RestService {

    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    public static final ZoneId utcZoneID = ZoneId.of("UTC");
    private static final DateTimeFormatter utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(utcZoneID);
    public static final Double vat24Value = 1.24d;

    private PricesWithVAT prices;
    private Instant lastRefresh;
    private String jsonStringData;
    private int todayDayOfYear;

    @GetMapping(value = "/todaysPrices.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getTodaysPrices() throws Exception {

        // Check hourly cache
        if (lastRefresh != null
                && lastRefresh.isAfter(Instant.now().minusSeconds(3600))
                && jsonStringData != null) {
            return jsonStringData;
        }

        // Update prices
        updateTodayPrices();
        ObjectMapper mapper = new ObjectMapper();
        this.jsonStringData = mapper.writeValueAsString(this.prices);

        return jsonStringData;
    }

    public void updateTodayPrices() {
        // Store timestamp and today
        this.lastRefresh = Instant.now();
        this.todayDayOfYear = Instant.now().atZone(fiZoneID).toLocalDateTime().getDayOfYear();

        // Fetch prices
        List<NordpoolPrice> days = NordpoolSpotService.getLatest7DaysList();
        List<PriceWithVAT> list = days.stream()
                .filter(np -> isTodayInFinland(np.timeInstant()))
                .map(PriceWithVAT::new).collect(Collectors.toList());
        this.prices = new PricesWithVAT(list);
    }

    private boolean isTodayInFinland(Instant instant) {
        return instant.atZone(fiZoneID).toLocalDate().getDayOfYear() == todayDayOfYear;
    }

    @Setter
    @Getter
    public static class PriceWithVAT {
        private double price;
        private String startDate;
        private String endDate;

        public PriceWithVAT(double priceWithVAT, String startDateUTC, String endDateUTC) {
            this.price = priceWithVAT;
            this.startDate = startDateUTC;
            this.endDate = endDateUTC;
        }

        public PriceWithVAT(NordpoolPrice np) {
            this(np.price * vat24Value,
                    utcFormatter.format(np.timeInstant()),
                    utcFormatter.format(np.timeInstant().plusSeconds(3600)));
        }

    }

    @Setter
    @Getter
    public static class PricesWithVAT {
        private List<PriceWithVAT> prices;

        public PricesWithVAT(List<PriceWithVAT> list) {
            this.prices = list;
        }

    }

    @GetMapping(value = "/prices.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getPrices() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(NordpoolSpotService.getLatest7DaysList());
    }

    // Define the request DTO
    @Setter
    @Getter
    public static class CalculationRequest {
        private LinkedHashMap<Long, Double> consumptionData;
        private double margin;
        private boolean vat;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class CalculationResponse {
        private double totalCost;
        private double averagePrice;
    }

    // Implement the REST API endpoint
    @PostMapping(value = "/calculateCost", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CalculationResponse calculateCost(@RequestBody CalculationRequest request) {
        LinkedHashMap<Instant, Double> consumptionData = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : request.getConsumptionData().entrySet()) {
            Instant instant = Instant.ofEpochMilli(entry.getKey());
            consumptionData.put(instant, entry.getValue());
        }

        var result = calculateSpotElectricityPriceDetails(consumptionData, request.getMargin(), request.isVat(), false);
        final var totalCost = new BigDecimal(result.totalCost).setScale(2, RoundingMode.HALF_UP).doubleValue();
        final var weightedAverage = totalCost / result.totalConsumption * 100;
        return new CalculationResponse(result.totalCost, weightedAverage);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ValidCalculationRange {
        private Long start;
        private Long end;
    }

    @PostMapping(value = "/validCalculationRange", produces = MediaType.APPLICATION_JSON_VALUE)
    public ValidCalculationRange validCalculationRange() {
        return new ValidCalculationRange(spotDataStart.toEpochMilli(), spotDataEnd.toEpochMilli());
    }

}
