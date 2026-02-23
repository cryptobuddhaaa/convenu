package com.convenu.app.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convenu.app.data.model.ContactInsert
import com.convenu.app.data.model.ContactModel
import com.convenu.app.data.model.ContactUpdate
import com.convenu.app.data.model.UserTag
import com.convenu.app.data.repository.ContactRepository
import com.convenu.app.data.repository.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ContactSortOption(val label: String) {
    DATE_MET("Date Met"),
    FIRST_NAME("First Name"),
    LAST_NAME("Last Name"),
    LAST_CONTACTED("Last Contacted"),
}

data class ContactsUiState(
    val isLoading: Boolean = true,
    val contacts: List<ContactModel> = emptyList(),
    val tags: List<UserTag> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val sortBy: ContactSortOption = ContactSortOption.DATE_MET,
    val filterTag: String? = null,
    val showAddForm: Boolean = false,
    val editingContact: ContactModel? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val showTagManager: Boolean = false,
    val actionMessage: String? = null,
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val contactsResult = contactRepository.getAll()
            val tagsResult = contactRepository.getTags()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                contacts = contactsResult.getOrDefault(emptyList()),
                tags = tagsResult.getOrDefault(emptyList()),
                error = contactsResult.exceptionOrNull()?.message ?: tagsResult.exceptionOrNull()?.message,
            )
        }
    }

    fun setSearchQuery(query: String) { _uiState.value = _uiState.value.copy(searchQuery = query) }
    fun setSortBy(sort: ContactSortOption) { _uiState.value = _uiState.value.copy(sortBy = sort) }
    fun setFilterTag(tag: String?) { _uiState.value = _uiState.value.copy(filterTag = if (_uiState.value.filterTag == tag) null else tag) }
    fun showAddForm() { _uiState.value = _uiState.value.copy(showAddForm = true, submitError = null) }
    fun hideAddForm() { _uiState.value = _uiState.value.copy(showAddForm = false, submitError = null) }
    fun showEditForm(contact: ContactModel) { _uiState.value = _uiState.value.copy(editingContact = contact, submitError = null) }
    fun hideEditForm() { _uiState.value = _uiState.value.copy(editingContact = null, submitError = null) }
    fun toggleTagManager() { _uiState.value = _uiState.value.copy(showTagManager = !_uiState.value.showTagManager) }
    fun clearMessage() { _uiState.value = _uiState.value.copy(actionMessage = null) }

    fun filteredAndSortedContacts(): List<ContactModel> {
        val state = _uiState.value
        var result = state.contacts.toList()

        // Tag filter
        state.filterTag?.let { tag ->
            result = result.filter { it.tags?.contains(tag) == true }
        }

        // Search filter
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            result = result.filter { c ->
                c.firstName.lowercase().contains(query) ||
                c.lastName.lowercase().contains(query) ||
                c.projectCompany?.lowercase()?.contains(query) == true ||
                c.position?.lowercase()?.contains(query) == true ||
                c.eventTitle?.lowercase()?.contains(query) == true ||
                c.email?.lowercase()?.contains(query) == true ||
                c.telegramHandle?.lowercase()?.contains(query) == true ||
                c.tags?.any { it.lowercase().contains(query) } == true
            }
        }

        // Sort
        result = when (state.sortBy) {
            ContactSortOption.DATE_MET -> result.sortedByDescending { it.dateMet ?: "" }
            ContactSortOption.FIRST_NAME -> result.sortedBy { it.firstName.lowercase() }
            ContactSortOption.LAST_NAME -> result.sortedBy { it.lastName.lowercase() }
            ContactSortOption.LAST_CONTACTED -> result.sortedWith(compareByDescending<ContactModel> { it.lastContactedAt != null }.thenByDescending { it.lastContactedAt ?: "" })
        }

        return result
    }

    fun addContact(
        firstName: String, lastName: String, projectCompany: String?, position: String?,
        telegramHandle: String?, email: String?, linkedin: String?, notes: String?,
        selectedTags: List<String>, eventTitle: String? = null, dateMet: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            val userId = tokenManager.userIdFlow.firstOrNull()
            if (userId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = "Not authenticated")
                return@launch
            }
            val insert = ContactInsert(
                userId = userId,
                firstName = firstName, lastName = lastName,
                projectCompany = projectCompany?.ifBlank { null },
                position = position?.ifBlank { null },
                telegramHandle = telegramHandle?.ifBlank { null },
                email = email?.ifBlank { null },
                linkedin = linkedin?.ifBlank { null },
                notes = notes?.ifBlank { null },
                tags = selectedTags.ifEmpty { null },
                eventTitle = eventTitle, dateMet = dateMet,
            )
            contactRepository.create(insert).onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, showAddForm = false, actionMessage = "Contact added")
                refresh()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = it.message)
            }
        }
    }

    fun updateContact(
        id: String, firstName: String, lastName: String, projectCompany: String?, position: String?,
        telegramHandle: String?, email: String?, linkedin: String?, notes: String?,
        selectedTags: List<String>,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            val update = ContactUpdate(
                firstName = firstName, lastName = lastName,
                projectCompany = projectCompany?.ifBlank { null },
                position = position?.ifBlank { null },
                telegramHandle = telegramHandle?.ifBlank { null },
                email = email?.ifBlank { null },
                linkedin = linkedin?.ifBlank { null },
                notes = notes?.ifBlank { null },
                tags = selectedTags.ifEmpty { null },
            )
            contactRepository.update(id, update).onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, editingContact = null, actionMessage = "Contact updated")
                refresh()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitError = it.message)
            }
        }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch {
            contactRepository.delete(id).onSuccess {
                _uiState.value = _uiState.value.copy(actionMessage = "Contact deleted")
                refresh()
            }.onFailure {
                _uiState.value = _uiState.value.copy(actionMessage = "Failed to delete: ${it.message}")
            }
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            contactRepository.createTag(name).onSuccess {
                _uiState.value = _uiState.value.copy(tags = _uiState.value.tags + it)
            }.onFailure {
                _uiState.value = _uiState.value.copy(actionMessage = "Failed to add tag: ${it.message}")
            }
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            val tagName = _uiState.value.tags.find { it.id == tagId }?.name
            contactRepository.deleteTag(tagId).onSuccess {
                _uiState.value = _uiState.value.copy(
                    tags = _uiState.value.tags.filter { it.id != tagId },
                    filterTag = if (_uiState.value.filterTag == tagName) null else _uiState.value.filterTag,
                )
            }
        }
    }

    fun exportCsv(): String {
        val contacts = filteredAndSortedContacts()
        val sb = StringBuilder()
        sb.appendLine("First Name,Last Name,Project/Company,Position,Telegram Handle,Email,Notes,Event,Date Met")
        contacts.forEach { c ->
            sb.appendLine(listOf(
                csvEscape(c.firstName), csvEscape(c.lastName),
                csvEscape(c.projectCompany ?: ""), csvEscape(c.position ?: ""),
                csvEscape(c.telegramHandle ?: ""), csvEscape(c.email ?: ""),
                csvEscape(c.notes ?: ""), csvEscape(c.eventTitle ?: ""),
                csvEscape(c.dateMet ?: ""),
            ).joinToString(","))
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        var s = value
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) {
            s = "'$s"
        }
        return if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            "\"${s.replace("\"", "\"\"")}\""
        } else s
    }
}
