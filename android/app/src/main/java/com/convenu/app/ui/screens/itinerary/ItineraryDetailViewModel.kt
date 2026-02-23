package com.convenu.app.ui.screens.itinerary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenu.app.data.model.ItineraryDto
import com.convenu.app.data.model.ItineraryEvent
import com.convenu.app.data.model.ItineraryUpdate
import com.convenu.app.data.model.LocationModel
import com.convenu.app.data.repository.ItineraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ItineraryDetailUiState(
    val isLoading: Boolean = true,
    val itinerary: ItineraryDto? = null,
    val error: String? = null,
    val showEventForm: Boolean = false,
    val editingEvent: ItineraryEvent? = null,
    val selectedDayDate: String? = null,
    val expandedDays: Set<String> = emptySet(),
    val eventSearch: String = "",
    val actionMessage: String? = null,
)

@HiltViewModel
class ItineraryDetailViewModel @Inject constructor(
    private val itineraryRepository: ItineraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val itineraryId: String = savedStateHandle["itineraryId"] ?: ""

    private val _uiState = MutableStateFlow(ItineraryDetailUiState())
    val uiState: StateFlow<ItineraryDetailUiState> = _uiState.asStateFlow()

    init { loadItinerary() }

    fun loadItinerary() {
        if (itineraryId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = itineraryRepository.getById(itineraryId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                itinerary = result.getOrNull(),
                error = result.exceptionOrNull()?.message,
            )
        }
    }

    fun toggleDayExpansion(date: String) {
        val current = _uiState.value.expandedDays
        _uiState.value = _uiState.value.copy(
            expandedDays = if (current.contains(date)) current - date else current + date
        )
    }

    fun setEventSearch(query: String) {
        _uiState.value = _uiState.value.copy(eventSearch = query)
    }

    fun showAddEventForm(dayDate: String) {
        _uiState.value = _uiState.value.copy(showEventForm = true, selectedDayDate = dayDate, editingEvent = null)
    }

    fun showEditEventForm(event: ItineraryEvent, dayDate: String) {
        _uiState.value = _uiState.value.copy(showEventForm = true, editingEvent = event, selectedDayDate = dayDate)
    }

    fun hideEventForm() {
        _uiState.value = _uiState.value.copy(showEventForm = false, editingEvent = null, selectedDayDate = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun addEvent(
        dayDate: String,
        title: String,
        startTime: String,
        endTime: String,
        locationName: String,
        locationAddress: String,
        eventType: String,
        description: String?,
    ) {
        viewModelScope.launch {
            val event = ItineraryEvent(
                id = UUID.randomUUID().toString(),
                title = title,
                startTime = startTime,
                endTime = endTime,
                location = LocationModel(name = locationName, address = locationAddress),
                eventType = eventType,
                description = description,
            )
            val result = itineraryRepository.addEvent(itineraryId, dayDate, event)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    itinerary = result.getOrNull(),
                    showEventForm = false,
                    actionMessage = "Event added",
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actionMessage = result.exceptionOrNull()?.message ?: "Failed to add event",
                )
            }
        }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        startTime: String,
        endTime: String,
        locationName: String,
        locationAddress: String,
        eventType: String,
        description: String?,
    ) {
        viewModelScope.launch {
            val event = ItineraryEvent(
                id = eventId,
                title = title,
                startTime = startTime,
                endTime = endTime,
                location = LocationModel(name = locationName, address = locationAddress),
                eventType = eventType,
                description = description,
            )
            val result = itineraryRepository.updateEvent(itineraryId, eventId, event)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    itinerary = result.getOrNull(),
                    showEventForm = false,
                    editingEvent = null,
                    actionMessage = "Event updated",
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actionMessage = result.exceptionOrNull()?.message ?: "Failed to update event",
                )
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            val result = itineraryRepository.deleteEvent(itineraryId, eventId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    itinerary = result.getOrNull(),
                    actionMessage = "Event deleted",
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actionMessage = result.exceptionOrNull()?.message ?: "Failed to delete event",
                )
            }
        }
    }

    fun updateItinerary(title: String, location: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            val update = ItineraryUpdate(
                title = title,
                location = location,
                startDate = startDate,
                endDate = endDate,
            )
            val result = itineraryRepository.update(itineraryId, update)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    itinerary = result.getOrNull(),
                    actionMessage = "Itinerary updated",
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    actionMessage = result.exceptionOrNull()?.message ?: "Failed to update",
                )
            }
        }
    }
}
