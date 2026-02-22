package com.convenu.app.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenu.app.BuildConfig
import com.convenu.app.data.repository.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Serializable
data class ContactDto(
    val id: String,
    val name: String,
    @SerialName("telegram_handle") val telegramHandle: String? = null,
    @SerialName("event_title") val eventTitle: String? = null,
    @SerialName("itinerary_title") val itineraryTitle: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    @SerialName("contacted") val contacted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

data class ContactsUiState(
    val isLoading: Boolean = true,
    val contacts: List<ContactDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val supabaseUrl = BuildConfig.SUPABASE_URL
            val supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            val token = tokenManager.tokenFlow.firstOrNull()
            val userId = tokenManager.userIdFlow.firstOrNull()

            if (supabaseUrl.isBlank() || token.isNullOrBlank() || userId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated",
                )
                return@launch
            }

            try {
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/contacts?user_id=eq.$userId&select=*&order=created_at.desc")
                    .get()
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $token")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val contacts = json.decodeFromString<List<ContactDto>>(body)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        contacts = contacts,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load contacts (${response.code})",
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "loadContacts failed")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load contacts",
                )
            }
        }
    }
}
