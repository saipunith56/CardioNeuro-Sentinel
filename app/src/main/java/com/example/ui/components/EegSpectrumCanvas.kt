package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun EegSpectrumCanvas(
    eegPreset: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eegPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val (deltaPower, thetaPower, alphaPower, betaPower) = when (eegPreset) {
        "Diffuse Slowing Ischemic" -> Quadruple(0.55f, 0.30f, 0.10f, 0.05f) // High delta/theta slowing
        "Temporal Spike Wave" -> Quadruple(0.20f, 0.25f, 0.25f, 0.30f) // Sharp epileptiform discharges
        else -> Quadruple(0.10f, 0.15f, 0.55f, 0.20f) // Normal posterior dominant 10Hz alpha
    }

    Box(
        modifier = modifier
            .testTag("eeg_spectrum_canvas_container")
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF030712))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EEG POWER SPECTRUM (PSD)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF818CF8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = eegPreset,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (eegPreset == "Normal Alpha Spectrum") Color(0xFF4ADE80) else Color(0xFFC084FC),
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("eeg_spectrum_canvas")
            ) {
                val width = size.width
                val height = size.height

                // Draw Bar Chart for Power Spectral Density
                val bands = listOf(
                    "Delta\n(0.5-4Hz)" to Pair(deltaPower * pulse, Color(0xFF38BDF8)),
                    "Theta\n(4-8Hz)" to Pair(thetaPower * pulse, Color(0xFF818CF8)),
                    "Alpha\n(8-13Hz)" to Pair(alphaPower * pulse, Color(0xFF4ADE80)),
                    "Beta\n(13-30Hz)" to Pair(betaPower * pulse, Color(0xFFF43F5E))
                )

                val barWidth = width / 5f
                val spacing = (width - (barWidth * 4)) / 5f

                bands.forEachIndexed { index, (label, data) ->
                    val (powerRatio, color) = data
                    val barHeight = (height * 0.70f) * powerRatio.coerceIn(0.05f, 0.95f)
                    val x = spacing + index * (barWidth + spacing)
                    val y = (height * 0.75f) - barHeight

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw baseline
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(0f, height * 0.75f),
                        end = Offset(width, height * 0.75f),
                        strokeWidth = 1.5f
                    )
                }

                // Draw Continuous Signal Overlay
                val path = Path()
                val signalY = height * 0.88f
                val steps = 180
                val dx = width / steps

                for (i in 0..steps) {
                    val px = i * dx
                    val py = signalY + (5f * sin(px * 0.15f + (pulse * 2f)))
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }

                drawPath(
                    path = path,
                    color = Color(0xFFC084FC),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
