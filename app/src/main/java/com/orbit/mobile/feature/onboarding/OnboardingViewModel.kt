package com.orbit.mobile.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.OrbitDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Onboarding VM
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: OrbitDataStore
) : ViewModel() {

    // Mark done
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            dataStore.setOnboardingSeen()
            onDone()
        }
    }
}
