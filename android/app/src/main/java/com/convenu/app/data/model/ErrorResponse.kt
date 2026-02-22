package com.convenu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
)
