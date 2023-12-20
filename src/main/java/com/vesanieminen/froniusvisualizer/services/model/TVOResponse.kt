package com.vesanieminen.froniusvisualizer.services.model

class TVOResponse {
    var created: String? = null
    var predictions: List<Data>? = null

    class Data {
        var time: String? = null
        var prediction = 0
    }
}