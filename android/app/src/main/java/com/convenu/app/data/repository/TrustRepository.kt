package com.convenu.app.data.repository

import com.convenu.app.data.api.ConvenuApi
import com.convenu.app.data.model.ErrorResponse
import com.convenu.app.data.model.TrustComputeResponse
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustRepository @Inject constructor(
    private val api: ConvenuApi,
    private val json: Json,
) {
    suspend fun computeTrust(): Result<TrustComputeResponse> {
        return try {
            val response = api.computeTrust()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = errorBody?.let {
                    runCatching { json.decodeFromString<ErrorResponse>(it).error }.getOrNull()
                } ?: "Trust compute failed (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "computeTrust failed")
            Result.failure(e)
        }
    }
}
