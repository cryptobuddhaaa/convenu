package com.convenu.app.data.repository

import com.convenu.app.data.api.ConvenuApi
import com.convenu.app.data.model.ConfirmTxRequest
import com.convenu.app.data.model.ConfirmTxResponse
import com.convenu.app.data.model.ErrorResponse
import com.convenu.app.data.model.HandshakeClaimRequest
import com.convenu.app.data.model.HandshakeClaimResponse
import com.convenu.app.data.model.HandshakeDto
import com.convenu.app.data.model.HandshakeInitiateRequest
import com.convenu.app.data.model.HandshakeInitiateResponse
import com.convenu.app.data.model.MintRequest
import com.convenu.app.data.model.MintResponse
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandshakeRepository @Inject constructor(
    private val api: ConvenuApi,
    private val json: Json,
) {
    suspend fun initiate(
        contactId: String,
        walletAddress: String,
    ): Result<HandshakeInitiateResponse> {
        return apiCall {
            api.initiateHandshake(
                request = HandshakeInitiateRequest(
                    contactId = contactId,
                    walletAddress = walletAddress,
                )
            )
        }
    }

    suspend fun claim(
        handshakeId: String,
        walletAddress: String,
    ): Result<HandshakeClaimResponse> {
        return apiCall {
            api.claimHandshake(
                request = HandshakeClaimRequest(
                    handshakeId = handshakeId,
                    walletAddress = walletAddress,
                )
            )
        }
    }

    suspend fun confirmTransaction(
        handshakeId: String,
        signedTransaction: String,
        side: String,
    ): Result<ConfirmTxResponse> {
        return apiCall {
            api.confirmTransaction(
                request = ConfirmTxRequest(
                    handshakeId = handshakeId,
                    signedTransaction = signedTransaction,
                    side = side,
                )
            )
        }
    }

    suspend fun mint(handshakeId: String): Result<MintResponse> {
        return apiCall { api.mintHandshake(request = MintRequest(handshakeId)) }
    }

    suspend fun getPending(): Result<List<HandshakeDto>> {
        return try {
            val response = api.getPendingHandshakes()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.handshakes)
            } else {
                val errorMsg = parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "getPending failed")
            Result.failure(e)
        }
    }

    private suspend fun <T> apiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "API call failed")
            Result.failure(e)
        }
    }

    private fun <T> parseError(response: Response<T>): String {
        val errorBody = response.errorBody()?.string()
        return errorBody?.let {
            runCatching { json.decodeFromString<ErrorResponse>(it).error }.getOrNull()
        } ?: "Request failed (${response.code()})"
    }
}
