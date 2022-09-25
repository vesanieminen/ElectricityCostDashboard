package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.FroniusService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Route(value = "")
public class MainView extends Div {

    private final ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(2);
    private FroniusService froniusService;

    public MainView(@Autowired FroniusService froniusService) throws ExecutionException, InterruptedException {
        this.froniusService = froniusService;

        addClassNames(LumoUtility.FlexDirection.ROW, LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.JustifyContent.CENTER);

        //final var apiVersion = froniusService.getAPIVersion();
        //add(new Pre("""
        //        apiVersion: %s
        //        baseURL: %s
        //        compatibilityRange: %s
        //        """.formatted(apiVersion.getAPIVersion(), apiVersion.getBaseURL(), apiVersion.getCompatibilityRange())));
        final var powerFlowRealtimeData = froniusService.getPowerFlowRealtimeData();
        //add(new Pre(powerFlowRealtimeData));
        final var strings = powerFlowRealtimeData.lines().toList();
        final var power = strings.get(9).split(":")[1];
        final var powerDouble = Double.parseDouble(power) / 1000;
        var yield = strings.get(6).split(":")[1];
        yield = yield.substring(0, yield.length() - 1);
        final var yieldDouble = Double.parseDouble(yield) / 1000;
        var yearYield = strings.get(7).split(":")[1];
        yearYield = yearYield.substring(0, yearYield.length() - 1);
        final var yearDouble = (int) Double.parseDouble(yearYield) / 1000;
        add(createCard(createSpan("Production:"), createSpan(powerDouble + " kW")));
        add(createCard(createSpan("Yield today:"), createSpan(yieldDouble + " kWh")));
        add(createCard(createSpan("Yield 2022:"), createSpan(yearDouble + " kWh")));
        //add(new Pre(powerFlowRealtimeData));
    }

    private static Div createCard(Component... components) {
        final var div = new Div(components);
        div.addClassNames(LumoUtility.FlexDirection.COLUMN, LumoUtility.Display.FLEX, LumoUtility.Padding.MEDIUM, LumoUtility.Margin.MEDIUM, LumoUtility.BoxShadow.MEDIUM);
        div.addClassNames("bg-yellow-200", LumoUtility.BorderRadius.LARGE);
        div.addClassNames(LumoUtility.AlignItems.CENTER);
        return div;
    }

    private Span createSpan(String title) {
        final var span = new Span(title);
        span.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);
        return span;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        //getUI().ifPresent(ui -> {
        //    final var before = System.currentTimeMillis();
        //    froniusService.getHistory(response -> {
        //        ui.access(() -> add(new Pre(response.body())));
        //        final var after = System.currentTimeMillis();
        //        System.out.println("length: " + (after - before) / 1000.0);
        //    });
        //});

        threadPool.schedule(() -> {
            //getUI().ifPresent(ui -> {
            //    final var before = System.currentTimeMillis();
            //    froniusService.getHistory(response -> {
            //        ui.access(() -> add(new Pre(response.body())));
            //    });
            //});

        }, 1, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        threadPool.shutdown();
    }
}
