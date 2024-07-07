package com.example.myapplication.ui.favorites

data class SavedStop(
    val line: String,
    val direction: String,
    val stop: String,
    val stopIds: List<String>,
    val tripId: String,
    val routeIds: List<String>,
    val time: String = ""
)
