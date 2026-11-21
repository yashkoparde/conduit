package com.conduit.msme.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conduit.msme.data.model.ConduitLanguage
import com.conduit.msme.data.model.SweepTransaction
import com.conduit.msme.ui.components.CelebratoryConfettiOverlay
import com.conduit.msme.ui.components.formatInrNumber
import com.conduit.msme.ui.theme.ConduitTheme
import com.conduit.msme.ui.theme.ConduitThemeTokens
import com.conduit.msme.ui.viewmodel.RepaymentUiState
import com.example.R
import kotlin.math.roundToInt

@Composable
fun RepaymentScreen(
    uiState: RepaymentUiState,
    language: ConduitLanguage,
    onSimulateDailySweep: () -> Unit,
    onDownloadCertificate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ConduitThemeTokens.extendedColors
    val isKannada = language == ConduitLanguage.KANNADA
    val progress = uiState.progress
    val fraction = if (progress.totalLoanAmount > 0.0) {
        (progress.amountRepaid / progress.totalLoanAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

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
                        text = stringResource(R.string.repayment_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.repayment_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Big Circular Progress Tracker Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.cardBorder),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RepaymentCircularRing(
                            progressFraction = fraction,
                            repaidText = formatInrNumber(progress.amountRepaid),
                            totalText = formatInrNumber(progress.totalLoanAmount)
                        )

                        // Today's Status Green Badge
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
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = extended.successGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = stringResource(
                                        R.string.repayment_today_status,
                                        formatInrNumber(progress.dailySweepAmount)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = extended.successGreen
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = extended.subtleSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stringResource(R.string.repayment_remaining_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${formatInrNumber(progress.remainingBalance)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Surface(
                                color = extended.cyanContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stringResource(R.string.repayment_days_left_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extended.onCyanContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${progress.daysRemaining}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.onCyanContainer
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onSimulateDailySweep,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("simulate_sweep_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extended.cyanAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.sweepRecordedJustNow) {
                                    Icons.Filled.Verified
                                } else {
                                    Icons.Filled.Autorenew
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.sweepRecordedJustNow) {
                                    stringResource(R.string.repayment_simulate_done_cta)
                                } else {
                                    stringResource(R.string.repayment_simulate_cta)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // 2. "Freedom from Moneylenders" Comparison Meter
            item {
                MoneylenderComparisonCard(
                    interestSaved = progress.interestSavedVsMoneylender
                )
            }

            // 3. Download Formal No-Due Certificate / CIBIL Upgraded Report
            item {
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = extended.karnatakaGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.repayment_cibil_boost_note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AnimatedVisibility(visible = uiState.certificateReady) {
                            Surface(
                                color = extended.successContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = extended.successGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.repayment_download_success),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = extended.successGreen
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onDownloadCertificate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("download_cibil_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.repayment_download_cta),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 4. Recent Daily UPI Sweeps History
            item {
                Text(
                    text = stringResource(R.string.repayment_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(uiState.recentSweeps, key = { it.id }) { tx ->
                SweepHistoryRow(transaction = tx, isKannada = isKannada)
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        CelebratoryConfettiOverlay(visible = uiState.showConfetti)
    }
}

@Composable
private fun RepaymentCircularRing(
    progressFraction: Float,
    repaidText: String,
    totalText: String
) {
    val extended = ConduitThemeTokens.extendedColors
    val animatedSweep by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "repayment_ring"
    )
    val trackColor = extended.subtleSurface
    val activeColor = extended.successGreenBright
    val cyanAccent = extended.cyanAccent
    val pct = (animatedSweep * 100f).roundToInt()

    Box(
        modifier = Modifier.size(214.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Vibrant gradient arc for fluid visual progression
            drawArc(
                brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                    listOf(cyanAccent, activeColor, extended.karnatakaGold, cyanAccent)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Surface(
                color = extended.cyanContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = cyanAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "₹$repaidText",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.repayment_repaid_format, repaidText, totalText),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MoneylenderComparisonCard(
    interestSaved: Double
) {
    val extended = ConduitThemeTokens.extendedColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = extended.successGreen,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.repayment_comparison_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.repayment_comparison_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Street Moneylender (Meter Vaddi) 48% APR Bar
            ComparisonBarItem(
                label = stringResource(R.string.repayment_moneylender_label),
                valueText = stringResource(R.string.repayment_moneylender_value),
                fraction = 1.0f,
                barColor = extended.warningRed,
                valueColor = extended.warningRed
            )

            // Conduit Prime Bank 13.2% APR Bar
            ComparisonBarItem(
                label = stringResource(R.string.repayment_conduit_label),
                valueText = stringResource(R.string.repayment_conduit_value),
                fraction = 0.717f,
                barColor = extended.successGreenBright,
                valueColor = extended.successGreen
            )

            HorizontalDivider(color = extended.cardBorder)

            // Green Savings Highlight Badge
            Surface(
                color = extended.successContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Savings,
                        contentDescription = null,
                        tint = extended.successGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.repayment_saved_banner,
                            formatInrNumber(interestSaved)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.successGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonBarItem(
    label: String,
    valueText: String,
    fraction: Float,
    barColor: Color,
    valueColor: Color
) {
    val extended = ConduitThemeTokens.extendedColors
    val animatedWidth by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "comparison_bar"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(extended.subtleSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth)
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun SweepHistoryRow(
    transaction: SweepTransaction,
    isKannada: Boolean
) {
    val extended = ConduitThemeTokens.extendedColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(extended.successContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = extended.successGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "${if (isKannada) transaction.dateLabelKn else transaction.dateLabelEn} • ${transaction.timeLabel}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = transaction.upiRef,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "₹${formatInrNumber(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = extended.successGreen
            )
        }
    }
}

@Preview(showBackground = true, name = "Repayment Screen - English Light")
@Composable
private fun RepaymentEnglishPreview() {
    ConduitTheme(isKannada = false) {
        RepaymentScreen(
            uiState = RepaymentUiState(
                recentSweeps = listOf(
                    SweepTransaction("SW-84", "Today", "ಇಂದು", "9:00 PM", 220.0, "UPI/432519082144", "SUCCESS")
                )
            ),
            language = ConduitLanguage.ENGLISH,
            onSimulateDailySweep = {},
            onDownloadCertificate = {}
        )
    }
}

@Preview(showBackground = true, locale = "kn", name = "Repayment Screen - Kannada")
@Composable
private fun RepaymentKannadaPreview() {
    ConduitTheme(isKannada = true) {
        RepaymentScreen(
            uiState = RepaymentUiState(
                recentSweeps = listOf(
                    SweepTransaction("SW-84", "Today", "ಇಂದು", "9:00 PM", 220.0, "UPI/432519082144", "SUCCESS")
                )
            ),
            language = ConduitLanguage.KANNADA,
            onSimulateDailySweep = {},
            onDownloadCertificate = {}
        )
    }
}
