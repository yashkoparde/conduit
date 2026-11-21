package com.conduit.msme.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conduit.msme.data.model.CreditAssessmentResponse
import com.conduit.msme.data.model.VoiceIntentResponse
import com.conduit.msme.ui.components.CelebratoryConfettiOverlay
import com.conduit.msme.ui.components.LiveAudioWaveform
import com.conduit.msme.ui.components.formatInrNumber
import com.conduit.msme.ui.theme.ConduitTheme
import com.conduit.msme.ui.theme.ConduitThemeTokens
import com.conduit.msme.ui.theme.NotoSansKannadaFontFamily
import com.conduit.msme.ui.viewmodel.VoiceApplyUiState
import com.example.R

@Composable
fun VoiceApplyScreen(
    uiState: VoiceApplyUiState,
    waveformAmplitudes: List<Float>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onOpenAAConsent: () -> Unit,
    onDismissAAConsent: () -> Unit,
    onApproveAAConsent: (String) -> Unit,
    onAcceptLoanOffer: () -> Unit,
    onResetFlow: () -> Unit,
    onNavigateToRepayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ConduitThemeTokens.extendedColors
    val activeStep = when (uiState) {
        is VoiceApplyUiState.Idle,
        is VoiceApplyUiState.RecordingVoice,
        is VoiceApplyUiState.TranscribingAndTranslating -> 1
        is VoiceApplyUiState.AwaitingAAConsent,
        is VoiceApplyUiState.UnderwritingInProgress -> 2
        is VoiceApplyUiState.LoanApprovedSuccess -> 3
        is VoiceApplyUiState.Error -> 1
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.voice_screen_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.voice_screen_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3-Step Friendly Stepper
            item {
                VoiceFlowStepper(activeStep = activeStep)
            }

            // Main State Content
            item {
                when (uiState) {
                    is VoiceApplyUiState.Idle -> {
                        VoiceCaptureCard(
                            isRecording = false,
                            isTranscribing = false,
                            elapsedSeconds = 0,
                            waveformAmplitudes = waveformAmplitudes,
                            onMicToggle = onStartRecording
                        )
                    }

                    is VoiceApplyUiState.RecordingVoice -> {
                        VoiceCaptureCard(
                            isRecording = true,
                            isTranscribing = false,
                            elapsedSeconds = uiState.elapsedSeconds,
                            waveformAmplitudes = waveformAmplitudes,
                            onMicToggle = onStopRecording
                        )
                    }

                    is VoiceApplyUiState.TranscribingAndTranslating -> {
                        VoiceCaptureCard(
                            isRecording = false,
                            isTranscribing = true,
                            elapsedSeconds = 0,
                            waveformAmplitudes = waveformAmplitudes,
                            onMicToggle = {}
                        )
                    }

                    is VoiceApplyUiState.AwaitingAAConsent -> {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 5 }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                RealTimeTranscriptionCard(intent = uiState.voiceIntent)
                                AutoExtractedTermsCard(intent = uiState.voiceIntent)

                                Button(
                                    onClick = onOpenAAConsent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("open_aa_consent_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = extended.cyanAccent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.voice_proceed_aa_cta),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                OutlinedButton(
                                    onClick = onResetFlow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("record_again_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, extended.cardBorder)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.voice_record_again_cta),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    is VoiceApplyUiState.UnderwritingInProgress -> {
                        UnderwritingProgressCard(
                            currentStep = uiState.currentStep,
                            progressFraction = uiState.progressFraction
                        )
                    }

                    is VoiceApplyUiState.LoanApprovedSuccess -> {
                        ApprovedOfferCard(
                            offer = uiState.offer,
                            isDisbursing = uiState.isDisbursing,
                            isDisbursed = uiState.isDisbursed,
                            merchantUpiVpa = uiState.merchantUpiVpa,
                            onDisburseClick = onAcceptLoanOffer,
                            onGoToRepayment = onNavigateToRepayment,
                            onStartNew = onResetFlow
                        )
                    }

                    is VoiceApplyUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = extended.warningContainer),
                            border = BorderStroke(1.dp, extended.warningRed),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = extended.warningRed,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = uiState.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = extended.onWarningContainer,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onResetFlow,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("voice_error_retry_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(text = stringResource(R.string.voice_error_retry))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // Celebratory Confetti when disbursed
        if (uiState is VoiceApplyUiState.LoanApprovedSuccess && uiState.isDisbursed) {
            CelebratoryConfettiOverlay(visible = true)
        }

        // 1-Tap RBI Account Aggregator Modal Dialog
        if (uiState is VoiceApplyUiState.AwaitingAAConsent && uiState.showConsentDialog) {
            AccountAggregatorConsentDialog(
                aaHandle = uiState.aaHandle,
                isSubmitting = uiState.isSubmittingConsent,
                onDismiss = onDismissAAConsent,
                onApprove = onApproveAAConsent
            )
        }
    }
}

@Composable
private fun VoiceFlowStepper(activeStep: Int) {
    val extended = ConduitThemeTokens.extendedColors
    val steps = listOf(
        1 to stringResource(R.string.voice_step_speak),
        2 to stringResource(R.string.voice_step_verify),
        3 to stringResource(R.string.voice_step_disburse)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        steps.forEach { (stepNum, label) ->
            val isCompleted = activeStep > stepNum
            val isCurrent = activeStep == stepNum
            val container = when {
                isCompleted -> extended.successContainer
                isCurrent -> extended.cyanContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val content = when {
                isCompleted -> extended.successGreen
                isCurrent -> extended.onCyanContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                modifier = Modifier.weight(1f),
                color = container,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCurrent) extended.cyanAccent else extended.cardBorder
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                    color = content,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceCaptureCard(
    isRecording: Boolean,
    isTranscribing: Boolean,
    elapsedSeconds: Int,
    waveformAmplitudes: List<Float>,
    onMicToggle: () -> Unit
) {
    val extended = ConduitThemeTokens.extendedColors
    val infinite = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScaleOuter by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.55f else 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 800 else 1_300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_outer"
    )
    val pulseScaleInner by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.32f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 650 else 1_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_inner"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (isRecording) {
                Surface(
                    color = extended.warningContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "● REC  0:0$elapsedSeconds",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.warningRed,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Hero Mic Button with pulsing concentric rings
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .scale(pulseScaleOuter)
                        .clip(CircleShape)
                        .background(
                            (if (isRecording) extended.karnatakaGold else extended.cyanAccent)
                                .copy(alpha = if (isRecording) 0.16f else 0.08f)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(pulseScaleInner)
                        .clip(CircleShape)
                        .background(
                            (if (isRecording) extended.cyanAccent else extended.cyanAccent)
                                .copy(alpha = if (isRecording) 0.25f else 0.14f)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isRecording) {
                                    listOf(extended.warningRed, extended.karnatakaGold)
                                } else {
                                    listOf(MaterialTheme.colorScheme.primary, extended.cyanAccent)
                                }
                            )
                        )
                        .testTag("hero_mic_button")
                        .clickable(enabled = !isTranscribing, onClick = onMicToggle),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = stringResource(R.string.voice_mic_button_cd),
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            // Live Audio Waveform Visualizer
            LiveAudioWaveform(
                amplitudes = waveformAmplitudes,
                isActive = isRecording || isTranscribing,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Text(
                text = when {
                    isTranscribing -> stringResource(R.string.voice_mic_transcribing_hint)
                    isRecording -> stringResource(R.string.voice_mic_recording_hint)
                    else -> stringResource(R.string.voice_mic_idle_hint)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (!isRecording && !isTranscribing) {
                Surface(
                    color = extended.karnatakaGoldContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.voice_mic_sample_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.onKarnatakaGoldContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Button(
                    onClick = onMicToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("tap_to_speak_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.cyanAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.voice_mic_tap_to_speak),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else if (isRecording) {
                Button(
                    onClick = onMicToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("tap_to_stop_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.warningRed,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.voice_mic_tap_to_stop),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun RealTimeTranscriptionCard(intent: VoiceIntentResponse) {
    val extended = ConduitThemeTokens.extendedColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = extended.cyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.voice_transcription_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = extended.karnatakaGoldContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stringResource(R.string.voice_detected_kannada),
                        style = MaterialTheme.typography.labelSmall,
                        color = extended.onKarnatakaGoldContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Original Kannada speech bubble
            Surface(
                color = extended.subtleSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.voice_original_speech_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "“${intent.transcribedText}”",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NotoSansKannadaFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // English Translation
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = stringResource(R.string.voice_english_translation_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "“${intent.englishTranslation}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AutoExtractedTermsCard(intent: VoiceIntentResponse) {
    val extended = ConduitThemeTokens.extendedColors
    val tenureDays = intent.inferredTenureMonths * 30
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.voice_extracted_terms_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExtractedTermTile(
                    label = stringResource(R.string.voice_term_amount_label),
                    value = "₹${formatInrNumber(intent.requestedAmount)}",
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                ExtractedTermTile(
                    label = stringResource(R.string.voice_term_tenure_label),
                    value = stringResource(R.string.voice_term_days_format, tenureDays),
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExtractedTermTile(
                    label = stringResource(R.string.voice_term_daily_label),
                    value = "₹220",
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                ExtractedTermTile(
                    label = stringResource(R.string.voice_term_purpose_label),
                    value = intent.businessPurpose,
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ExtractedTermTile(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    val extended = ConduitThemeTokens.extendedColors
    Surface(
        modifier = modifier,
        color = if (highlight) extended.cyanContainer else extended.subtleSurface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) extended.onCyanContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) extended.onCyanContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AccountAggregatorConsentDialog(
    aaHandle: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onApprove: (String) -> Unit
) {
    val extended = ConduitThemeTokens.extendedColors
    var otpValue by remember { mutableStateOf("482910") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Surface(
                color = extended.successContainer,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = extended.successGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = extended.successContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stringResource(R.string.aa_modal_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = extended.successGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.aa_modal_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.aa_modal_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Surface(
                    color = extended.subtleSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.aa_modal_handle_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = aaHandle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                OutlinedTextField(
                    value = otpValue,
                    onValueChange = { if (it.length <= 6) otpValue = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.aa_modal_otp_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aa_otp_input")
                )
                Text(
                    text = stringResource(R.string.aa_modal_privacy_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.successGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApprove(otpValue) },
                enabled = !isSubmitting && otpValue.length >= 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("aa_approve_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extended.successGreen,
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.aa_modal_approve_cta),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.aa_modal_cancel_cta))
            }
        }
    )
}

@Composable
private fun UnderwritingProgressCard(
    currentStep: Int,
    progressFraction: Float
) {
    val extended = ConduitThemeTokens.extendedColors
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "underwriting_progress"
    )
    val steps = listOf(
        1 to stringResource(R.string.underwriting_step_1),
        2 to stringResource(R.string.underwriting_step_2),
        3 to stringResource(R.string.underwriting_step_3)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.underwriting_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.underwriting_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = extended.cyanAccent,
                trackColor = extended.subtleSurface,
                strokeCap = StrokeCap.Round
            )

            steps.forEach { (stepNum, title) ->
                val isDone = currentStep > stepNum
                val isActive = currentStep == stepNum
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when {
                        isDone -> Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.underwriting_step_done),
                            tint = extended.successGreenBright,
                            modifier = Modifier.size(24.dp)
                        )
                        isActive -> CircularProgressIndicator(
                            color = extended.cyanAccent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(extended.subtleSurface)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive || isDone) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovedOfferCard(
    offer: CreditAssessmentResponse,
    isDisbursing: Boolean,
    isDisbursed: Boolean,
    merchantUpiVpa: String,
    onDisburseClick: () -> Unit,
    onGoToRepayment: () -> Unit,
    onStartNew: () -> Unit
) {
    val extended = ConduitThemeTokens.extendedColors
    val formattedAmount = formatInrNumber(offer.approvedAmount)
    val formattedSweep = formatInrNumber(offer.dailySweepAmount)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Celebratory Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(extended.heroDarkStart, extended.successGreen)
                        )
                    )
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = extended.karnatakaGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.offer_approved_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = if (isDisbursed) {
                            stringResource(R.string.offer_disbursed_title)
                        } else {
                            stringResource(R.string.offer_approved_title, formattedAmount)
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )

                    Text(
                        text = if (isDisbursed) {
                            stringResource(
                                R.string.offer_disbursed_subtitle,
                                formattedAmount,
                                merchantUpiVpa,
                                formattedSweep
                            )
                        } else {
                            stringResource(R.string.offer_approved_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtractedTermTile(
                        label = stringResource(R.string.offer_rate_label),
                        value = stringResource(R.string.offer_rate_value, offer.interestRateApr.toString()),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    ExtractedTermTile(
                        label = stringResource(R.string.offer_daily_sweep_label),
                        value = stringResource(R.string.offer_daily_sweep_value, formattedSweep),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Savings Callout
                Surface(
                    color = extended.successContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Savings,
                            contentDescription = null,
                            tint = extended.successGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.offer_savings_callout),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = extended.successGreen
                        )
                    }
                }

                HorizontalDivider(color = extended.cardBorder)

                if (!isDisbursed) {
                    Button(
                        onClick = onDisburseClick,
                        enabled = !isDisbursing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("disburse_upi_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extended.successGreen,
                            contentColor = Color.White
                        )
                    ) {
                        if (isDisbursing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.offer_disburse_cta),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onGoToRepayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("open_repayment_after_disburse_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extended.cyanAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.offer_go_to_repayment_cta),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onStartNew,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, extended.cardBorder)
                    ) {
                        Text(text = stringResource(R.string.offer_apply_new_cta))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Voice Apply - Idle Light")
@Composable
private fun VoiceApplyIdlePreview() {
    ConduitTheme(isKannada = false) {
        VoiceApplyScreen(
            uiState = VoiceApplyUiState.Idle,
            waveformAmplitudes = List(28) { 0.2f },
            onStartRecording = {},
            onStopRecording = {},
            onOpenAAConsent = {},
            onDismissAAConsent = {},
            onApproveAAConsent = {},
            onAcceptLoanOffer = {},
            onResetFlow = {},
            onNavigateToRepayment = {}
        )
    }
}

@Preview(showBackground = true, locale = "kn", name = "Voice Apply - Kannada Transcription")
@Composable
private fun VoiceApplyKannadaPreview() {
    ConduitTheme(isKannada = true) {
        VoiceApplyScreen(
            uiState = VoiceApplyUiState.AwaitingAAConsent(
                voiceIntent = VoiceIntentResponse(
                    detectedLanguage = "kn",
                    transcribedText = "ನನಗೆ ದೀಪಾವಳಿ ಹಬ್ಬದ ಸಾಮಾನಿಗೆ 50,000 ರೂಪಾಯಿ 3 ತಿಂಗಳಿಗೆ ಬೇಕು.",
                    requestedAmount = 50_000.0,
                    inferredTenureMonths = 3,
                    businessPurpose = "Diwali Festival Grocery Stock"
                )
            ),
            waveformAmplitudes = List(28) { 0.35f },
            onStartRecording = {},
            onStopRecording = {},
            onOpenAAConsent = {},
            onDismissAAConsent = {},
            onApproveAAConsent = {},
            onAcceptLoanOffer = {},
            onResetFlow = {},
            onNavigateToRepayment = {}
        )
    }
}
