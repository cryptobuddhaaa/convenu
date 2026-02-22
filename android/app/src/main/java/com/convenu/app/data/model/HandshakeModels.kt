package com.convenu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Initiate ---

@Serializable
data class HandshakeInitiateRequest(
    @SerialName("contactId") val contactId: String,
    @SerialName("walletAddress") val walletAddress: String,
)

@Serializable
data class HandshakeInitiateResponse(
    @SerialName("handshakeId") val handshakeId: String,
    val transaction: String,
    @SerialName("receiverIdentifier") val receiverIdentifier: String,
    @SerialName("contactName") val contactName: String,
)

// --- Claim ---

@Serializable
data class HandshakeClaimRequest(
    @SerialName("handshakeId") val handshakeId: String,
    @SerialName("walletAddress") val walletAddress: String,
)

@Serializable
data class HandshakeClaimResponse(
    @SerialName("handshakeId") val handshakeId: String,
    val status: String,
    val transaction: String,
    @SerialName("initiatorName") val initiatorName: String,
)

// --- Confirm Transaction ---

@Serializable
data class ConfirmTxRequest(
    @SerialName("handshakeId") val handshakeId: String,
    @SerialName("signedTransaction") val signedTransaction: String,
    val side: String,
)

@Serializable
data class ConfirmTxResponse(
    @SerialName("txSignature") val txSignature: String,
    val side: String,
    @SerialName("bothPaid") val bothPaid: Boolean,
    val status: String,
)

// --- Mint ---

@Serializable
data class MintRequest(
    @SerialName("handshakeId") val handshakeId: String,
)

@Serializable
data class MintResponse(
    val status: String,
    @SerialName("initiatorNft") val initiatorNft: String,
    @SerialName("receiverNft") val receiverNft: String,
    @SerialName("pointsAwarded") val pointsAwarded: Int,
)

// --- Pending List ---

@Serializable
data class PendingHandshakesResponse(
    val handshakes: List<HandshakeDto>,
)

@Serializable
data class HandshakeDto(
    val id: String,
    @SerialName("initiator_user_id") val initiatorUserId: String,
    @SerialName("receiver_user_id") val receiverUserId: String? = null,
    @SerialName("receiver_identifier") val receiverIdentifier: String,
    @SerialName("contact_id") val contactId: String? = null,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("event_title") val eventTitle: String? = null,
    @SerialName("event_date") val eventDate: String? = null,
    @SerialName("initiator_wallet") val initiatorWallet: String? = null,
    @SerialName("receiver_wallet") val receiverWallet: String? = null,
    @SerialName("initiator_minted_at") val initiatorMintedAt: String? = null,
    @SerialName("receiver_minted_at") val receiverMintedAt: String? = null,
    val status: String,
    @SerialName("initiator_nft_address") val initiatorNftAddress: String? = null,
    @SerialName("receiver_nft_address") val receiverNftAddress: String? = null,
    @SerialName("initiator_tx_signature") val initiatorTxSignature: String? = null,
    @SerialName("receiver_tx_signature") val receiverTxSignature: String? = null,
    @SerialName("points_awarded") val pointsAwarded: Int = 0,
    @SerialName("mint_fee_lamports") val mintFeeLamports: Long = 10_000_000,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("initiator_name") val initiatorName: String? = null,
    @SerialName("initiator_email") val initiatorEmail: String? = null,
)
