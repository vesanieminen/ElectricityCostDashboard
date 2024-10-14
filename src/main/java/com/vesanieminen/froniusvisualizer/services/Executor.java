package com.vesanieminen.froniusvisualizer.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.updateData;
import static com.vesanieminen.froniusvisualizer.services.PakastinSpotService.getAndWriteToFile2YearData;
import static com.vesanieminen.froniusvisualizer.util.Utils.getSecondsToNextEvenHour;
import static com.vesanieminen.froniusvisualizer.util.Utils.getSecondsToNextTimeAt;

@Slf4j
@Component
public class Executor {

    private ScheduledExecutorService executorService;

    @PostConstruct
    public void init() {
        ThreadFactory virtualThreadFactory = Thread.ofVirtual().factory();
        executorService = Executors.newScheduledThreadPool(0, virtualThreadFactory);
        scheduleTasks();
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
    }

    private void scheduleTasks() {
        // initial data fetches
        executorService.schedule(() -> safeExecute(Executor::updateNordpoolForLast7Days), 0, TimeUnit.SECONDS);
        executorService.schedule(() -> safeExecute(Executor::updatePakastinData), 0, TimeUnit.SECONDS);
        executorService.schedule(() -> safeExecute(Executor::updateSpotHintaService), 0, TimeUnit.SECONDS);
        executorService.schedule(() -> safeExecute(Executor::updateFingridData), 0, TimeUnit.SECONDS);

        // hourly schedules
        executorService.scheduleAtFixedRate(
                () -> safeExecute(() -> updateNordpoolData(LocalDate.now().plusDays(1))),
                getSecondsToNextEvenHour(),
                TimeUnit.HOURS.toSeconds(1),
                TimeUnit.SECONDS
        );
        executorService.scheduleAtFixedRate(
                () -> safeExecute(Executor::updatePakastinData),
                getSecondsToNextEvenHour(),
                TimeUnit.HOURS.toSeconds(1),
                TimeUnit.SECONDS
        );
        executorService.scheduleAtFixedRate(
                () -> safeExecute(Executor::updateSpotHintaService),
                getSecondsToNextEvenHour(),
                TimeUnit.HOURS.toSeconds(1),
                TimeUnit.SECONDS
        );

        // Fingrid hourly data fetch with delay
        executorService.scheduleAtFixedRate(
                () -> safeExecute(Executor::updateFingridData),
                getSecondsToNextEvenHour() + 180,
                TimeUnit.HOURS.toSeconds(1),
                TimeUnit.SECONDS
        );

        // Nordpool daily fetch at 13:51 Finnish time
        executorService.scheduleAtFixedRate(
                () -> safeExecute(() -> updateNordpoolData(LocalDate.now().plusDays(1))),
                getSecondsToNextTimeAt(13, 51),
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS
        );
    }

    private void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.info("Exception whilst running a scheduled task: %s".formatted(e.toString()));
        }
    }

    public static void updateNordpoolData(LocalDate localDate) {
        log.info("Started update Nordpool");
        final var startTime = System.currentTimeMillis();
        updateData(localDate, true);
        log.info("Ended update Nordpool in {} seconds", (System.currentTimeMillis() - startTime) / 1000.0);
    }

    public static void updateSpotHintaService() {
        log.info("Started updateSpotHintaService");
        final var startTime = System.currentTimeMillis();
        SpotHintaService.updateData();
        log.info("Ended updateSpotHintaService in {} seconds", (System.currentTimeMillis() - startTime) / 1000.0);
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
        log.info("Started updatePakastinData");
        final var startTime = System.currentTimeMillis();

        //getAndWriteToFile();
        getAndWriteToFile2YearData();

        log.info("Ended updatePakastinData in {} seconds", (System.currentTimeMillis() - startTime) / 1000.0);
    }

    /**
     * Updates data for the last 7 days, including today and tomorrow.
     * Uses Java 21 virtual threads with a 1.2-second delay after each call
     * to comply with Nordpool's API rate limiting.
     */
    public static void updateNordpoolForLast7Days() {
        log.info("Started updateNordpoolForLast7Days");
        final var startTime = System.currentTimeMillis();

        // Create a ScheduledExecutorService with virtual threads
        ThreadFactory virtualThreadFactory = Thread.ofVirtual().factory();
        try (var scheduler = Executors.newScheduledThreadPool(0, virtualThreadFactory)) {

            LocalDate today = LocalDate.now();
            List<LocalDate> dates = new ArrayList<>();

            // Collect dates from 6 days ago up to tomorrow (total 8 days)
            for (int i = -6; i <= 1; i++) {
                dates.add(today.plusDays(i));
            }

            // Sort dates in chronological order
            dates.sort(Comparator.naturalOrder());

            // Schedule updateData calls with a 1.2-second delay between each
            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = dates.get(i);
                long delayInMillis = i * 1200L; // 1.2 seconds per call
                scheduler.schedule(() -> updateData(date, true), delayInMillis, TimeUnit.MILLISECONDS);
            }

            // Shutdown the scheduler after tasks are completed
            scheduler.shutdown();
            try {
                // Wait for all scheduled tasks to complete
                boolean finished = scheduler.awaitTermination(20, TimeUnit.SECONDS);
                if (!finished) {
                    log.warn("Scheduler did not finish within the timeout period.");
                }
            } catch (InterruptedException e) {
                log.error("Scheduler was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        log.info("Ended updateNordpoolForLast7Days in {} seconds", (System.currentTimeMillis() - startTime) / 1000.0);
    }

}
