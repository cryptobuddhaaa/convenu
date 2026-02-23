package com.convenu.app.data.repository

import com.convenu.app.data.api.SupabaseClient
import com.convenu.app.data.model.ContactInsert
import com.convenu.app.data.model.ContactModel
import com.convenu.app.data.model.ContactUpdate
import com.convenu.app.data.model.UserTag
import com.convenu.app.data.model.UserTagInsert
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) {

    // -------------------------------------------------------------------------
    // Contacts
    // -------------------------------------------------------------------------

    suspend fun getAll(): Result<List<ContactModel>> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        return supabaseClient.get(
            "contacts?user_id=eq.$userId&select=*&order=created_at.desc"
        ).mapCatching { body ->
            supabaseClient.json.decodeFromString<List<ContactModel>>(body)
        }.onFailure { Timber.e(it, "getAll contacts failed") }
    }

    suspend fun getByItinerary(itineraryId: String): Result<List<ContactModel>> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        return supabaseClient.get(
            "contacts?user_id=eq.$userId&itinerary_id=eq.$itineraryId&select=*&order=created_at.desc"
        ).mapCatching { body ->
            supabaseClient.json.decodeFromString<List<ContactModel>>(body)
        }.onFailure { Timber.e(it, "getByItinerary contacts failed: itineraryId=$itineraryId") }
    }

    suspend fun create(insert: ContactInsert): Result<ContactModel> {
        val body = supabaseClient.json.encodeToString(ContactInsert.serializer(), insert)

        return supabaseClient.post("contacts", body).mapCatching { responseBody ->
            supabaseClient.json.decodeFromString<List<ContactModel>>(responseBody).firstOrNull()
                ?: throw Exception("Empty response from contacts insert")
        }.onFailure { Timber.e(it, "create contact failed") }
    }

    suspend fun update(id: String, update: ContactUpdate): Result<ContactModel> {
        val body = supabaseClient.json.encodeToString(ContactUpdate.serializer(), update)

        return supabaseClient.patch("contacts?id=eq.$id", body).mapCatching { responseBody ->
            supabaseClient.json.decodeFromString<List<ContactModel>>(responseBody).firstOrNull()
                ?: throw Exception("Empty response from contacts update")
        }.onFailure { Timber.e(it, "update contact failed: id=$id") }
    }

    suspend fun delete(id: String): Result<Unit> {
        return supabaseClient.delete("contacts?id=eq.$id")
            .onFailure { Timber.e(it, "delete contact failed: id=$id") }
    }

    suspend fun deleteByEvent(eventId: String): Result<Unit> {
        return supabaseClient.delete("contacts?event_id=eq.$eventId")
            .onFailure { Timber.e(it, "deleteByEvent contacts failed: eventId=$eventId") }
    }

    // -------------------------------------------------------------------------
    // Tags
    // -------------------------------------------------------------------------

    suspend fun getTags(): Result<List<UserTag>> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        return supabaseClient.get(
            "user_tags?user_id=eq.$userId&select=*&order=created_at.asc"
        ).mapCatching { body ->
            supabaseClient.json.decodeFromString<List<UserTag>>(body)
        }.onFailure { Timber.e(it, "getTags failed") }
    }

    suspend fun createTag(name: String): Result<UserTag> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        val insert = UserTagInsert(userId = userId, name = name)
        val body = supabaseClient.json.encodeToString(UserTagInsert.serializer(), insert)

        return supabaseClient.post("user_tags", body).mapCatching { responseBody ->
            supabaseClient.json.decodeFromString<List<UserTag>>(responseBody).firstOrNull()
                ?: throw Exception("Empty response from user_tags insert")
        }.onFailure { Timber.e(it, "createTag failed: name=$name") }
    }

    suspend fun deleteTag(tagId: String): Result<Unit> {
        return supabaseClient.delete("user_tags?id=eq.$tagId")
            .onFailure { Timber.e(it, "deleteTag failed: tagId=$tagId") }
    }
}
