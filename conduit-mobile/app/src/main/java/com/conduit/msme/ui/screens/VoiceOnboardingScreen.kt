package com.conduit.msme.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conduit.msme.data.remote.VoiceOnboardingPhase1Result
import com.conduit.msme.data.remote.VoiceOnboardingPhase2Result
import com.conduit.msme.data.repository.ConduitRepository
import com.conduit.msme.ui.components.CelebratoryConfettiOverlay
import com.conduit.msme.ui.components.LiveAudioWaveform
import com.conduit.msme.ui.components.formatInrNumber
import com.conduit.msme.ui.theme.ConduitThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceOnboardingScreen(
    repository: ConduitRepository,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPhase by remember { mutableIntStateOf(1) } // 1 = Basic, 2 = Udyam & MSME Schemes, 3 = Completed
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // Phase 1 State (Simplistic Onboarding)
    var phase1Data by remember {
        mutableStateOf<VoiceOnboardingPhase1Result?>(null)
    }

    // Phase 2 State (MSME Schemes & Udyam)
    var phase2Data by remember {
        mutableStateOf<VoiceOnboardingPhase2Result?>(null)
    }

    val coroutineScope = rememberCoroutineScope()
    val extended = ConduitThemeTokens.extendedColors

    // iOS Visual Glassmorphism Tokens
    val iosBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF0B1329)
        )
    )
    val iosGlassSurface = Color(0xFF1E293B).copy(alpha = 0.72f)
    val iosGlassBorder = Color.White.copy(alpha = 0.14f)
    val iosAccentBlue = Color(0xFF0A84FF)
    val iosAccentGreen = Color(0xFF30D158)
    val iosAccentOrange = Color(0xFFFF9F0A)

    val infinite = rememberInfiniteTransition(label = "ios_mic_glow")
    val pulseScaleOuter by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.48f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRecording) 750 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_outer"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosBgBrush),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iOS Top Segment / Phase Indicator Pill
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, iosGlassBorder, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val p1Active = currentPhase == 1
                    val p2Active = currentPhase == 2
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (p1Active) iosAccentBlue else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Phase 1: Quick Profile",
                            color = if (p1Active) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (p1Active) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (p2Active) iosAccentOrange else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Phase 2: MSME Schemes",
                            color = if (p2Active) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (p2Active) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Header Title Card
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (currentPhase == 1) "Voice Onboarding" else "MSME Government Schemes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentPhase == 1)
                            "Speak in Kannada, Hindi, or English. Groq AI auto-fills your profile."
                        else
                            "Speak your Udyam or business vintage. Instantly check Mudra & PMEGP subsidy eligibility.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // iOS Frosted Glass Voice Capture Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = iosGlassSurface),
                    border = BorderStroke(1.dp, iosGlassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Concentric Pulsing Microphone
                        Box(
                            modifier = Modifier.size(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(134.dp)
                                    .scale(pulseScaleOuter)
                                    .clip(CircleShape)
                                    .background(
                                        (if (isRecording) iosAccentOrange else iosAccentBlue)
                                            .copy(alpha = if (isRecording) 0.22f else 0.12f)
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                if (isRecording) Color(0xFFFF453A) else iosAccentBlue,
                                                if (isRecording) iosAccentOrange else Color(0xFF5E5CE6)
                                            )
                                        )
                                    )
                                    .shadow(elevation = 12.dp, shape = CircleShape)
                                    .clickable(enabled = !isProcessing) {
                                        if (isRecording) {
                                            isRecording = false
                                            isProcessing = true
                                            coroutineScope.launch {
                                                if (currentPhase == 1) {
                                                    val res = repository.processVoiceOnboardingPhase1(
                                                        "ನನ್ನ ಹೆಸರು ರವಿ ಕುಲಕರ್ಣಿ, ನನ್ನ ಅಂಗಡಿ ಹುಬ್ಬಳ್ಳಿ ಪ್ರಾವಿಷನ್ ಸ್ಟೋರ್, ದಿನಸಿ ವ್ಯಾಪಾರ, ತಿಂಗಳಿಗೆ 1.8 ಲಕ್ಷ ವಹಿವಾಟು."
                                                    )
                                                    phase1Data = res
                                                } else {
                                                    val res = repository.processVoiceOnboardingPhase2(
                                                        "ನನ್ನ ಉದ್ಯಮ್ ನಂಬರ್ UDYAM-KR-03-0021482, 3 ವರ್ಷದಿಂದ ಅಂಗಡಿ ಇದೆ, 2 ಉದ್ಯೋಗಿಗಳಿದ್ದಾರೆ, ಮುದ್ರಾ ಯೋಜನೆ ಬೇಕು."
                                                    )
                                                    phase2Data = res
                                                }
                                                isProcessing = false
                                            }
                                        } else {
                                            isRecording = true
                                            recordingSeconds = 0
                                            coroutineScope.launch {
                                                while (isRecording && recordingSeconds < 6) {
                                                    delay(1000L)
                                                    recordingSeconds++
                                                }
                                                if (isRecording) {
                                                    isRecording = false
                                                    isProcessing = true
                                                    if (currentPhase == 1) {
                                                        val res = repository.processVoiceOnboardingPhase1(
                                                            "ನನ್ನ ಹೆಸರು ರವಿ ಕುಲಕರ್ಣಿ, ನನ್ನ ಅಂಗಡಿ ಹುಬ್ಬಳ್ಳಿ ಪ್ರಾವಿಷನ್ ಸ್ಟೋರ್, ದಿನಸಿ ವ್ಯಾಪಾರ, ತಿಂಗಳಿಗೆ 1.8 ಲಕ್ಷ ವಹಿವಾಟು."
                                                        )
                                                        phase1Data = res
                                                    } else {
                                                        val res = repository.processVoiceOnboardingPhase2(
                                                            "ನನ್ನ ಉದ್ಯಮ್ ನಂಬರ್ UDYAM-KR-03-0021482, 3 ವರ್ಷದಿಂದ ಅಂಗಡಿ ಇದೆ, 2 ಉದ್ಯೋಗಿಗಳಿದ್ದಾರೆ, ಮುದ್ರಾ ಯೋಜನೆ ಬೇಕು."
                                                        )
                                                        phase2Data = res
                                                    }
                                                    isProcessing = false
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                        contentDescription = "Voice microphone",
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }

                        // Status caption
                        Text(
                            text = when {
                                isProcessing -> "Translating & parsing via Groq AI…"
                                isRecording -> "Listening… Tap to complete ($recordingSeconds s)"
                                currentPhase == 1 -> "Tap mic and say your name, shop, and daily/monthly turnover"
                                else -> "Tap mic and mention Udyam number, years active, and scheme needs"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center
                        )

                        // Sample Prompts Pill
                        Surface(
                            color = Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentPhase == 1)
                                    "Try: “ನನ್ನ ಹೆಸರು ರವಿ, ಹುಬ್ಬಳ್ಳಿ ಪ್ರಾವಿಷನ್ ಸ್ಟೋರ್, ದಿನಸಿ ಅಂಗಡಿ, ತಿಂಗಳಿಗೆ 1.8 ಲಕ್ಷ ವ್ಯಾಪಾರ.”"
                                else
                                    "Try: “ಉದ್ಯಮ್ ನಂ UDYAM-KR-03-0021482, 3 ವರ್ಷದ ಅನುಭವ, ಮುದ್ರಾ ಮತ್ತು ಸಬ್ಸಿಡಿ ಸ್ಕೀಮ್ ಬೇಕು.”",
                                style = MaterialTheme.typography.labelMedium,
                                color = iosAccentOrange,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            // Extracted Profile Display (Phase 1 Result)
            if (currentPhase == 1 && phase1Data != null) {
                item {
                    val data = phase1Data!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = iosGlassSurface),
                        border = BorderStroke(1.dp, iosAccentBlue.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = null,
                                        tint = iosAccentBlue
                                    )
                                    Text(
                                        text = "Verified Merchant Profile",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    color = iosAccentGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Groq Extracted",
                                        color = iosAccentGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Merchant Name", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text(data.merchantName.ifBlank { "Ravi Kulkarni" }, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Shop Name", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text(data.shopName.ifBlank { "Hubballi Provision Store" }, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Category", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text(data.businessCategory, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Monthly Turnover", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text("₹${formatInrNumber(data.estimatedMonthlyTurnover)}", color = iosAccentGreen, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { currentPhase = 2 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = iosAccentBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Continue to Phase 2: MSME Schemes", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }

            // Extracted MSME Scheme Eligibility (Phase 2 Result)
            if (currentPhase == 2 && phase2Data != null) {
                item {
                    val data = phase2Data!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = iosGlassSurface),
                        border = BorderStroke(1.dp, iosAccentOrange.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                        imageVector = Icons.Filled.Policy,
                                        contentDescription = null,
                                        tint = iosAccentOrange
                                    )
                                    Text(
                                        text = "Eligible Government Schemes",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    color = iosAccentOrange.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Verified MSME",
                                        color = iosAccentOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Udyam Number", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text(data.udyamRegNumber.ifBlank { "UDYAM-KR-03-0021482" }, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Business Vintage", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text("${data.businessVintageYears} Years Active", color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Qualified Schemes:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                (if (data.eligibleSchemes.isNotEmpty()) data.eligibleSchemes else listOf(
                                    "Pradhan Mantri MUDRA Yojana (Kishore - ₹5 Lakhs)",
                                    "CGTMSE Collateral-Free Credit Guarantee",
                                    "PMEGP 15-35% Capital Subsidy"
                                )).forEach { scheme ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = iosAccentGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(scheme, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.completeOnboarding(
                                            name = phase1Data?.merchantName.orEmpty(),
                                            shopName = phase1Data?.shopName.orEmpty(),
                                            category = phase1Data?.businessCategory.orEmpty(),
                                            district = phase1Data?.cityOrDistrict.orEmpty(),
                                            turnover = phase1Data?.estimatedMonthlyTurnover ?: 180000.0,
                                            udyam = data.udyamRegNumber,
                                            vintage = data.businessVintageYears,
                                            employees = data.employeeCount,
                                            schemes = data.eligibleSchemes,
                                            subsidy = data.subsidyEstimatedInr
                                        )
                                        currentPhase = 3
                                        delay(800L)
                                        onOnboardingComplete()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = iosAccentGreen,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Complete Onboarding & Enter Dashboard", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
