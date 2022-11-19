package com.vesanieminen.froniusvisualizer.services;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.vesanieminen.froniusvisualizer.services.PakastinSpotService.getAndWriteToFile2YearData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.updateSpotData;
import static com.vesanieminen.froniusvisualizer.util.Utils.getSecondsToNextEvenHour;

@Slf4j
public class Executor {

    static {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        executorService.schedule(Executor::updateAll, 0, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(Executor::updateAll, getSecondsToNextEvenHour(), TimeUnit.HOURS.toSeconds(1), TimeUnit.SECONDS);
    }

    private static void updateAll() {
        log.info("Started updateAll");
        final var startTime = System.currentTimeMillis();
        NordpoolSpotService.updateNordpoolData();
        try {
            FingridService.updateWindEstimateData();
            TimeUnit.MILLISECONDS.sleep(500);
            FingridService.updateProductionEstimateData();
            TimeUnit.MILLISECONDS.sleep(500);
            FingridService.updateConsumptionEstimateData();
            writeMarketPriceFile();
            updateSpotData();
            TimeUnit.MILLISECONDS.sleep(500);
            FingridService.updateRealtimeData();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("Ended updateAll in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
    }

    private static void writeMarketPriceFile() {
        //getAndWriteToFile();
        getAndWriteToFile2YearData();
    }


    public static void init() {
        // NOP
    }

}
