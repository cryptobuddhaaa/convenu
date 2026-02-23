package com.convenu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletVerifyRequest(
    @SerialName("walletId") val walletId: String,
    val signature: String,
    val message: String,
    @SerialName("walletAddress") val walletAddress: String,
)

@Serializable
data class WalletVerifyResponse(
    val verified: Boolean,
    val error: String? = null,
)

@Serializable
data class UserWalletInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("wallet_address") val walletAddress: String,
    @SerialName("is_primary") val isPrimary: Boolean = true,
)
