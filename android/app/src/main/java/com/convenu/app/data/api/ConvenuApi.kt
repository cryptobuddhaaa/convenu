package com.convenu.app.data.api

import com.convenu.app.data.model.ConfirmTxRequest
import com.convenu.app.data.model.ConfirmTxResponse
import com.convenu.app.data.model.HandshakeClaimRequest
import com.convenu.app.data.model.HandshakeClaimResponse
import com.convenu.app.data.model.HandshakeInitiateRequest
import com.convenu.app.data.model.HandshakeInitiateResponse
import com.convenu.app.data.model.MintRequest
import com.convenu.app.data.model.MintResponse
import com.convenu.app.data.model.PendingHandshakesResponse
import com.convenu.app.data.model.TelegramAuthRequest
import com.convenu.app.data.model.TelegramAuthResponse
import com.convenu.app.data.model.TrustComputeResponse
import com.convenu.app.data.model.WalletVerifyRequest
import com.convenu.app.data.model.WalletVerifyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ConvenuApi {

    // --- Auth ---

    @POST("auth/telegram")
    suspend fun authTelegram(
        @Body request: TelegramAuthRequest,
    ): Response<TelegramAuthResponse>

    // --- Wallet ---

    @POST("wallet/verify")
    suspend fun verifyWallet(
        @Body request: WalletVerifyRequest,
    ): Response<WalletVerifyResponse>

    // --- Handshake ---

    @POST("handshake")
    suspend fun initiateHandshake(
        @Query("action") action: String = "initiate",
        @Body request: HandshakeInitiateRequest,
    ): Response<HandshakeInitiateResponse>

    @POST("handshake")
    suspend fun claimHandshake(
        @Query("action") action: String = "claim",
        @Body request: HandshakeClaimRequest,
    ): Response<HandshakeClaimResponse>

    @POST("handshake")
    suspend fun confirmTransaction(
        @Query("action") action: String = "confirm-tx",
        @Body request: ConfirmTxRequest,
    ): Response<ConfirmTxResponse>

    @POST("handshake")
    suspend fun mintHandshake(
        @Query("action") action: String = "mint",
        @Body request: MintRequest,
    ): Response<MintResponse>

    @GET("handshake")
    suspend fun getPendingHandshakes(
        @Query("action") action: String = "pending",
    ): Response<PendingHandshakesResponse>

    // --- Trust ---

    @POST("trust/compute")
    suspend fun computeTrust(): Response<TrustComputeResponse>
}
