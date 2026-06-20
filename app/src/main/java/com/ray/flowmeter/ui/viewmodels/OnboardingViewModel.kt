package com.ray.flowmeter.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ray.flowmeter.data.UserPreferencesRepository
import kotlinx.coroutines.launch

// ViewModel managing the onboarding setup sequence and baseline configuration.
class OnboardingViewModel(
    private val repository: UserPreferencesRepository,
) : ViewModel() {
    
    // Completes onboarding by saving state, turning on monitoring, and logging baseline version code.
    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(completed = true)
            repository.setMonitoringEnabled(enabled = true)
            repository.updateLastVersionCode(com.ray.flowmeter.BuildConfig.VERSION_CODE)
        }
    }
}
