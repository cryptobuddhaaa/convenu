package com.convenu.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenu.app.data.model.HandshakeDto
import com.convenu.app.data.model.TrustComputeResponse
import com.convenu.app.data.repository.HandshakeRepository
import com.convenu.app.data.repository.TrustRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val trust: TrustComputeResponse? = null,
    val recentHandshakes: List<HandshakeDto> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val trustRepository: TrustRepository,
    private val handshakeRepository: HandshakeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val trustResult = trustRepository.computeTrust()
            val handshakeResult = handshakeRepository.getPending()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                trust = trustResult.getOrNull(),
                recentHandshakes = handshakeResult.getOrDefault(emptyList()),
                error = trustResult.exceptionOrNull()?.message
                    ?: handshakeResult.exceptionOrNull()?.message,
            )
        }
    }
}
