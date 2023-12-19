package com.vesanieminen.froniusvisualizer.services.model

import java.io.Serializable

class APIVersion : Serializable {
    var aPIVersion: String? = null
    var baseURL: String? = null
    var compatibilityRange: String? = null
}