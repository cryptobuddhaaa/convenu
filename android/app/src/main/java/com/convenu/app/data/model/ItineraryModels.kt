package com.convenu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationModel(
    val name: String = "",
    val address: String = "",
    val coordinates: Coordinates? = null,
    @SerialName("mapsUrl") val mapsUrl: String? = null,
    @SerialName("placeId") val placeId: String? = null,
)

@Serializable
data class Coordinates(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class ItineraryEvent(
    val id: String,
    val title: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    val location: LocationModel = LocationModel(),
    @SerialName("eventType") val eventType: String = "meeting",
    val description: String? = null,
    val goals: List<String>? = null,
    @SerialName("lumaEventUrl") val lumaEventUrl: String? = null,
    val notes: List<String> = emptyList(),
    @SerialName("isOrganized") val isOrganized: Boolean? = null,
)

@Serializable
data class ItineraryDay(
    val date: String,
    @SerialName("dayNumber") val dayNumber: Int,
    val events: List<ItineraryEvent> = emptyList(),
    val goals: List<String> = emptyList(),
)

@Serializable
data class ItineraryData(
    val days: List<ItineraryDay> = emptyList(),
)

@Serializable
data class ItineraryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val location: String,
    val data: ItineraryData = ItineraryData(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ItineraryInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val location: String,
    val data: ItineraryData = ItineraryData(),
)

@Serializable
data class ItineraryUpdate(
    val title: String? = null,
    val description: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val location: String? = null,
    val data: ItineraryData? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
