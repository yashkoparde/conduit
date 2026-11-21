package com.conduit.msme.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.conduit.msme.data.model.MonthlyInflow
import com.conduit.msme.ui.theme.ConduitThemeTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CreditSolvencyGauge(
    score: Int,
    maxScore: Int,
    tierLabel: String,
    outOfLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 156.dp
) {
    val extended = ConduitThemeTokens.extendedColors
    val targetFraction = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 1_050, easing = FastOutSlowInEasing),
        label = "credit_gauge_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val cyan = extended.cyanAccent
    val green = extended.successGreenBright
    val gold = extended.karnatakaGold

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 13.dp.toPx()
            val inset = strokeWidth / 2f + 4.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val startAngle = 140f
            val totalSweep = 260f

            // Background arc
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active score gradient arc
            val activeSweep = totalSweep * animatedFraction
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(gold, cyan, green, cyan)
                ),
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glowing terminal knob
            val endAngleRad = ((startAngle + activeSweep) * PI / 180.0)
            val radius = arcSize.width / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val knobPos = Offset(
                x = center.x + (radius * cos(endAngleRad)).toFloat(),
                y = center.y + (radius * sin(endAngleRad)).toFloat()
            )
            drawCircle(
                color = Color.White,
                radius = strokeWidth * 0.62f,
                center = knobPos
            )
            drawCircle(
                color = green,
                radius = strokeWidth * 0.38f,
                center = knobPos
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = outOfLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = extended.successContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = tierLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = extended.successGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun LiveAudioWaveform(
    amplitudes: List<Float>,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val extended = ConduitThemeTokens.extendedColors
    val activeColor = extended.cyanAccent
    val goldAccent = extended.karnatakaGold
    val idleColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        if (amplitudes.isEmpty()) return@Canvas
        val barCount = amplitudes.size
        val spacing = 5.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3.dp.toPx())
        val centerY = size.height / 2f

        amplitudes.forEachIndexed { idx, amp ->
            val clamped = amp.coerceIn(0.12f, 1.0f)
            val barHeight = (size.height * 0.88f * clamped).coerceAtLeast(8.dp.toPx())
            val x = idx * (barWidth + spacing)
            val y = centerY - barHeight / 2f
            val color = when {
                !isActive -> idleColor
                idx % 5 == 0 -> goldAccent
                else -> activeColor
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun InflowStabilityBarChart(
    inflows: List<MonthlyInflow>,
    isKannada: Boolean,
    modifier: Modifier = Modifier
) {
    val extended = ConduitThemeTokens.extendedColors
    val maxVal = (inflows.maxOfOrNull { it.amountThousands } ?: 250f).coerceAtLeast(1f)
    val normalBarColor = extended.cyanAccent
    val peakBarColor = extended.karnatakaGold
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(inflows) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
        ) {
            val topPadding = 12.dp.toPx()
            val bottomPadding = 6.dp.toPx()
            val chartHeight = size.height - topPadding - bottomPadding

            // Subtle horizontal reference lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                val y = topPadding + chartHeight * (1f - fraction)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (inflows.isEmpty()) return@Canvas
            val count = inflows.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.56f).coerceIn(8.dp.toPx(), 20.dp.toPx())

            inflows.forEachIndexed { index, item ->
                val heightFraction = (item.amountThousands / maxVal) * animProgress.value
                val barHeight = (chartHeight * heightFraction).coerceAtLeast(6.dp.toPx())
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                val y = topPadding + (chartHeight - barHeight)

                drawRoundRect(
                    color = if (item.isFestivalPeak) peakBarColor else normalBarColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            inflows.forEach { item ->
                Text(
                    text = if (isKannada) item.monthKannada else item.monthCode,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (item.isFestivalPeak) FontWeight.Bold else FontWeight.Normal,
                    color = if (item.isFestivalPeak) {
                        extended.karnatakaGold
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VerifiableCredentialQrCanvas(
    seedHash: String,
    modifier: Modifier = Modifier,
    size: Dp = 156.dp
) {
    val moduleDark = MaterialTheme.colorScheme.onSurface
    val accentCyan = ConduitThemeTokens.extendedColors.cyanAccent
    val bgWhite = MaterialTheme.colorScheme.surface

    Surface(
        color = bgWhite,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val grid = 17
            val cellSize = this.size.width / grid
            val seed = seedHash.hashCode()

            fun isFinderZone(r: Int, c: Int): Boolean {
                val topLeft = r < 5 && c < 5
                val topRight = r < 5 && c >= grid - 5
                val bottomLeft = r >= grid - 5 && c < 5
                return topLeft || topRight || bottomLeft
            }

            fun drawFinder(startR: Int, startC: Int) {
                val x = startC * cellSize
                val y = startR * cellSize
                drawRoundRect(
                    color = moduleDark,
                    topLeft = Offset(x, y),
                    size = Size(cellSize * 5, cellSize * 5),
                    cornerRadius = CornerRadius(cellSize * 0.6f, cellSize * 0.6f)
                )
                drawRoundRect(
                    color = bgWhite,
                    topLeft = Offset(x + cellSize, y + cellSize),
                    size = Size(cellSize * 3, cellSize * 3),
                    cornerRadius = CornerRadius(cellSize * 0.3f, cellSize * 0.3f)
                )
                drawRoundRect(
                    color = accentCyan,
                    topLeft = Offset(x + cellSize * 1.7f, y + cellSize * 1.7f),
                    size = Size(cellSize * 1.6f, cellSize * 1.6f),
                    cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f)
                )
            }

            drawFinder(0, 0)
            drawFinder(0, grid - 5)
            drawFinder(grid - 5, 0)

            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    if (isFinderZone(r, c)) continue
                    val inCenterBadge = r in 7..9 && c in 7..9
                    if (inCenterBadge) continue
                    val hashMix = ((r * 31 + c * 17) xor seed) and 0x7FFF
                    if (hashMix % 2 == 0 || (r + c) % 5 == 0) {
                        drawRoundRect(
                            color = moduleDark,
                            topLeft = Offset(c * cellSize + 0.8f, r * cellSize + 0.8f),
                            size = Size(cellSize - 1.6f, cellSize - 1.6f),
                            cornerRadius = CornerRadius(cellSize * 0.2f, cellSize * 0.2f)
                        )
                    }
                }
            }

            // Center verified emblem dot
            drawCircle(
                color = accentCyan,
                radius = cellSize * 1.15f,
                center = Offset(this.size.width / 2f, this.size.height / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = cellSize * 0.55f,
                center = Offset(this.size.width / 2f, this.size.height / 2f)
            )
        }
    }
}

@Composable
fun CelebratoryConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val extended = ConduitThemeTokens.extendedColors
    val palette = listOf(
        extended.karnatakaGold,
        extended.cyanAccent,
        extended.successGreenBright,
        Color(0xFFF43F5E),
        Color(0xFF38BDF8)
    )
    val infinite = rememberInfiniteTransition(label = "confetti_fall")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val count = 36
        for (i in 0 until count) {
            val seedX = ((i * 73) % 100) / 100f
            val speed = 0.7f + ((i * 19) % 5) * 0.14f
            val yFraction = ((progress * speed) + (i * 0.09f)) % 1f
            val sway = sin((progress * 6.28f) + i) * 18.dp.toPx()
            val x = (size.width * seedX + sway).coerceIn(0f, size.width)
            val y = size.height * yFraction
            val color = palette[i % palette.size].copy(alpha = (1f - yFraction * 0.5f).coerceIn(0.2f, 0.95f))

            if (i % 2 == 0) {
                drawCircle(
                    color = color,
                    radius = (4.5f + (i % 3) * 1.5f).dp.toPx(),
                    center = Offset(x, y)
                )
            } else {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(8.dp.toPx(), 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
