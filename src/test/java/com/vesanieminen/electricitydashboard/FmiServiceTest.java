package com.vesanieminen.electricitydashboard;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import com.vesanieminen.froniusvisualizer.services.FmiService;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class FmiServiceTest {
    @Test
    public void testFmi() {
        var observations = FmiService.getObservations().getObservations();

        for (var observation : observations) {
            System.out.println(observation.getName());
            System.out.println(FmiService.parseFmiTimestamp(observation.getLocaltime(), observation.getLocaltz()));
        }
    }
}