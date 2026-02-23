package com.convenu.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.convenu.app.data.repository.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavHostViewModel @Inject constructor(
    val tokenManager: TokenManager,
) : ViewModel()
