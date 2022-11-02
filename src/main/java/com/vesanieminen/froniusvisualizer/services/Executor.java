package com.vesanieminen.froniusvisualizer.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Executor {

    static {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
        executorService.scheduleAtFixedRate(NordpoolSpotService::updateNordpoolData, 0, 30, TimeUnit.MINUTES);
        executorService.scheduleAtFixedRate(FingridService::updateWindEstimateData, 0, 30, TimeUnit.MINUTES);
        executorService.scheduleAtFixedRate(FingridService::updateRealtimeData, 0, 30, TimeUnit.MINUTES);
        //executorService.scheduleAtFixedRate(PakastinSpotService::updateData, 0, 30, TimeUnit.MINUTES);
    }

    public static void init() {
        // NOP
    }

}
