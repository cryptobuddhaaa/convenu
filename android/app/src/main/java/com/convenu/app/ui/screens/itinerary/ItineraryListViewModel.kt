package com.convenu.app.ui.screens.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenu.app.data.model.ItineraryDto
import com.convenu.app.data.model.ItineraryInsert
import com.convenu.app.data.model.ItineraryData
import com.convenu.app.data.model.ItineraryDay
import com.convenu.app.data.repository.ItineraryRepository
import com.convenu.app.data.repository.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ItineraryListUiState(
    val isLoading: Boolean = true,
    val itineraries: List<ItineraryDto> = emptyList(),
    val error: String? = null,
    val showCreateForm: Boolean = false,
    val isCreating: Boolean = false,
    val createError: String? = null,
)

@HiltViewModel
class ItineraryListViewModel @Inject constructor(
    private val itineraryRepository: ItineraryRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItineraryListUiState())
    val uiState: StateFlow<ItineraryListUiState> = _uiState.asStateFlow()

    init { loadItineraries() }

    fun loadItineraries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = itineraryRepository.getAll()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                itineraries = result.getOrDefault(emptyList()),
                error = result.exceptionOrNull()?.message,
            )
        }
    }

    fun showCreateForm() { _uiState.value = _uiState.value.copy(showCreateForm = true, createError = null) }
    fun hideCreateForm() { _uiState.value = _uiState.value.copy(showCreateForm = false, createError = null) }

    fun createItinerary(title: String, location: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            val userId = tokenManager.userIdFlow.firstOrNull()
            if (userId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(isCreating = false, createError = "Not authenticated")
                return@launch
            }

            // Generate days between start and end dates
            val days = generateDays(startDate, endDate)

            val insert = ItineraryInsert(
                userId = userId,
                title = title,
                location = location,
                startDate = startDate,
                endDate = endDate,
                data = ItineraryData(days = days),
            )

            val result = itineraryRepository.create(insert)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isCreating = false, showCreateForm = false)
                loadItineraries()
            } else {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createError = result.exceptionOrNull()?.message ?: "Failed to create itinerary",
                )
            }
        }
    }

    fun deleteItinerary(id: String) {
        viewModelScope.launch {
            itineraryRepository.delete(id)
            loadItineraries()
        }
    }

    private fun generateDays(startDate: String, endDate: String): List<ItineraryDay> {
        return try {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            val numDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
            (0 until numDays).map { i ->
                ItineraryDay(
                    date = start.plusDays(i.toLong()).toString(),
                    dayNumber = i + 1,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
