package com.convenu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactModel(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("itinerary_id") val itineraryId: String? = null,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    @SerialName("project_company") val projectCompany: String? = null,
    val position: String? = null,
    @SerialName("telegram_handle") val telegramHandle: String? = null,
    val email: String? = null,
    val linkedin: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    @SerialName("event_title") val eventTitle: String? = null,
    @SerialName("luma_event_url") val lumaEventUrl: String? = null,
    @SerialName("date_met") val dateMet: String? = null,
    @SerialName("last_contacted_at") val lastContactedAt: String? = null,
    val contacted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Serializable
data class ContactInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("itinerary_id") val itineraryId: String? = null,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("project_company") val projectCompany: String? = null,
    val position: String? = null,
    @SerialName("telegram_handle") val telegramHandle: String? = null,
    val email: String? = null,
    val linkedin: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    @SerialName("event_title") val eventTitle: String? = null,
    @SerialName("luma_event_url") val lumaEventUrl: String? = null,
    @SerialName("date_met") val dateMet: String? = null,
)

@Serializable
data class ContactUpdate(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("project_company") val projectCompany: String? = null,
    val position: String? = null,
    @SerialName("telegram_handle") val telegramHandle: String? = null,
    val email: String? = null,
    val linkedin: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    @SerialName("last_contacted_at") val lastContactedAt: String? = null,
    val contacted: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class UserTag(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class UserTagInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
)
