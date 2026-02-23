package com.convenu.app.data.repository

import com.convenu.app.data.api.SupabaseClient
import com.convenu.app.data.model.PointEntry
import com.convenu.app.data.model.TrustScoreFull
import kotlinx.serialization.builtins.ListSerializer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointsRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
) {
    /**
     * Fetches the most recent [limit] point entries for the authenticated user,
     * ordered by creation date descending.
     */
    suspend fun getPoints(limit: Int = 20): Result<List<PointEntry>> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val path = "user_points" +
                "?user_id=eq.$userId" +
                "&select=id,points,reason,created_at,handshake_id" +
                "&order=created_at.desc" +
                "&limit=$limit"

            supabaseClient.get(path).map { body ->
                supabaseClient.json.decodeFromString(
                    ListSerializer(PointEntry.serializer()),
                    body,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "getPoints failed")
            Result.failure(e)
        }
    }

    /**
     * Returns the total accumulated points for the authenticated user by calling
     * the Supabase RPC function `get_user_total_points`. The function returns a
     * plain integer in the response body.
     */
    suspend fun getTotalPoints(): Result<Int> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val jsonBody = """{"p_user_id":"$userId"}"""

            supabaseClient.rpc("get_user_total_points", jsonBody).map { body ->
                body.trim().toInt()
            }
        } catch (e: Exception) {
            Timber.e(e, "getTotalPoints failed")
            Result.failure(e)
        }
    }

    /**
     * Fetches the full trust score record for the authenticated user from the
     * `trust_scores` table. Returns failure if no record is found.
     */
    suspend fun getTrustScore(): Result<TrustScoreFull> {
        val userId = supabaseClient.getUserId()
            ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val path = "trust_scores?user_id=eq.$userId&select=*"

            supabaseClient.get(path).mapCatching { body ->
                val list = supabaseClient.json.decodeFromString(
                    ListSerializer(TrustScoreFull.serializer()),
                    body,
                )
                list.firstOrNull()
                    ?: throw Exception("No trust score found for user")
            }
        } catch (e: Exception) {
            Timber.e(e, "getTrustScore failed")
            Result.failure(e)
        }
    }
}
