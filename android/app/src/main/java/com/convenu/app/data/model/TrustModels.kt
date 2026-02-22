package com.convenu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrustComputeResponse(
    @SerialName("trustLevel") val trustLevel: Int,
    @SerialName("totalHandshakes") val totalHandshakes: Int,
    @SerialName("walletConnected") val walletConnected: Boolean,
)
