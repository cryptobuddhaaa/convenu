package com.convenu.app.data.repository

import com.convenu.app.data.api.SupabaseClient
import com.convenu.app.data.model.ItineraryData
import com.convenu.app.data.model.ItineraryDto
import com.convenu.app.data.model.ItineraryEvent
import com.convenu.app.data.model.ItineraryInsert
import com.convenu.app.data.model.ItineraryUpdate
import kotlinx.serialization.encodeToString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItineraryRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) {
    private val json get() = supabaseClient.json

    // -------------------------------------------------------------------------
    // Core CRUD
    // -------------------------------------------------------------------------

    suspend fun getAll(): Result<List<ItineraryDto>> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val path = "itineraries?user_id=eq.$userId&select=*&order=created_at.desc"
            supabaseClient.get(path).mapCatching { body ->
                json.decodeFromString<List<ItineraryDto>>(body)
            }
        } catch (e: Exception) {
            Timber.e(e, "getAll itineraries failed")
            Result.failure(e)
        }
    }

    suspend fun getById(id: String): Result<ItineraryDto> {
        return try {
            val path = "itineraries?id=eq.$id&select=*"
            supabaseClient.get(path).mapCatching { body ->
                json.decodeFromString<List<ItineraryDto>>(body)
                    .firstOrNull()
                    ?: throw Exception("Itinerary not found: $id")
            }
        } catch (e: Exception) {
            Timber.e(e, "getById itinerary failed: $id")
            Result.failure(e)
        }
    }

    suspend fun create(insert: ItineraryInsert): Result<ItineraryDto> {
        return try {
            val body = json.encodeToString(insert)
            supabaseClient.post("itineraries", body).mapCatching { responseBody ->
                json.decodeFromString<List<ItineraryDto>>(responseBody)
                    .firstOrNull()
                    ?: throw Exception("No itinerary returned from create")
            }
        } catch (e: Exception) {
            Timber.e(e, "create itinerary failed")
            Result.failure(e)
        }
    }

    suspend fun update(id: String, update: ItineraryUpdate): Result<ItineraryDto> {
        return try {
            val body = json.encodeToString(update)
            supabaseClient.patch("itineraries?id=eq.$id", body).mapCatching { responseBody ->
                json.decodeFromString<List<ItineraryDto>>(responseBody)
                    .firstOrNull()
                    ?: throw Exception("No itinerary returned from update")
            }
        } catch (e: Exception) {
            Timber.e(e, "update itinerary failed: $id")
            Result.failure(e)
        }
    }

    suspend fun delete(id: String): Result<Unit> {
        return try {
            supabaseClient.delete("itineraries?id=eq.$id")
        } catch (e: Exception) {
            Timber.e(e, "delete itinerary failed: $id")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Event mutations (fetch -> modify in memory -> PATCH data field)
    // -------------------------------------------------------------------------

    suspend fun addEvent(
        itineraryId: String,
        dayDate: String,
        event: ItineraryEvent,
    ): Result<ItineraryDto> {
        return try {
            val itinerary = getById(itineraryId).getOrElse { return Result.failure(it) }

            val updatedDays = itinerary.data.days.map { day ->
                if (day.date == dayDate) {
                    day.copy(events = day.events + event)
                } else {
                    day
                }
            }

            if (updatedDays.none { it.date == dayDate }) {
                return Result.failure(Exception("Day not found for date: $dayDate"))
            }

            patchData(itineraryId, itinerary.data.copy(days = updatedDays))
        } catch (e: Exception) {
            Timber.e(e, "addEvent failed: itineraryId=$itineraryId dayDate=$dayDate")
            Result.failure(e)
        }
    }

    suspend fun updateEvent(
        itineraryId: String,
        eventId: String,
        event: ItineraryEvent,
    ): Result<ItineraryDto> {
        return try {
            val itinerary = getById(itineraryId).getOrElse { return Result.failure(it) }

            var eventFound = false
            val updatedDays = itinerary.data.days.map { day ->
                val updatedEvents = day.events.map { existing ->
                    if (existing.id == eventId) {
                        eventFound = true
                        event
                    } else {
                        existing
                    }
                }
                day.copy(events = updatedEvents)
            }

            if (!eventFound) {
                return Result.failure(Exception("Event not found: $eventId"))
            }

            patchData(itineraryId, itinerary.data.copy(days = updatedDays))
        } catch (e: Exception) {
            Timber.e(e, "updateEvent failed: itineraryId=$itineraryId eventId=$eventId")
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(
        itineraryId: String,
        eventId: String,
    ): Result<ItineraryDto> {
        return try {
            val itinerary = getById(itineraryId).getOrElse { return Result.failure(it) }

            var eventFound = false
            val updatedDays = itinerary.data.days.map { day ->
                val filtered = day.events.filter { it.id != eventId }
                if (filtered.size != day.events.size) eventFound = true
                day.copy(events = filtered)
            }

            if (!eventFound) {
                return Result.failure(Exception("Event not found: $eventId"))
            }

            patchData(itineraryId, itinerary.data.copy(days = updatedDays))
        } catch (e: Exception) {
            Timber.e(e, "deleteEvent failed: itineraryId=$itineraryId eventId=$eventId")
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private suspend fun patchData(
        itineraryId: String,
        data: ItineraryData,
    ): Result<ItineraryDto> {
        val body = json.encodeToString(ItineraryUpdate(data = data))
        return supabaseClient.patch("itineraries?id=eq.$itineraryId", body).mapCatching { responseBody ->
            json.decodeFromString<List<ItineraryDto>>(responseBody)
                .firstOrNull()
                ?: throw Exception("No itinerary returned from patch")
        }
    }
}
