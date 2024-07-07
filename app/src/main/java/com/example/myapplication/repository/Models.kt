package com.example.myapplication.repository.model

import java.time.LocalTime

data class Trip(
    val tripId: String,
    val routeId: String,
    val serviceId: String,
    val tripHeadSign: String
)

data class CalendarEntry(
    val serviceId: String,
    val monday: Int,
    val tuesday: Int,
    val wednesday: Int,
    val thursday: Int,
    val friday: Int,
    val saturday: Int,
    val sunday: Int,
    val startDate: String,
    val endDate: String
)

data class Route(
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String
)

data class Stop(
    val stopId: String,
    val stopName: String
)

data class StopTime(
    val tripId: String,
    val arrivalTime: String,
    val departureTime: String,
    val stopId: String
)

data class Calendar(
    val serviceId: String,
    val monday: Int,
    val tuesday: Int,
    val wednesday: Int,
    val thursday: Int,
    val friday: Int,
    val saturday: Int,
    val sunday: Int,
    val startDate: String,
    val endDate: String
)

data class CalendarDateEntry(
    val serviceId: String,
    val date: String,
    val exceptionType: Int
)
