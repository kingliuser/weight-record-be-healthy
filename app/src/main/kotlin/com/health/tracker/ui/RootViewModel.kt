package com.health.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.tracker.domain.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RootViewModel(profileRepository: ProfileRepository) : ViewModel() {

    /** null = 还没查到，避免闪一下设置页。 */
    val hasProfile: StateFlow<Boolean?> = profileRepository.observe()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
