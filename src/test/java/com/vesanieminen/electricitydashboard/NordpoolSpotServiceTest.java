package com.vesanieminen.electricitydashboard;

import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NordpoolSpotServiceTest {

    @Test
    public void test() {
        NordpoolSpotService.updateData(LocalDate.now().plusDays(1), true);
        final var nordpoolResponse = NordpoolSpotService.getNordpoolResponse();
        int i = 0;
    }


    @Test
    void test2() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread::startVirtualThread
        );
        long initialDelayInMillis = 1000L;
        long delayBetweenTasksInMillis = 1000L;
        for (int i = 0; i < 5; i++) {
            long delay = initialDelayInMillis + i * delayBetweenTasksInMillis;
            scheduler.schedule(() -> System.out.println("Called at " + Instant.now()), delay, TimeUnit.MILLISECONDS);
        }

        // Shutdown the scheduler and wait for tasks to complete
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Scheduler did not terminate in the specified time.");
                List<Runnable> droppedTasks = scheduler.shutdownNow();
                System.err.println("Scheduler was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}