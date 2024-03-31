package com.vesanieminen.froniusvisualizer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Load properties from versions.properties.
 *
 * This uses properties in Spring via Java configuration.
 */
@PropertySource(value = {"classpath:versions.properties"})
@Configuration
public class AppVersions {

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.buildtime}")
    private String appBuildTime;

    @Value("${app.vaadin.version}")
    private String appVaadinVersion;

    @Value("${app.java.version}")
    private String appJavaVersion;

    @Value("${app.git.commit}")
    private String gitCommit;

    public String getVersion() {
        return appVersion;
    }

    public String getBuildTime() {
        return appBuildTime;
    }

    public String getVaadinVersion() {
        return appVaadinVersion;
    }

    public String getJavaVersion() {
        return appJavaVersion;
    }

    public String getGitCommit() {
        return gitCommit;
    }

}
