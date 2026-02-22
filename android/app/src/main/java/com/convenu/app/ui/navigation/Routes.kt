package com.convenu.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CONTACTS = "contacts"
    const val HANDSHAKES = "handshakes"
    const val HANDSHAKE_DETAIL = "handshake/{handshakeId}"
    const val WALLET = "wallet"

    fun handshakeDetail(handshakeId: String) = "handshake/$handshakeId"
}
