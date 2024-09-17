package com.vesanieminen.froniusvisualizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }

    public static  class PricesWithVAT {
        private List<PriceWithVAT> prices;
        public PricesWithVAT(List<PriceWithVAT> list) {
            this.prices = list;
        }

        public List<PriceWithVAT> getPrices() {
            return prices;
        }

        public void setPrices(List<PriceWithVAT> prices) {
            this.prices = prices;
        }
    }

    @GetMapping(value = "/prices.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getPrices() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(NordpoolSpotService.getLatest7DaysList());
    }

}
