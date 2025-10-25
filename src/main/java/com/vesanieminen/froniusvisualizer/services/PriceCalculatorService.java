package com.vesanieminen.froniusvisualizer.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.services.PakastinSpotService.mapToResponse;
import static com.vesanieminen.froniusvisualizer.services.PakastinSpotService.pakastin15MinFile;
import static com.vesanieminen.froniusvisualizer.services.PakastinSpotService.pakastin60MinFile;
import static com.vesanieminen.froniusvisualizer.util.Utils.dayFilter;
import static com.vesanieminen.froniusvisualizer.util.Utils.divide;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.getVAT;
import static com.vesanieminen.froniusvisualizer.util.Utils.isAfter;
import static com.vesanieminen.froniusvisualizer.util.Utils.isBefore;
import static com.vesanieminen.froniusvisualizer.util.Utils.isBetweenHours;
import static com.vesanieminen.froniusvisualizer.util.Utils.isBetweenNovAndMar;
import static com.vesanieminen.froniusvisualizer.util.Utils.isMondayToSaturday;
import static com.vesanieminen.froniusvisualizer.util.Utils.monthFilter;
import static com.vesanieminen.froniusvisualizer.util.Utils.nordpoolZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.numberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.sum;

@Slf4j
public class PriceCalculatorService {

    private static final Instant quarterPriceInstant = Instant.parse("2025-09-30T21:00:00Z");
    // 15min data
    private static LinkedHashMap<Instant, Double> spotPriceMap;
    private static List<NordpoolPrice> nordpoolPriceList;
    public static Instant spotDataStart;
    public static Instant spotDataEnd;
    // hour data
    private static LinkedHashMap<Instant, Double> spotPriceMap_60min;
    private static List<NordpoolPrice> nordpoolPriceList_60min;
    public static Instant spotDataStart_60min;
    public static Instant spotDataEnd_60min;
    @Getter
    public static List<MonthData> monthlyPrices;
    @Getter
    public static Map<YearMonth, Double> averagePriceMap = new HashMap<>();

    // 15 min methods
    public static LinkedHashMap<Instant, Double> getSpotData() {
        if (spotPriceMap == null) {
            spotPriceMap = updateSpotData();
        }
        return spotPriceMap;
    }

    public static LinkedHashMap<Instant, Double> updateSpotData() {
        return readSpotFileAndUpdateSpotData(pakastin15MinFile);
    }

    public static LinkedHashMap<Instant, Double> readSpotFileAndUpdateSpotData(String filename) {
        spotPriceMap = new LinkedHashMap<>();
        final String file;
        try {
            file = Files.readString(Path.of(filename));
        } catch (IOException e) {
            log.error("Could not load the spot price file", e);
            throw new RuntimeException(e);
        }
        final var pakastinResponse = mapToResponse(file);
        pakastinResponse.prices.forEach(price -> spotPriceMap.put(price.date, price.value / 10));
        spotDataStart = pakastinResponse.prices.getFirst().date;
        spotDataEnd = pakastinResponse.prices.getLast().date;
        log.info("updated spot data");
        //log.info("size of pakastin map: " + sizeOf(spotPriceMap));
        updateNordPoolPriceList();
        updateMonthlyPrices();
        return spotPriceMap;
    }

    public static void updateNordPoolPriceList() {
        if (spotPriceMap != null) {
            nordpoolPriceList = spotPriceMap.entrySet().stream().map(item -> new NordpoolPrice(item.getValue() * getVAT(item.getKey()), item.getKey().toEpochMilli())).toList();
        }
    }

    // 60 min methods
    public static LinkedHashMap<Instant, Double> getSpotData_60min() {
        if (spotPriceMap_60min == null) {
            spotPriceMap_60min = updateSpotData_60min();
        }
        return spotPriceMap_60min;
    }

    public static LinkedHashMap<Instant, Double> updateSpotData_60min() {
        return readSpotFileAndUpdateSpotData_60min(pakastin60MinFile);
    }

    public static LinkedHashMap<Instant, Double> readSpotFileAndUpdateSpotData_60min(String filename) {
        spotPriceMap_60min = new LinkedHashMap<>();
        final String file;
        try {
            file = Files.readString(Path.of(filename));
        } catch (IOException e) {
            log.error("Could not load the spot price file", e);
            throw new RuntimeException(e);
        }
        final var pakastinResponse = mapToResponse(file);
        pakastinResponse.prices.forEach(price -> spotPriceMap_60min.put(price.date, price.value / 10));
        spotDataStart_60min = pakastinResponse.prices.getFirst().date;
        spotDataEnd_60min = pakastinResponse.prices.getLast().date;
        log.info("updated 60 min spot data");
        updateNordPoolPriceList_60min();
        return spotPriceMap_60min;
    }

    public static void updateNordPoolPriceList_60min() {
        if (spotPriceMap_60min != null) {
            nordpoolPriceList_60min = spotPriceMap_60min.entrySet().stream().map(item -> new NordpoolPrice(item.getValue() * getVAT(item.getKey()), item.getKey().toEpochMilli())).toList();
        }
    }

    public static FingridUsageData getFingridUsageData(String filePath) throws IOException, ParseException, CsvValidationException {
        var start = Instant.MAX;
        var end = Instant.MIN;
        final var map = new LinkedHashMap<Instant, Double>();
        final var reader = Files.newBufferedReader(Path.of(filePath));
        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();

        String[] header = csvReader.readNext();
        boolean isNewFormat = header.length == 8;

        String[] line;
        while ((line = csvReader.readNext()) != null) {
            // in case the Fingrid csv data has rows that contain: "null;MISSING", skip them
            if ("MISSING".equals(line[getColumnIndex(isNewFormat, 6)])) {
                break;
            }
            final var instant = Instant.parse(line[getColumnIndex(isNewFormat, 4)]);
            if (isNewFormat) {
                // On 2023-01-16 Fingrid changed from . to , for the comma separator
                if (line[6].contains(".")) {
                    map.put(instant, Double.parseDouble(line[6]));
                } else {
                    map.put(instant, numberFormat.parse(line[6]).doubleValue());
                }
            } else {
                map.put(instant, numberFormat.parse(line[5]).doubleValue());
            }
            if (start.isAfter(instant)) {
                start = instant;
            }
            if (end.isBefore(instant)) {
                end = instant;
            }
        }
        return new FingridUsageData(map, start, end);
    }

    public static FingridUsageData getFingridUsageData(InputStream inputStream, boolean useQuarterlyPricePrecision) throws IOException, ParseException, CsvValidationException {
        var start = Instant.MAX;
        var end = Instant.MIN;
        final var map = new LinkedHashMap<Instant, Double>();
        final var reader = new InputStreamReader(inputStream);
        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();

        String[] header = csvReader.readNext();
        boolean isNewFormat = header.length == 8;

        String[] line;
        while ((line = csvReader.readNext()) != null) {
            // in case the Fingrid csv data has rows that contain: "null;MISSING", skip them
            if ("MISSING".equals(line[getColumnIndex(isNewFormat, 6)])) {
                break;
            }
            final var instant = Instant.parse(line[getColumnIndex(isNewFormat, 4)]);
            if (isNewFormat) {
                // On 2023-01-16 Fingrid changed from . to , for the comma separator
                if (line[6].contains(".")) {
                    map.put(instant, Double.parseDouble(line[6]));
                } else {
                    // in case of 15min interval data exists in Fingrid file
                    if ("PT15M".equals(line[2])) {
                        // Quarterly spot prices exist and are wanted
                        if (quarterPriceInstant.compareTo(instant) <= 0 && useQuarterlyPricePrecision) {
                            map.put(instant, numberFormat.parse(line[6]).doubleValue());
                        }
                        // combine 4 x 15 min values into 1h
                        else {
                            final var _15Min = csvReader.readNext();
                            final var _30Min = csvReader.readNext();
                            final var _45Min = csvReader.readNext();
                            // skip if there are no values to combine for a full hour
                            if (_15Min == null || _30Min == null || _45Min == null) {
                                break;
                            }
                            // also skip if one of the 15min interval values is missing
                            if ("MISSING".equals(_15Min[getColumnIndex(true, 6)]) || "MISSING".equals(_30Min[getColumnIndex(isNewFormat, 6)]) || "MISSING".equals(_45Min[getColumnIndex(isNewFormat, 6)])) {
                                break;
                            }
                            final var value00Min = numberFormat.parse(line[6]).doubleValue();
                            final var value15Min = numberFormat.parse(_15Min[6]).doubleValue();
                            final var value30Min = numberFormat.parse(_30Min[6]).doubleValue();
                            final var value45Min = numberFormat.parse(_45Min[6]).doubleValue();
                            map.put(instant, value00Min + value15Min + value30Min + value45Min);
                        }

                    } else {
                        map.put(instant, numberFormat.parse(line[6]).doubleValue());
                    }
                }
            } else {
                map.put(instant, numberFormat.parse(line[5]).doubleValue());
            }
            if (start.isAfter(instant)) {
                start = instant;
            }
            if (end.isBefore(instant)) {
                end = instant;
            }
        }
        reader.close();
        return new FingridUsageData(map, start, end);
    }

    private static int getColumnIndex(boolean isNewFormat, int index) {
        return isNewFormat ? index + 1 : index;
    }

    public record FingridUsageData(LinkedHashMap<Instant, Double> data, Instant start, Instant end) {
    }

    public static boolean is15MinResolution(InputStream inputStream) throws IOException, ParseException, CsvValidationException {
        final var reader = new InputStreamReader(inputStream);
        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .build();

        String[] header = csvReader.readNext();

        String[] line;
        while ((line = csvReader.readNext()) != null) {
            if ("PT15M".equals(line[2])) {
                return true;
            }
        }
        return false;
    }

    public static double calculateSpotAveragePrice(LinkedHashMap<LocalDateTime, Double> spotData) {
        return spotData.values().stream().reduce(0d, Double::sum) / spotData.values().size();
    }

    public static double calculateSpotAveragePriceThisYear() {
        //if (spotAverageThisYear == null) {
        final var year = getCurrentTimeWithHourPrecision().getYear();
        return getSpotData_60min().entrySet().stream().filter(yearFilter(year)).map(item -> item.getValue() * getVAT(item.getKey())).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(yearFilter(year)).count();
        //spotAverageThisYear = getSpotData_60min().entrySet().stream().filter(yearFilter(year)).map(item -> item.getValue() * getVAT(item.getKey())).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(yearFilter(year)).count();
        //}
        //return spotAverageThisYear;
    }

    public static double calculateSpotAveragePriceThisYearWithoutVAT() {
        final var year = getCurrentTimeWithHourPrecision().getYear();
        return getSpotData_60min().entrySet().stream().filter(yearFilter(year)).map(Map.Entry::getValue).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(yearFilter(year)).count();
    }

    private static Predicate<Map.Entry<Instant, Double>> yearFilter(int year) {
        return item -> item.getKey().atZone(fiZoneID).getYear() == year;
    }

    public static List<MonthData> calculateMonthlyPrices() {
        final var startYear = spotDataStart.atZone(fiZoneID).getYear();
        final var startMonth = spotDataStart.atZone(fiZoneID).getMonth().getValue();
        final var endYear = spotDataEnd.atZone(fiZoneID).getYear();
        final var endMonth = spotDataEnd.atZone(fiZoneID).getMonth().getValue();
        final var monthData = new ArrayList<MonthData>();
        for (int year = startYear; year <= endYear; ++year) {
            for (int month = startMonth; year == endYear ? month <= endMonth : month <= 12; ++month) {
                final var average = calculateSpotAveragePriceOnMonth(year, month);
                monthData.add(new MonthData(YearMonth.of(year, month), average));
            }
        }
        return monthData;
    }

    public static void updateMonthlyPrices() {
        if (spotPriceMap != null) {
            monthlyPrices = calculateMonthlyPrices();
            averagePriceMap = calculateMonthlyAveragePriceMap();
        }
    }

    public record MonthData(YearMonth yearMonth, double averagePrice) {
    }

    // New method to create a HashMap<YearMonth, Double> mapping
    public static Map<YearMonth, Double> calculateMonthlyAveragePriceMap() {
        Map<YearMonth, Double> averagePriceMap = new HashMap<>();
        for (MonthData md : monthlyPrices) {
            YearMonth yearMonth = md.yearMonth();
            double average = md.averagePrice();
            averagePriceMap.put(yearMonth, average);
        }
        return averagePriceMap;
    }

    public static double calculateSpotAveragePriceOnMonth(int year, int month) {
        return getSpotData_60min().entrySet().stream().filter(monthFilter(month, year)).map(item -> item.getValue() * getVAT(item.getKey())).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(monthFilter(month, year)).count();
    }

    public static double calculateSpotAveragePriceThisMonth() {
        final var now = getCurrentTimeWithHourPrecision();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(monthFilter(month, year)).map(item -> item.getValue() * getVAT(item.getKey())).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(monthFilter(month, year)).count();
    }

    public static double calculateSpotAveragePriceThisMonthWithoutVAT() {
        final var now = getCurrentTimeWithHourPrecision();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(monthFilter(month, year)).map(Map.Entry::getValue).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(monthFilter(month, year)).count();
    }

    public static double calculateSpotAveragePriceToday() {
        final var now = getCurrentTimeWithHourPrecision();
        final var day = now.getDayOfMonth();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(dayFilter(day, month, year)).map(item -> item.getValue() * getVAT(item.getKey())).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(dayFilter(day, month, year)).count();
    }

    public static double calculateSpotAveragePriceTodayWithoutVAT() {
        final var now = getCurrentTimeWithHourPrecision();
        final var day = now.getDayOfMonth();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(dayFilter(day, month, year)).map(Map.Entry::getValue).reduce(0d, Double::sum) / getSpotData_60min().entrySet().stream().filter(dayFilter(day, month, year)).count();
    }

    public static List<Double> getPricesToday() {
        final var now = getCurrentTimeWithHourPrecision();
        final var day = now.getDayOfMonth();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(dayFilter(day, month, year)).map(item -> item.getValue() * getVAT(item.getKey())).collect(Collectors.toList());
    }

    public static List<Double> getPricesTomorrow() {
        final var now = getCurrentTimeWithHourPrecision();
        final var day = now.getDayOfMonth() + 1;
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(dayFilter(day, month, year)).map(item -> item.getValue() * getVAT(item.getKey())).collect(Collectors.toList());
    }

    public static List<Map.Entry<Instant, Double>> getPriceDataToday() {
        final var now = getCurrentTimeWithHourPrecision();
        final var day = now.getDayOfMonth();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(dayFilter(day, month, year)).map(item -> Map.entry(item.getKey(), item.getValue() * getVAT(item.getKey()))).collect(Collectors.toList());
    }

    public static Set<Map.Entry<Instant, Double>> getPriceDataForMonth() {
        final var now = getCurrentTimeWithHourPrecision();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(monthFilter(month, year)).map(item -> Map.entry(item.getKey(), item.getValue() * getVAT(item.getKey()))).collect(Collectors.toSet());
    }

    public static List<Double> getPricesForYear() {
        final var now = getCurrentTimeWithHourPrecision();
        final var year = now.getYear();
        return getSpotData_60min().entrySet().stream().filter(yearFilter(year)).map(item -> item.getValue() * getVAT(item.getKey())).collect(Collectors.toList());
    }

    public static List<NordpoolPrice> getPrices() {
        return nordpoolPriceList;
    }

    public static List<NordpoolPrice> getPrices_60min() {
        return nordpoolPriceList_60min;
    }

    public static double calculateSpotElectricityPrice(LinkedHashMap<LocalDateTime, Double> spotData, LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double margin) {
        return fingridConsumptionData.keySet().stream().filter(spotData::containsKey).map(item -> (spotData.get(item) + margin) * fingridConsumptionData.get(item)).reduce(0d, Double::sum) / 100;
    }

    public static SpotCalculation calculateSpotElectricityPriceDetails(LinkedHashMap<Instant, Double> fingridConsumptionData, double margin, boolean vat, boolean isQuarterlyPricesEnabled) {
        final var spotData = isQuarterlyPricesEnabled ? getSpotData() : getSpotData_60min();
        final var spotCalculation = calculateSpotCosts(fingridConsumptionData, margin, vat, spotData);
        final var count = fingridConsumptionData.keySet().stream().filter(spotData::containsKey).count();
        spotCalculation.averagePrice = spotCalculation.totalSpotPrice / count;
        spotCalculation.averagePriceWithoutMargin = spotCalculation.totalSpotPriceWithoutMargin / count;
        spotCalculation.totalCost = spotCalculation.totalCost / 100;
        spotCalculation.totalCostWithoutMargin = spotCalculation.totalCostWithoutMargin / 100;
        divide(spotCalculation.spotAveragePerHour, count / 24.0);
        return spotCalculation;
    }

    private static @NotNull SpotCalculation calculateSpotCosts(LinkedHashMap<Instant, Double> fingridConsumptionData, double margin, boolean vat, LinkedHashMap<Instant, Double> spotData) {
        return fingridConsumptionData.keySet().stream().filter(spotData::containsKey)
                .map(item -> new SpotCalculation(
                        spotData.get(item) * getVAT(item, vat) + margin,
                        spotData.get(item) * getVAT(item, vat),
                        (spotData.get(item) * getVAT(item, vat) + margin) * fingridConsumptionData.get(item),
                        spotData.get(item) * getVAT(item, vat) * fingridConsumptionData.get(item),
                        fingridConsumptionData.get(item),
                        item,
                        item,
                        new HourValue(item.atZone(fiZoneID).getHour(), fingridConsumptionData.get(item)),
                        new HourValue(item.atZone(fiZoneID).getHour(), (spotData.get(item) * getVAT(item, vat) + margin) * fingridConsumptionData.get(item) / 100),
                        new HourValue(item.atZone(fiZoneID).getHour(), (spotData.get(item) * getVAT(item, vat)) * fingridConsumptionData.get(item) / 100),
                        new HourValue(item.atZone(fiZoneID).getHour(), spotData.get(item) * getVAT(item, vat))
                ))
                .reduce(new SpotCalculation(
                        0,
                        0,
                        0,
                        0,
                        0,
                        Instant.MAX,
                        Instant.MIN,
                        HourValue.Zero(),
                        HourValue.Zero(),
                        HourValue.Zero(),
                        HourValue.Zero()
                ), (i1, i2) -> new SpotCalculation(
                        i1.totalSpotPrice + i2.totalSpotPrice,
                        i1.totalSpotPriceWithoutMargin + i2.totalSpotPriceWithoutMargin,
                        i1.totalCost + i2.totalCost,
                        i1.totalCostWithoutMargin + i2.totalCostWithoutMargin,
                        i1.totalConsumption + i2.totalConsumption,
                        i1.start.compareTo(i2.start) < 0 ? i1.start : i2.start,
                        i1.end.compareTo(i2.end) > 0 ? i1.end : i2.end,
                        sum(i1.consumptionHours, i2.consumptionHours),
                        sum(i1.costHours, i2.costHours),
                        sum(i1.costHoursWithoutMargin, i2.costHoursWithoutMargin),
                        sum(i1.spotAveragePerHour, i2.spotAveragePerHour)
                ));
    }

    public static boolean isConsumptionDataQuarterlyPrecision(LinkedHashMap<Instant, Double> fingridConsumptionData) {
        if (fingridConsumptionData == null || fingridConsumptionData.isEmpty()) {
            return false;
        }
        return fingridConsumptionData.keySet().stream().anyMatch(item -> item.atZone(fiZoneID).getMinute() != 0);
    }

    public static HashMap<YearMonth, SpotCalculation> calculateSpotElectricityPriceDetailsPerMonth(LinkedHashMap<Instant, Double> fingridConsumptionData, double margin, boolean vat, Instant start, Instant end, boolean isQuarterlyPricesEnabled) {

        final var map = new HashMap<YearMonth, SpotCalculation>();

        if (end.isBefore(start)) {
            return map;
        }

        // Convert start and end Instants to ZonedDateTime for easier manipulation
        ZonedDateTime startDateTime = start.atZone(fiZoneID).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endDateTime = end.atZone(fiZoneID).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);

        ZonedDateTime current = startDateTime;

        while (!current.isAfter(endDateTime)) {

            // Define the start and end of the current month
            Instant monthStart = current.toInstant();
            ZonedDateTime nextMonth = current.plusMonths(1);
            Instant monthEnd = nextMonth.toInstant().minusMillis(1); // End of the current month

            // Filter consumption data for the current month
            LinkedHashMap<Instant, Double> monthConsumptionData = getDateTimeRange(fingridConsumptionData, monthStart, monthEnd);

            // Handle empty data gracefully
            SpotCalculation spotCalculation;
            if (monthConsumptionData.isEmpty()) {
                spotCalculation = new SpotCalculation(0, 0, 0, 0, 0, monthStart, monthEnd);
            } else {
                // Calculate SpotCalculation for the current month
                spotCalculation = calculateSpotElectricityPriceDetails(monthConsumptionData, margin, vat, isQuarterlyPricesEnabled);
            }

            YearMonth yearMonth = YearMonth.from(current);

            map.put(yearMonth, spotCalculation);

            // Move to the next month
            current = nextMonth;
        }

        return map;
    }

    public static SpotCalculation calculateSpotElectricityPriceDetails(LinkedHashMap<Instant, Double> fingridConsumptionData, double margin, boolean vat, Instant start, Instant end, boolean isQuarterlyPriceEnabled) throws IOException {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRange(fingridConsumptionData, start, end);
        return calculateSpotElectricityPriceDetails(filtered, margin, vat, isQuarterlyPriceEnabled);
    }

    private static LinkedHashMap<Instant, Double> getDateTimeRange(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end) {
        if (fingridConsumptionData == null)
            return null;
        return fingridConsumptionData.entrySet().stream().filter(item ->
                (start.compareTo(item.getKey()) <= 0 && 0 <= end.compareTo(item.getKey()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    public static double calculateFixedElectricityPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double fixed) {
        return fingridConsumptionData.keySet().stream().map(item -> fixed * fingridConsumptionData.get(item)).reduce(0d, Double::sum) / 100;
    }

    public static double calculateFixedElectricityPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double fixed, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRange(fingridConsumptionData, start, end);
        return calculateFixedElectricityPrice(filtered, fixed);
    }

    public static double calculateElectricityTaxPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double fixed) {
        return fingridConsumptionData.keySet().stream().map(item -> fixed * getVAT(item) * fingridConsumptionData.get(item)).reduce(0d, Double::sum) / 100;
    }

    public static double calculateElectricityTaxPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double fixed, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRange(fingridConsumptionData, start, end);
        return calculateElectricityTaxPrice(filtered, fixed);
    }

    public static double calculateConsumption(LinkedHashMap<Instant, Double> fingridConsumptionData) {
        return fingridConsumptionData.values().stream().reduce(0d, Double::sum);
    }

    // night transfer

    public static double calculateDayConsumption(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeBetweenHoursFilter(fingridConsumptionData, start, end, 7, 22);
        return calculateConsumption(filtered);
    }

    public static double calculateNightConsumption(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeOutsideHoursFilter(fingridConsumptionData, start, end, 22, 7);
        return calculateConsumption(filtered);
    }

    private static LinkedHashMap<Instant, Double> getDateTimeRangeBetweenHoursFilter(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end, int hourAfter, int hourBefore) {
        return getDateTimeRange(fingridConsumptionData, start, end).entrySet().stream().filter(isBetweenHours(hourAfter, hourBefore))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    private static LinkedHashMap<Instant, Double> getDateTimeRangeOutsideHoursFilter(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end, int hourAfter, int hourBefore) {
        return getDateTimeRange(fingridConsumptionData, start, end).entrySet().stream().filter(isBefore(hourBefore).or(isAfter(hourAfter)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    public static double calculateDayPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double price, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeBetweenHoursFilter(fingridConsumptionData, start, end, 7, 22);
        return calculateFixedElectricityPrice(filtered, price);
    }

    public static double calculateNightPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double price, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeOutsideHoursFilter(fingridConsumptionData, start, end, 22, 7);
        return calculateFixedElectricityPrice(filtered, price);
    }

    // seasonal transfer

    private static LinkedHashMap<Instant, Double> getDateTimeRangeSeasonalWinterFilter(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end) {
        return getDateTimeRange(fingridConsumptionData, start, end).entrySet().stream().filter(isBetweenNovAndMar().and(isMondayToSaturday().and(isBetweenHours(7, 22))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    private static LinkedHashMap<Instant, Double> getDateTimeRangeSeasonalOtherFilter(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end) {
        return getDateTimeRange(fingridConsumptionData, start, end).entrySet().stream().filter(isBetweenNovAndMar().and(isMondayToSaturday().and(isBetweenHours(7, 22))).negate())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    public static double calculateSeasonalWinterPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double price, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeSeasonalWinterFilter(fingridConsumptionData, start, end);
        return calculateFixedElectricityPrice(filtered, price);
    }

    public static double calculateSeasonalOtherPrice(LinkedHashMap<Instant, Double> fingridConsumptionData, double price, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeSeasonalOtherFilter(fingridConsumptionData, start, end);
        return calculateFixedElectricityPrice(filtered, price);
    }

    public static double calculateSeasonalWinterConsumption(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeSeasonalWinterFilter(fingridConsumptionData, start, end);
        return calculateConsumption(filtered);
    }

    public static double calculateSeasonalOtherConsumption(LinkedHashMap<Instant, Double> fingridConsumptionData, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filtered = getDateTimeRangeSeasonalOtherFilter(fingridConsumptionData, start, end);
        return calculateConsumption(filtered);
    }


    // BAAS

    public static ArrayList<Double> calculateFixedElectricityPriceWithPastProductionReduced(LinkedHashMap<Instant, Double> fingridConsumptionData, LinkedHashMap<Instant, Double> fingridProductionData, double fixed, Instant start, Instant end) {
        final LinkedHashMap<Instant, Double> filteredConsumption = getDateTimeRange(fingridConsumptionData, start, end);
        final LinkedHashMap<Instant, Double> filteredProduction = getDateTimeRange(fingridProductionData, start, end);

        var energyCost = 0.0;
        var savedProduction = 0.0;
        var excessProduction = 0.0;
        for (var item : filteredConsumption.keySet()) {
            var cost = fixed * filteredConsumption.get(item) / 100;
            energyCost += cost;
            if (filteredProduction != null) {
                var production = fixed * filteredProduction.get(item) / 100;
                excessProduction += production;

                if (excessProduction > 0 && cost > 0) {
                    var saved = Math.min(excessProduction, cost);
                    savedProduction += saved;
                    excessProduction -= saved;
                }
            }
        }

        var retVal = new ArrayList<Double>();

        retVal.add(energyCost);
        retVal.add(savedProduction);
        retVal.add(excessProduction);

        return retVal;
    }


    public static int getLatestDayOfMonth() {
        return getPrices().getLast().timeInstant().atZone(fiZoneID).getDayOfMonth();
    }

    /**
     * Groups together different calculation values.
     * Has multiple arrays that acts as buckets that are filled of data from different days for periods of 24h.
     */
    public static class SpotCalculation {
        public double totalSpotPrice;
        public double totalSpotPriceWithoutMargin;
        public double totalCost;
        public double totalCostWithoutMargin;
        public double totalConsumption;
        public double averagePrice;
        public double averagePriceWithoutMargin;
        public Instant start;
        public Instant end;
        public double[] consumptionHours = new double[24];
        public double[] costHours = new double[24];
        public double[] costHoursWithoutMargin = new double[24];
        public double[] spotAveragePerHour = new double[24];
        public CalculationType calculationType;

        public SpotCalculation(double totalSpotPrice, double totalSpotPriceWithoutMargin, double totalCost, double totalCostWithoutMargin, double totalConsumption, Instant start, Instant end) {
            this.totalSpotPrice = totalSpotPrice;
            this.totalSpotPriceWithoutMargin = totalSpotPriceWithoutMargin;
            this.totalCost = totalCost;
            this.totalCostWithoutMargin = totalCostWithoutMargin;
            this.totalConsumption = totalConsumption;
            this.start = start;
            this.end = end;
        }

        public SpotCalculation(double totalSpotPrice, double totalSpotPriceWithoutMargin, double totalCost, double totalCostWithoutMargin, double totalConsumption, Instant start, Instant end, HourValue consumption, HourValue cost, HourValue costWithoutMargin, HourValue spot) {
            this(totalSpotPrice, totalSpotPriceWithoutMargin, totalCost, totalCostWithoutMargin, totalConsumption, start, end);
            consumptionHours[consumption.hour] = consumption.value;
            costHours[cost.hour] = cost.value;
            costHoursWithoutMargin[costWithoutMargin.hour] = costWithoutMargin.value;
            spotAveragePerHour[spot.hour] = spot.value;
        }

        public SpotCalculation(double totalSpotPrice, double totalSpotPriceWithoutMargin, double totalCost, double totalCostWithoutMargin, double totalConsumption, Instant start, Instant end, double[] consumptionHours, double[] costHours, double[] costHoursWithoutMargin, double[] spotAveragePerHour) {
            this(totalSpotPrice, totalSpotPriceWithoutMargin, totalCost, totalCostWithoutMargin, totalConsumption, start, end);
            this.consumptionHours = consumptionHours;
            this.costHours = costHours;
            this.costHoursWithoutMargin = costHoursWithoutMargin;
            this.spotAveragePerHour = spotAveragePerHour;
        }

    }

    @Getter
    public static class HourValue {
        public int hour;
        public double value;

        public HourValue(int hour, double value) {
            this.hour = hour;
            this.value = value;
        }

        public static HourValue Zero() {
            return new HourValue(0, 0);
        }
    }

    enum CalculationType {
        SPOT_15_MIN,
        SPOT_60_MIN
    }

    @Getter
    @Setter
    public static class SpotCalculations {
        public SpotCalculation spot15Min;
        public SpotCalculation spot60Min;
    }

    public static boolean hasBeenUpdatedSuccessfullyToday() {
        if (spotPriceMap == null || spotDataEnd == null) {
            return false;
        }
        final var zonedDateTime = spotDataEnd.atZone(nordpoolZoneID).truncatedTo(ChronoUnit.DAYS);
        final var other = ZonedDateTime.now(nordpoolZoneID).truncatedTo(ChronoUnit.DAYS);
        return zonedDateTime.isAfter(other);
    }

    public static boolean hasBeenUpdatedSuccessfullyYesterday() {
        if (spotPriceMap == null || spotDataEnd == null) {
            return false;
        }
        return spotDataEnd.atZone(nordpoolZoneID).getDayOfMonth() == ZonedDateTime.now(nordpoolZoneID).getDayOfMonth();
    }

    public static boolean hasBeenUpdatedSuccessfullyToday_60min() {
        if (spotPriceMap_60min == null || spotDataEnd_60min == null) {
            return false;
        }
        final var zonedDateTime = spotDataEnd_60min.atZone(nordpoolZoneID).truncatedTo(ChronoUnit.DAYS);
        final var other = ZonedDateTime.now(nordpoolZoneID).truncatedTo(ChronoUnit.DAYS);
        return zonedDateTime.isAfter(other);
    }

    public static boolean hasBeenUpdatedSuccessfullyYesterday_60min() {
        if (spotPriceMap_60min == null || spotDataEnd_60min == null) {
            return false;
        }
        return spotDataEnd_60min.atZone(nordpoolZoneID).getDayOfMonth() == ZonedDateTime.now(nordpoolZoneID).getDayOfMonth();
    }

    public static averageMinMax getHourlyAveragePrices(Instant start, Instant end, boolean vat) {
        Map<Integer, DoubleSummaryStatistics> statsByHour = spotPriceMap.entrySet().stream()
                .filter(item -> !item.getKey().isBefore(start) && item.getKey().isBefore(end))
                .collect(Collectors.groupingBy(
                        item -> item.getKey().atZone(fiZoneID).getHour(),
                        Collectors.summarizingDouble(item -> item.getValue() * getVAT(item.getKey(), vat))
                ));

        final var averagePrices = new Number[24];
        final var minPrices = new Number[24];
        final var maxPrices = new Number[24];

        for (int hour = 0; hour < 24; hour++) {
            DoubleSummaryStatistics stats = statsByHour.get(hour);
            if (stats != null && stats.getCount() > 0) {
                averagePrices[hour] = stats.getAverage();
                minPrices[hour] = stats.getMin();
                maxPrices[hour] = stats.getMax();
            } else {
                averagePrices[hour] = 0.0;
                minPrices[hour] = 0.0;
                maxPrices[hour] = 0.0;
            }
        }

        return new averageMinMax(averagePrices, minPrices, maxPrices);
    }

    public record averageMinMax(Number[] average, Number[] min, Number[] max) {
    }

    public static Map<DayOfWeek, averageMinMax> getHourlyAveragePricesByDay(Instant start, Instant end, boolean vat) {
        Map<DayOfWeek, Map<Integer, DoubleSummaryStatistics>> statsByDayAndHour = spotPriceMap.entrySet().stream()
                .filter(item -> !item.getKey().isBefore(start) && item.getKey().isBefore(end))
                .collect(Collectors.groupingBy(
                        item -> item.getKey().atZone(fiZoneID).getDayOfWeek(),
                        Collectors.groupingBy(
                                item -> item.getKey().atZone(fiZoneID).getHour(),
                                Collectors.summarizingDouble(item -> item.getValue() * getVAT(item.getKey(), vat))
                        )
                ));

        Map<DayOfWeek, averageMinMax> result = new HashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            final var averagePrices = new Number[24];
            final var minPrices = new Number[24];
            final var maxPrices = new Number[24];

            for (int hour = 0; hour < 24; hour++) {
                DoubleSummaryStatistics stats = statsByDayAndHour.getOrDefault(day, new HashMap<>()).get(hour);
                if (stats != null && stats.getCount() > 0) {
                    averagePrices[hour] = stats.getAverage();
                    minPrices[hour] = stats.getMin();
                    maxPrices[hour] = stats.getMax();
                } else {
                    averagePrices[hour] = null;
                    minPrices[hour] = null;
                    maxPrices[hour] = null;
                }
            }

            result.put(day, new averageMinMax(averagePrices, minPrices, maxPrices));
        }
        return result;
    }


    @Getter
    public static class HourValuePerDay {
        private final DayOfWeek dayOfWeek;
        private final Map<Integer, Double> hourlyPrices;

        public HourValuePerDay(DayOfWeek dayOfWeek, Map<Integer, Double> hourlyPrices) {
            this.dayOfWeek = dayOfWeek;
            this.hourlyPrices = hourlyPrices;
        }

        public Double getHourlyPrice(int hour) {
            return hourlyPrices.getOrDefault(hour, 0.0);
        }
    }
}
