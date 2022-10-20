package com.vesanieminen.froniusvisualizer.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.numberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.sum;

public class PriceCalculatorService {

    public static final String spotPriceDataFile = "src/main/resources/data/sahko.tk/chart.csv";
    public static final String fingridConsumptionDataFile = "src/main/resources/data/fingrid/consumption.csv";

    public static final DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static LinkedHashMap<LocalDateTime, Double> spotPriceMap;
    private static Double spotAverageThisYear;
    private static Double spotAverageThisMonth;

    public static LinkedHashMap<LocalDateTime, Double> getSpotData() throws IOException {
        if (spotPriceMap == null) {
            spotPriceMap = new LinkedHashMap<>();
            final var reader = Files.newBufferedReader(Path.of(spotPriceDataFile));
            final var csvReader = new CSVReader(reader);
            csvReader.readNext(); // skip header
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                final var dateTime = LocalDateTime.parse(line[0], datetimeFormatter);
                spotPriceMap.put(dateTime, Double.valueOf(line[1]));
            }
        }
        return spotPriceMap;
    }

    public static ConsumptionData getFingridConsumptionData(String filePath) throws IOException, ParseException {
        final var consumptionData = new ConsumptionData();
        var start = LocalDateTime.MAX;
        var end = LocalDateTime.MIN;
        final var map = new LinkedHashMap<LocalDateTime, Double>();
        final var reader = Files.newBufferedReader(Path.of(filePath));
        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();
        String[] line;
        while ((line = csvReader.readNext()) != null) {
            final var instant = Instant.parse(line[4]);
            final var fiLocalDateTime = LocalDateTime.ofInstant(instant, fiZoneID);
            map.put(fiLocalDateTime, numberFormat.parse(line[5]).doubleValue());
            if (start.isAfter(fiLocalDateTime)) {
                start = fiLocalDateTime;
            }
            if (end.isBefore(fiLocalDateTime)) {
                end = fiLocalDateTime;
            }
        }
        consumptionData.data = map;
        consumptionData.start = start;
        consumptionData.end = end;
        return consumptionData;
    }

    public static class ConsumptionData {
        public LinkedHashMap<LocalDateTime, Double> data;
        public LocalDateTime start;
        public LocalDateTime end;
    }

    public static double calculateSpotAveragePrice(LinkedHashMap<LocalDateTime, Double> spotData) {
        return spotData.values().stream().reduce(0d, Double::sum) / spotData.values().size();
    }

    public static double calculateSpotAveragePriceThisYear() throws IOException {
        if (spotAverageThisYear == null) {
            spotAverageThisYear = getSpotData().entrySet().stream().filter(item -> item.getKey().getYear() == getCurrentTimeWithHourPrecision().getYear()).map(Map.Entry::getValue).reduce(0d, Double::sum) / getSpotData().values().size();
        }
        return spotAverageThisYear;
    }

    public static double calculateSpotAveragePriceThisMonth() throws IOException {
        final var now = getCurrentTimeWithHourPrecision();
        final var month = now.getMonthValue();
        final var year = now.getYear();
        return getSpotData().entrySet().stream().filter(monthFilter(month, year)).map(Map.Entry::getValue).reduce(0d, Double::sum) / getSpotData().entrySet().stream().filter(monthFilter(month, year)).count();
    }

    private static Predicate<Map.Entry<LocalDateTime, Double>> monthFilter(int month, int year) {
        return item -> item.getKey().getMonthValue() == month && item.getKey().getYear() == year;
    }

    public static double calculateSpotElectricityPrice(LinkedHashMap<LocalDateTime, Double> spotData, LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double margin) {
        return fingridConsumptionData.keySet().stream().filter(spotData::containsKey).map(item -> (spotData.get(item) + margin) * fingridConsumptionData.get(item)).reduce(0d, Double::sum) / 100;
    }

    public static SpotCalculation calculateSpotElectricityPriceDetails(LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double margin) throws IOException {
        final var spotData = getSpotData();
        final var spotCalculation = fingridConsumptionData.keySet().stream().filter(spotData::containsKey)
                .map(item -> new SpotCalculation(
                        spotData.get(item) + margin,
                        (spotData.get(item) + margin) * fingridConsumptionData.get(item),
                        spotData.get(item) * fingridConsumptionData.get(item),
                        fingridConsumptionData.get(item),
                        item,
                        item,
                        new HourValue(item.getHour(), fingridConsumptionData.get(item)),
                        new HourValue(item.getHour(), (spotData.get(item) + margin) * fingridConsumptionData.get(item) / 100)
                ))
                .reduce(new SpotCalculation(
                        0,
                        0,
                        0,
                        0,
                        LocalDateTime.MAX,
                        LocalDateTime.MIN,
                        HourValue.Zero(),
                        HourValue.Zero()
                ), (i1, i2) -> new SpotCalculation(
                        i1.totalPrice + i2.totalPrice,
                        i1.totalCost + i2.totalCost,
                        i1.totalCostWithoutMargin + i2.totalCostWithoutMargin,
                        i1.totalConsumption + i2.totalConsumption,
                        i1.start.compareTo(i2.start) < 0 ? i1.start : i2.start,
                        i1.end.compareTo(i2.end) > 0 ? i1.end : i2.end,
                        sum(i1.consumptionHours, i2.consumptionHours),
                        sum(i1.costHours, i2.costHours)
                ));
        final var count = fingridConsumptionData.keySet().stream().filter(spotData::containsKey).count();
        spotCalculation.averagePrice = spotCalculation.totalPrice / count;
        spotCalculation.totalCost = spotCalculation.totalCost / 100;
        spotCalculation.totalCostWithoutMargin = spotCalculation.totalCostWithoutMargin / 100;
        return spotCalculation;
    }

    public static SpotCalculation calculateSpotElectricityPriceDetails(LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double margin, LocalDateTime start, LocalDateTime end) throws IOException {
        final LinkedHashMap<LocalDateTime, Double> filtered = getDateTimeRange(fingridConsumptionData, start, end);
        return calculateSpotElectricityPriceDetails(filtered, margin);
    }

    private static LinkedHashMap<LocalDateTime, Double> getDateTimeRange(LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, LocalDateTime start, LocalDateTime end) {
        final var filtered = fingridConsumptionData.entrySet().stream().filter(item ->
                (start.isBefore(item.getKey()) || start.isEqual(item.getKey())) &&
                        (end.isAfter(item.getKey()) || end.isEqual(item.getKey()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
        return filtered;
    }

    public static double calculateFixedElectricityPrice(LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double fixed) {
        return fingridConsumptionData.keySet().stream().map(item -> fixed * fingridConsumptionData.get(item)).reduce(0d, Double::sum) / 100;
    }

    public static double calculateFixedElectricityPrice(LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double fixed, LocalDateTime start, LocalDateTime end) {
        final LinkedHashMap<LocalDateTime, Double> filtered = getDateTimeRange(fingridConsumptionData, start, end);
        return calculateFixedElectricityPrice(filtered, fixed);
    }

    public static class SpotCalculation {
        public double totalPrice;
        public double totalCost;
        public double totalCostWithoutMargin;
        public double totalConsumption;
        public double averagePrice;
        public LocalDateTime start;
        public LocalDateTime end;
        public double[] consumptionHours = new double[24];
        public double[] costHours = new double[24];

        public SpotCalculation(double totalPrice, double totalCost, double totalCostWithoutMargin, double totalConsumption, LocalDateTime start, LocalDateTime end) {
            this.totalPrice = totalPrice;
            this.totalCost = totalCost;
            this.totalCostWithoutMargin = totalCostWithoutMargin;
            this.totalConsumption = totalConsumption;
            this.start = start;
            this.end = end;
        }

        public SpotCalculation(double totalPrice, double totalCost, double totalCostWithoutMargin, double totalConsumption, LocalDateTime start, LocalDateTime end, HourValue consumption, HourValue cost) {
            this(totalPrice, totalCost, totalCostWithoutMargin, totalConsumption, start, end);
            consumptionHours[consumption.hour] = consumption.value;
            costHours[cost.hour] = cost.value;
        }

        public SpotCalculation(double totalPrice, double totalCost, double totalCostWithoutMargin, double totalConsumption, LocalDateTime start, LocalDateTime end, double[] consumptionHours, double[] costHours) {
            this(totalPrice, totalCost, totalCostWithoutMargin, totalConsumption, start, end);
            this.consumptionHours = consumptionHours;
            this.costHours = costHours;
        }

    }

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

}
