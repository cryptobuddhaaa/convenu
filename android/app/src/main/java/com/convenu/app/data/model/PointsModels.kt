package com.convenu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PointEntry(
    val id: String,
    val points: Int,
    val reason: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("handshake_id") val handshakeId: String? = null,
)

@Serializable
data class TrustScoreFull(
    @SerialName("user_id") val userId: String,
    @SerialName("telegram_premium") val telegramPremium: Boolean = false,
    @SerialName("has_profile_photo") val hasProfilePhoto: Boolean = false,
    @SerialName("has_username") val hasUsername: Boolean = false,
    @SerialName("telegram_account_age_days") val telegramAccountAgeDays: Int? = null,
    @SerialName("wallet_connected") val walletConnected: Boolean = false,
    @SerialName("total_handshakes") val totalHandshakes: Int = 0,
    @SerialName("trust_level") val trustLevel: Int = 1,
    @SerialName("updated_at") val updatedAt: String? = null,
)
