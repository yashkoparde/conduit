package com.conduit.msme.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conduit.msme.data.model.ConduitLanguage
import com.conduit.msme.ui.components.CreditSolvencyGauge
import com.conduit.msme.ui.components.formatInrNumber
import com.conduit.msme.ui.theme.ConduitTheme
import com.conduit.msme.ui.theme.ConduitThemeTokens
import com.conduit.msme.ui.viewmodel.HomeUiState
import com.example.R

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleLanguage: () -> Unit,
    onNavigateToVoiceApply: () -> Unit,
    onNavigateToPassport: () -> Unit,
    onNavigateToRepayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ConduitThemeTokens.extendedColors
    val isKannada = uiState.language == ConduitLanguage.KANNADA

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
            // 1. Vernacular Merchant Greeting Header + Language Toggle
            item {
                MerchantGreetingHeader(
                    onToggleLanguage = onToggleLanguage
                )
            }

            // 2. Hero Action Card — Festival Working Capital with Voice CTA
            item {
                FestivalVoiceHeroCard(
                    onSpeakClick = onNavigateToVoiceApply
                )
            }

            // 3. Animated Credit Solvency Gauge Card
            item {
                CreditSolvencyCard(
                    score = uiState.digitalTwin.creditScore,
                    maxScore = uiState.digitalTwin.maxScore,
                    tierLabel = if (isKannada) {
                        uiState.digitalTwin.tierLabelKn
                    } else {
                        uiState.digitalTwin.tierLabelEn
                    },
                    onOpenPassport = onNavigateToPassport
                )
            }

            // 4. High-Impact Merchant Stat Badges
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBadgeCard(
                        icon = Icons.Filled.Storefront,
                        iconTint = extended.cyanAccent,
                        iconContainer = extended.cyanContainer,
                        label = stringResource(R.string.home_stat_daily_upi_label),
                        value = "₹${formatInrNumber(uiState.digitalTwin.dailyUpiTurnover)}",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadgeCard(
                        icon = Icons.Filled.TrendingUp,
                        iconTint = extended.karnatakaGold,
                        iconContainer = extended.karnatakaGoldContainer,
                        label = stringResource(R.string.home_stat_consistency_label),
                        value = "${uiState.digitalTwin.consistencyPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadgeCard(
                        icon = Icons.Filled.Savings,
                        iconTint = extended.successGreen,
                        iconContainer = extended.successContainer,
                        label = stringResource(R.string.home_stat_saved_label),
                        value = "₹${formatInrNumber(uiState.repaymentProgress.interestSavedVsMoneylender)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Active Loan Repayment Card
            item {
                ActiveLoanSummaryCard(
                    repaid = uiState.repaymentProgress.amountRepaid,
                    total = uiState.repaymentProgress.totalLoanAmount,
                    daysRemaining = uiState.repaymentProgress.daysRemaining,
                    onViewTracker = onNavigateToRepayment
                )
            }

            // 6. Sovereign Financial Passport Shortcut Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_open_passport_cta")
                        .clickable(onClick = onNavigateToPassport),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extended.cardBorder),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = extended.karnatakaGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_quick_passport_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.home_quick_passport_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = extended.cyanAccent
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun MerchantGreetingHeader(
    onToggleLanguage: () -> Unit
) {
    val extended = ConduitThemeTokens.extendedColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_merchant_ravi_1795182815550),
                contentDescription = stringResource(R.string.home_greeting_primary),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, extended.karnatakaGold, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_greeting_primary),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_business_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedButton(
            onClick = onToggleLanguage,
            modifier = Modifier
                .height(48.dp)
                .testTag("language_toggle_button"),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.5.dp, extended.cyanAccent),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = stringResource(R.string.language_toggle_cd),
                tint = extended.cyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.language_toggle_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FestivalVoiceHeroCard(
    onSpeakClick: () -> Unit
) {
    val extended = ConduitThemeTokens.extendedColors
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = extended.heroDarkStart)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_festival_banner_1795182821719),
                contentDescription = stringResource(R.string.home_hero_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // High-contrast scrim preserving the warm kirana art on the right
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to extended.heroDarkStart.copy(alpha = 0.96f),
                            0.58f to extended.heroDarkStart.copy(alpha = 0.86f),
                            1.0f to extended.heroDarkStart.copy(alpha = 0.42f)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = extended.karnatakaGold.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, extended.karnatakaGold.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = extended.karnatakaGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_hero_festival_tag),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.home_hero_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(0.86f)
                )

                Text(
                    text = stringResource(R.string.home_hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.fillMaxWidth(0.84f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = onSpeakClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("home_voice_apply_cta"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.cyanAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.home_hero_cta),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditSolvencyCard(
    score: Int,
    maxScore: Int,
    tierLabel: String,
    onOpenPassport: () -> Unit
) {
    val extended = ConduitThemeTokens.extendedColors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPassport),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CreditSolvencyGauge(
                score = score,
                maxScore = maxScore,
                tierLabel = tierLabel,
                outOfLabel = stringResource(R.string.home_solvency_out_of),
                size = 136.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = extended.cyanContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = extended.onCyanContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_verified_merchant_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.onCyanContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.home_solvency_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.home_solvency_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = extended.karnatakaGoldContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_preapproved_limit),
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.onKarnatakaGoldContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadgeCard(
    icon: ImageVector,
    iconTint: Color,
    iconContainer: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val extended = ConduitThemeTokens.extendedColors
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.cardBorder),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveLoanSummaryCard(
    repaid: Double,
    total: Double,
    daysRemaining: Int,
    onViewTracker: () -> Unit
) {
    val extended = ConduitThemeTokens.extendedColors
    val rawProgress = if (total > 0.0) (repaid / total).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "active_loan_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_view_repayment_cta")
            .clickable(onClick = onViewTracker),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.home_active_loan_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = extended.successContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = extended.successGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_active_loan_autopay_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.successGreen
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.home_active_loan_daily_sweep),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = extended.successGreenBright,
                trackColor = extended.subtleSurface,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.home_active_loan_progress_format,
                        formatInrNumber(repaid),
                        formatInrNumber(total)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.home_active_loan_days_left, daysRemaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = extended.cyanAccent
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Home Screen - English Light")
@Composable
private fun HomeScreenEnglishPreview() {
    ConduitTheme(isKannada = false) {
        HomeScreen(
            uiState = HomeUiState(language = ConduitLanguage.ENGLISH),
            onToggleLanguage = {},
            onNavigateToVoiceApply = {},
            onNavigateToPassport = {},
            onNavigateToRepayment = {}
        )
    }
}

@Preview(showBackground = true, locale = "kn", name = "Home Screen - Kannada")
@Composable
private fun HomeScreenKannadaPreview() {
    ConduitTheme(isKannada = true) {
        HomeScreen(
            uiState = HomeUiState(language = ConduitLanguage.KANNADA),
            onToggleLanguage = {},
            onNavigateToVoiceApply = {},
            onNavigateToPassport = {},
            onNavigateToRepayment = {}
        )
    }
}
