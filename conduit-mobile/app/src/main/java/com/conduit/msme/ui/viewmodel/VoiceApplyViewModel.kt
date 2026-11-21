package com.conduit.msme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conduit.msme.data.model.CreditAssessmentResponse
import com.conduit.msme.data.model.VoiceIntentResponse
import com.conduit.msme.data.repository.ConduitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

sealed interface VoiceApplyUiState {
    data object Idle : VoiceApplyUiState

    data class RecordingVoice(
        val elapsedSeconds: Int = 0
    ) : VoiceApplyUiState

    data object TranscribingAndTranslating : VoiceApplyUiState

    data class AwaitingAAConsent(
        val voiceIntent: VoiceIntentResponse,
        val showConsentDialog: Boolean = false,
        val aaHandle: String = "ravi.hubballi@finvu",
        val isSubmittingConsent: Boolean = false
    ) : VoiceApplyUiState

    data class UnderwritingInProgress(
        val voiceIntent: VoiceIntentResponse,
        val currentStep: Int,
        val progressFraction: Float
    ) : VoiceApplyUiState

    data class LoanApprovedSuccess(
        val voiceIntent: VoiceIntentResponse,
        val offer: CreditAssessmentResponse,
        val isDisbursing: Boolean = false,
        val isDisbursed: Boolean = false,
        val merchantUpiVpa: String = "ravi.hubballi@okaxis"
    ) : VoiceApplyUiState

    data class Error(
        val message: String
    ) : VoiceApplyUiState
}

class VoiceApplyViewModel(
    private val repository: ConduitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceApplyUiState>(VoiceApplyUiState.Idle)
    val uiState: StateFlow<VoiceApplyUiState> = _uiState.asStateFlow()

    private val _waveformAmplitudes = MutableStateFlow(List(28) { 0.14f })
    val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes.asStateFlow()

    private var recordingJob: Job? = null
    private var timerJob: Job? = null

    fun startRecording() {
        recordingJob?.cancel()
        timerJob?.cancel()
        _uiState.value = VoiceApplyUiState.RecordingVoice(elapsedSeconds = 0)

        recordingJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                tick++
                val bars = List(28) { index ->
                    val centerWeight = 1f - (abs(index - 13.5f) / 15f)
                    val wave = (sin((tick * 0.45f) + index * 0.55f) + 1f) * 0.5f
                    val jitter = Random.nextFloat() * 0.35f
                    (0.15f + (wave * 0.55f + jitter) * centerWeight).coerceIn(0.12f, 1.0f)
                }
                _waveformAmplitudes.value = bars
                delay(70L)
            }
        }

        timerJob = viewModelScope.launch {
            var seconds = 0
            while (isActive && seconds < 8) {
                delay(1_000L)
                seconds++
                val current = _uiState.value
                if (current is VoiceApplyUiState.RecordingVoice) {
                    _uiState.value = current.copy(elapsedSeconds = seconds)
                }
            }
            if (_uiState.value is VoiceApplyUiState.RecordingVoice) {
                stopRecording()
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        timerJob?.cancel()
        _waveformAmplitudes.value = List(28) { 0.14f }
        _uiState.value = VoiceApplyUiState.TranscribingAndTranslating

        viewModelScope.launch {
            runCatching {
                val simulatedAudioPayload = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YQAAAAA="
                repository.transcribeVoiceIntent(simulatedAudioPayload)
            }.onSuccess { intent ->
                _uiState.value = VoiceApplyUiState.AwaitingAAConsent(
                    voiceIntent = intent,
                    showConsentDialog = false
                )
            }.onFailure { throwable ->
                _uiState.value = VoiceApplyUiState.Error(
                    throwable.localizedMessage ?: "Could not understand audio. Please try speaking again."
                )
            }
        }
    }

    fun openAAConsentDialog() {
        val current = _uiState.value as? VoiceApplyUiState.AwaitingAAConsent ?: return
        _uiState.value = current.copy(showConsentDialog = true)
    }

    fun dismissAAConsentDialog() {
        val current = _uiState.value as? VoiceApplyUiState.AwaitingAAConsent ?: return
        _uiState.value = current.copy(showConsentDialog = false, isSubmittingConsent = false)
    }

    fun approveAAConsent(otp: String) {
        val current = _uiState.value as? VoiceApplyUiState.AwaitingAAConsent ?: return
        val sanitizedOtp = otp.filter { it.isDigit() }.ifEmpty { "482910" }
        _uiState.value = current.copy(isSubmittingConsent = true)

        viewModelScope.launch {
            runCatching {
                val profile = repository.profileFlow.first()
                val consent = repository.initiateAAConsent(
                    msmeId = profile.id,
                    phone = "+919845120482",
                    aaHandle = current.aaHandle,
                    otp = sanitizedOtp
                )
                if (!consent.isGranted) {
                    error("Account Aggregator verification was declined.")
                }

                // Smooth 3-stage real-time underwriting pipeline
                _uiState.value = VoiceApplyUiState.UnderwritingInProgress(
                    voiceIntent = current.voiceIntent,
                    currentStep = 1,
                    progressFraction = 0.28f
                )
                delay(1_150L)

                _uiState.value = VoiceApplyUiState.UnderwritingInProgress(
                    voiceIntent = current.voiceIntent,
                    currentStep = 2,
                    progressFraction = 0.64f
                )
                delay(1_150L)

                _uiState.value = VoiceApplyUiState.UnderwritingInProgress(
                    voiceIntent = current.voiceIntent,
                    currentStep = 3,
                    progressFraction = 0.94f
                )
                val assessment = repository.fetchCreditAssessment(profile.id)
                delay(950L)

                _uiState.value = VoiceApplyUiState.LoanApprovedSuccess(
                    voiceIntent = current.voiceIntent,
                    offer = assessment,
                    isDisbursed = false,
                    merchantUpiVpa = profile.upiVpa
                )
            }.onFailure { throwable ->
                _uiState.value = VoiceApplyUiState.Error(
                    throwable.localizedMessage ?: "Verification could not be completed. Please try again."
                )
            }
        }
    }

    fun acceptLoanOffer() {
        val current = _uiState.value as? VoiceApplyUiState.LoanApprovedSuccess ?: return
        if (current.isDisbursed || current.isDisbursing) return

        _uiState.value = current.copy(isDisbursing = true)
        viewModelScope.launch {
            delay(850L)
            repository.acceptLoanOffer(current.offer)
            _uiState.value = current.copy(
                isDisbursing = false,
                isDisbursed = true
            )
        }
    }

    fun resetFlow() {
        recordingJob?.cancel()
        timerJob?.cancel()
        _waveformAmplitudes.value = List(28) { 0.14f }
        _uiState.value = VoiceApplyUiState.Idle
    }

    override fun onCleared() {
        recordingJob?.cancel()
        timerJob?.cancel()
        super.onCleared()
    }

    class Factory(private val repository: ConduitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VoiceApplyViewModel(repository) as T
        }
    }
}
