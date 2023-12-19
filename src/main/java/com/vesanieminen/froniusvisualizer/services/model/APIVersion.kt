package com.vesanieminen.froniusvisualizer.services.model;

import java.io.Serializable;

public class APIVersion implements Serializable {

    private String APIVersion;
    private String BaseURL;
    private String CompatibilityRange;

    public String getAPIVersion() {
        return APIVersion;
    }

    public void setAPIVersion(String APIVersion) {
        this.APIVersion = APIVersion;
    }

    public String getBaseURL() {
        return BaseURL;
    }

    public void setBaseURL(String baseURL) {
        BaseURL = baseURL;
    }

    public String getCompatibilityRange() {
        return CompatibilityRange;
    }

    public void setCompatibilityRange(String compatibilityRange) {
        CompatibilityRange = compatibilityRange;
    }
}
