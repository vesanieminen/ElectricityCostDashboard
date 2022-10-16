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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.vesanieminen.froniusvisualizer.util.Utils.numberFormat;

public class PriceCalculatorService {

    public static final String spotPriceDataFile = "src/main/resources/data/sahko.tk/chart.csv";
    public static final String fingridConsumptionDataFile = "src/main/resources/data/fingrid/consumption.csv";

    public static final DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static LinkedHashMap<LocalDateTime, Double> spotPriceMap;
    private static Double spotAverage2022;

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

    public static LinkedHashMap<LocalDateTime, Double> getFingridConsumptionData(String filePath) throws IOException, ParseException {
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
            final var utc = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
            map.put(utc, numberFormat.parse(line[5]).doubleValue());
        }
        return map;
    }

    public static double calculateSpotAveragePrice(LinkedHashMap<LocalDateTime, Double> spotData) {
        return spotData.values().stream().reduce(0d, Double::sum) / spotData.values().size();
    }

    public static double calculateSpotAveragePrice2022() throws IOException {
        if (spotAverage2022 == null) {
            spotAverage2022 = getSpotData().entrySet().stream().filter(item -> item.getKey().getYear() == 2022).map(Map.Entry::getValue).reduce(0d, Double::sum) / getSpotData().values().size();
        }
        return spotAverage2022;
    }

    public static double calculateSpotElectricityPrice(LinkedHashMap<LocalDateTime, Double> spotData, LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double margin) {
        return fingridConsumptionData.keySet().stream().filter(spotData::containsKey).map(item -> (spotData.get(item) + margin) * fingridConsumptionData.get(item)).reduce(0d, Double::sum) / 100;
    }

    public static SpotCalculation calculateSpotElectricityPriceDetails(LinkedHashMap<LocalDateTime, Double> spotData, LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double margin) {
        final var spotCalculation = fingridConsumptionData.keySet().stream().filter(spotData::containsKey)
                .map(item -> new SpotCalculation(spotData.get(item) + margin, (spotData.get(item) + margin) * fingridConsumptionData.get(item), spotData.get(item) * fingridConsumptionData.get(item), fingridConsumptionData.get(item)))
                .reduce(new SpotCalculation(0, 0, 0, 0), (i1, i2) -> new SpotCalculation(i1.totalPrice + i2.totalPrice, (i1.totalCost + i2.totalCost), i1.totalCostWithoutMargin + i2.totalCostWithoutMargin, i1.totalConsumption + i2.totalConsumption));
        final var count = fingridConsumptionData.keySet().stream().filter(spotData::containsKey).count();
        spotCalculation.averagePrice = spotCalculation.totalPrice / count;
        spotCalculation.totalCost = spotCalculation.totalCost / 100;
        spotCalculation.totalCostWithoutMargin = spotCalculation.totalCostWithoutMargin / 100;
        return spotCalculation;
    }

    public static double calculateFixedElectricityPrice(LinkedHashMap<LocalDateTime, Double> fingridConsumptionData, double fixed) {
        return fingridConsumptionData.keySet().stream().map(item -> fixed * fingridConsumptionData.get(item)).reduce(0d, Double::sum) / 100;
    }

    public static class SpotCalculation {
        public double totalPrice;
        public double totalCost;
        public double totalCostWithoutMargin;
        public double totalConsumption;
        public double averagePrice;

        public SpotCalculation(double totalPrice, double totalCost, double totalCostWithoutMargin, double totalConsumption) {
            this.totalPrice = totalPrice;
            this.totalCost = totalCost;
            this.totalCostWithoutMargin = totalCostWithoutMargin;
            this.totalConsumption = totalConsumption;
        }
    }

}
