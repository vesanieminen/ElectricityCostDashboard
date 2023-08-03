package com.vesanieminen.froniusvisualizer;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vesanieminen.froniusvisualizer.services.Executor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SpringBootApplication
@Theme(value = "froniusvizualizer")
@PWA(name = "Liukuri", shortName = "Liukuri", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@NpmPackage(value = "@vaadin-component-factory/vcf-nav", version = "1.0.6")
@JsModule("src/prefers-color-scheme.js")
@EnableScheduling
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        Executor.init();
        SpringApplication.run(Application.class, args);
    }

}
