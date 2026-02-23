package com.convenu.app.data.api

import com.convenu.app.BuildConfig
import com.convenu.app.data.repository.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    val json: Json,
) {
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    suspend fun get(path: String): Result<String> {
        val token = tokenManager.tokenFlow.firstOrNull()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/$path")
                .get()
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $token")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Request failed (${response.code}): ${body ?: "no body"}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Supabase GET failed: $path")
            Result.failure(e)
        }
    }

    suspend fun post(path: String, jsonBody: String, prefer: String = "return=representation"): Result<String> {
        val token = tokenManager.tokenFlow.firstOrNull()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/$path")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", prefer)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful) {
                Result.success(body ?: "")
            } else {
                Result.failure(Exception("Request failed (${response.code}): ${body ?: "no body"}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Supabase POST failed: $path")
            Result.failure(e)
        }
    }

    suspend fun patch(path: String, jsonBody: String): Result<String> {
        val token = tokenManager.tokenFlow.firstOrNull()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/$path")
                .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful) {
                Result.success(body ?: "")
            } else {
                Result.failure(Exception("Request failed (${response.code}): ${body ?: "no body"}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Supabase PATCH failed: $path")
            Result.failure(e)
        }
    }

    suspend fun delete(path: String): Result<Unit> {
        val token = tokenManager.tokenFlow.firstOrNull()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/$path")
                .delete()
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $token")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val body = response.body?.string()
                Result.failure(Exception("Delete failed (${response.code}): ${body ?: "no body"}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Supabase DELETE failed: $path")
            Result.failure(e)
        }
    }

    suspend fun rpc(functionName: String, jsonBody: String): Result<String> {
        val token = tokenManager.tokenFlow.firstOrNull()
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/rpc/$functionName")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful) {
                Result.success(body ?: "")
            } else {
                Result.failure(Exception("RPC failed (${response.code}): ${body ?: "no body"}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Supabase RPC failed: $functionName")
            Result.failure(e)
        }
    }

    suspend fun getUserId(): String? = tokenManager.userIdFlow.firstOrNull()
}
