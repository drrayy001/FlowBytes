package com.ray.flowmeter.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: UserPreferencesRepository,
) : ViewModel() {
    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(completed = true)
            repository.setMonitoringEnabled(enabled = true)
        }
    }
}
