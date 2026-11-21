package com.conduit.msme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conduit.msme.data.local.ConduitPreferencesDataStore
import com.conduit.msme.data.model.ConduitLanguage
import com.conduit.msme.data.model.DigitalTwinSnapshot
import com.conduit.msme.data.model.MSMEProfile
import com.conduit.msme.data.model.RepaymentProgress
import com.conduit.msme.data.repository.ConduitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val language: ConduitLanguage = ConduitLanguage.ENGLISH,
    val profile: MSMEProfile = ConduitPreferencesDataStore.defaultMerchantProfile(),
    val digitalTwin: DigitalTwinSnapshot = ConduitPreferencesDataStore.defaultDigitalTwin(),
    val repaymentProgress: RepaymentProgress = RepaymentProgress(
        totalLoanAmount = 50_000.0,
        amountRepaid = 18_480.0,
        remainingBalance = 31_520.0,
        dailySweepAmount = 220.0,
        interestSavedVsMoneylender = 17_400.0,
        daysRemaining = 144
    )
)

class HomeViewModel(
    private val repository: ConduitRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.languageFlow,
        repository.profileFlow,
        repository.digitalTwinFlow,
        repository.repaymentFlow
    ) { language, profile, twin, repayment ->
        HomeUiState(
            language = language,
            profile = profile,
            digitalTwin = twin,
            repaymentProgress = repayment
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    fun toggleLanguage() {
        viewModelScope.launch {
            repository.toggleLanguage()
        }
    }

    fun setLanguage(language: ConduitLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    class Factory(private val repository: ConduitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
