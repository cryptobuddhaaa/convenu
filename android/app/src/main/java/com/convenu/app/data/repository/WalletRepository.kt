package com.convenu.app.data.repository

import com.convenu.app.BuildConfig
import com.convenu.app.data.api.ConvenuApi
import com.convenu.app.data.model.ErrorResponse
import com.convenu.app.data.model.UserWalletInsert
import com.convenu.app.data.model.WalletVerifyRequest
import com.convenu.app.data.model.WalletVerifyResponse
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
class WalletRepository @Inject constructor(
    private val api: ConvenuApi,
    private val tokenManager: TokenManager,
    private val json: Json,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun createWalletRow(walletAddress: String): Result<String> {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        val userId = tokenManager.userIdFlow.firstOrNull()
            ?: return Result.failure(Exception("Not logged in"))
        val token = tokenManager.tokenFlow.firstOrNull()
            ?: return Result.failure(Exception("No auth token"))

        return try {
            val body = json.encodeToString(
                UserWalletInsert.serializer(),
                UserWalletInsert(userId = userId, walletAddress = walletAddress),
            )

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/user_wallets")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Extract wallet ID from response
                val walletId = runCatching {
                    json.decodeFromString<List<Map<String, kotlinx.serialization.json.JsonElement>>>(
                        responseBody
                    ).firstOrNull()?.get("id")?.toString()?.trim('"')
                }.getOrNull() ?: ""
                Result.success(walletId)
            } else {
                Timber.e("createWalletRow failed: $response $responseBody")
                Result.failure(Exception("Failed to create wallet row (${response.code})"))
            }
        } catch (e: Exception) {
            Timber.e(e, "createWalletRow failed")
            Result.failure(e)
        }
    }

    suspend fun verifyWallet(
        walletId: String,
        signature: String,
        message: String,
        walletAddress: String,
    ): Result<WalletVerifyResponse> {
        return try {
            val response = api.verifyWallet(
                WalletVerifyRequest(
                    walletId = walletId,
                    signature = signature,
                    message = message,
                    walletAddress = walletAddress,
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = errorBody?.let {
                    runCatching { json.decodeFromString<ErrorResponse>(it).error }.getOrNull()
                } ?: "Verification failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "verifyWallet failed")
            Result.failure(e)
        }
    }
}
