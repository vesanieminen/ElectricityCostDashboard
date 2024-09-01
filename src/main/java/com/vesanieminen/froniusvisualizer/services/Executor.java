package com.vesanieminen.froniusvisualizer.services;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.vesanieminen.froniusvisualizer.services.PakastinSpotService.getAndWriteToFile2YearData;
import static com.vesanieminen.froniusvisualizer.util.Utils.getSecondsToNextEvenHour;
import static com.vesanieminen.froniusvisualizer.util.Utils.getSecondsToNextTimeAt;

@Slf4j
public class Executor {

    static {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
        executorService.schedule(Executor::updateAll, 0, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(Executor::updatePrices, getSecondsToNextEvenHour(), TimeUnit.HOURS.toSeconds(1), TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(Executor::updateFingridData, getSecondsToNextEvenHour() + 120, TimeUnit.HOURS.toSeconds(1), TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(Executor::updateNordpoolData, getSecondsToNextTimeAt(13, 51), TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    private static void updateAll() {
        log.info("Started updateAll");
        updatePrices();
        updateFingridData();
        log.info("Ended updateAll");
    }

    public static void updateNordpoolData() {
        log.info("Started update Nordpool");
        final var startTime = System.currentTimeMillis();
        NordpoolSpotService.updateNordpoolData(true);
        log.info("Ended update Nordpool in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
    }

    public static void updatePrices() {
        log.info("Started updatePrices");
        final var startTime = System.currentTimeMillis();
        NordpoolSpotService.updateNordpoolData(false);
        updatePakastinData();
        SpotHintaService.updateData();
        log.info("Ended updatePrices in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
    }

    private static void updateFingridData() {
        log.info("Started updateFingridData");
        final var startTime = System.currentTimeMillis();
        try {
            FingridService.updateRealtimeData();
            TimeUnit.MILLISECONDS.sleep(500);
            FingridService.updateWindEstimateData();
            // TODO: fix these for the new Fingrid API:
            //TimeUnit.MILLISECONDS.sleep(500);
            //FingridService.updateProductionEstimateData();
            //TimeUnit.MILLISECONDS.sleep(500);
            //FingridService.updateConsumptionEstimateData();
        } catch (InterruptedException e) {
            log.error("Could not update Fingrid data");
        }
        log.info("Ended updateFingridData in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
    }

    private static void updatePakastinData() {
        //getAndWriteToFile();
        getAndWriteToFile2YearData();
    }

    public static void init() {
        // NOP
    }

}
