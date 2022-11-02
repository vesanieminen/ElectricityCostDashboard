package com.vesanieminen.froniusvisualizer.services;

import com.vesanieminen.froniusvisualizer.util.Utils;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Executor {

    static {
        ZonedDateTime now = ZonedDateTime.now(Utils.fiZoneID);
        ZonedDateTime nextHour = Utils.getNextHour();
        final var seconds = Duration.between(now, nextHour).getSeconds();

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        executorService.schedule(Executor::updateAll, 0, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(Executor::updateAll, seconds, TimeUnit.MINUTES.toSeconds(1), TimeUnit.SECONDS);
    }

    private static void updateAll() {
        log.info("Started updateAll");
        final var startTime = System.currentTimeMillis();
        NordpoolSpotService.updateNordpoolData();
        FingridService.updateWindEstimateData();
        FingridService.updateRealtimeData();
        ////PakastinSpotService.updateData();
        log.info("Ended updateAll in " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
    }


    public static void init() {
        // NOP
    }

}
