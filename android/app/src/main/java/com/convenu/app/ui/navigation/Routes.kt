package com.convenu.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CONTACTS = "contacts"
    const val HANDSHAKES = "handshakes"
    const val HANDSHAKE_DETAIL = "handshake/{handshakeId}"
    const val WALLET = "wallet"
    const val ITINERARIES = "itineraries"
    const val ITINERARY_DETAIL = "itinerary/{itineraryId}"

    fun handshakeDetail(handshakeId: String) = "handshake/$handshakeId"
    fun itineraryDetail(itineraryId: String) = "itinerary/$itineraryId"
}
