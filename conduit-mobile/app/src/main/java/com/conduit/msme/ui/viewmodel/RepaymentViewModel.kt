package com.conduit.msme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conduit.msme.data.model.RepaymentProgress
import com.conduit.msme.data.model.SweepTransaction
import com.conduit.msme.data.repository.ConduitRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RepaymentUiState(
    val progress: RepaymentProgress = RepaymentProgress(
        totalLoanAmount = 50_000.0,
        amountRepaid = 18_480.0,
        remainingBalance = 31_520.0,
        dailySweepAmount = 220.0,
        interestSavedVsMoneylender = 17_400.0,
        daysRemaining = 144
    ),
    val recentSweeps: List<SweepTransaction> = emptyList(),
    val showConfetti: Boolean = false,
    val sweepRecordedJustNow: Boolean = false,
    val certificateReady: Boolean = false,
    val moneylenderTotalCost: Double = 72_000.0,
    val conduitTotalCost: Double = 51_650.0
)

class RepaymentViewModel(
    private val repository: ConduitRepository
) : ViewModel() {

    private val _showConfetti = MutableStateFlow(false)
    private val _sweepRecordedJustNow = MutableStateFlow(false)
    private val _certificateReady = MutableStateFlow(false)
    private val _extraSweeps = MutableStateFlow<List<SweepTransaction>>(emptyList())

    val uiState: StateFlow<RepaymentUiState> = combine(
        repository.repaymentFlow,
        _showConfetti,
        _sweepRecordedJustNow,
        _certificateReady,
        _extraSweeps
    ) { progress, confetti, recordedNow, certReady, extra ->
        RepaymentUiState(
            progress = progress,
            recentSweeps = extra + repository.recentSweepHistory(),
            showConfetti = confetti,
            sweepRecordedJustNow = recordedNow,
            certificateReady = certReady
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = RepaymentUiState(recentSweeps = repository.recentSweepHistory())
    )

    init {
        viewModelScope.launch {
            repository.refreshRepaymentProgress()
        }
    }

    fun simulateDailyUpiSweep() {
        viewModelScope.launch {
            val updated = repository.recordDailyMicroSweep()
            val nextId = "SW-${85 + _extraSweeps.value.size}"
            val newTx = SweepTransaction(
                id = nextId,
                dateLabelEn = "Just now",
                dateLabelKn = "ಈಗಷ್ಟೇ",
                timeLabel = "9:00 PM",
                amount = updated.dailySweepAmount,
                upiRef = "UPI/43261984${1000 + _extraSweeps.value.size}",
                status = "SUCCESS"
            )
            _extraSweeps.value = listOf(newTx) + _extraSweeps.value
            _sweepRecordedJustNow.value = true
            _showConfetti.value = true
            delay(2_800L)
            _showConfetti.value = false
        }
    }

    fun triggerCibilCertificateDownload() {
        viewModelScope.launch {
            _certificateReady.value = true
            _showConfetti.value = true
            delay(2_400L)
            _showConfetti.value = false
        }
    }

    fun dismissCertificateBanner() {
        _certificateReady.value = false
    }

    class Factory(private val repository: ConduitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RepaymentViewModel(repository) as T
        }
    }
}
