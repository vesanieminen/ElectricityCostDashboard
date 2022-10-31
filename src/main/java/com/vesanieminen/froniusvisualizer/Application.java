package com.vesanieminen.froniusvisualizer;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/**
 * The entry point of the Spring Boot application.
 * <p>
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@Theme(value = "froniusvizualizer")
@PWA(name = "Sähkön hinta", shortName = "Sähkön hinta", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@JsModule("src/prefers-color-scheme.js")
//@JsModule("src/app-height.js")
public class Application implements AppShellConfigurator {

    //@Bean
    //public ServletWebServerFactory servletContainer() {
    //    if (isDevelopmentMode() || isStagingEnvironment()) {
    //        System.out.println("dev / staging mode");
    //        return new TomcatServletWebServerFactory();
    //    }
    //    System.out.println("production mode");
    //    return new TomcatServletWebServerFactory() {
    //        @Override
    //        protected void postProcessContext(Context context) {
    //            SecurityConstraint securityConstraint = new SecurityConstraint();
    //            securityConstraint.setUserConstraint("CONFIDENTIAL");
    //            SecurityCollection collection = new SecurityCollection();
    //            collection.addPattern("/*");
    //            securityConstraint.addCollection(collection);
    //            context.addConstraint(securityConstraint);
    //        }
    //    };
    //}

}
