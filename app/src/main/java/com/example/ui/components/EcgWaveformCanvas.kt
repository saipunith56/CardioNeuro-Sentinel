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
import androidx.compose.ui.geometry.Offset
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
fun EcgWaveformCanvas(
    ecgPreset: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecgSweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    Box(
        modifier = modifier
            .testTag("ecg_waveform_canvas_container")
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
                    text = "ECG LEAD II • 25mm/s • 10mm/mV",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = ecgPreset,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (ecgPreset == "Normal Sinus") Color(0xFF4ADE80) else Color(0xFFFB7185),
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("ecg_waveform_canvas")
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Draw Grid Lines (Medical ECG Graph paper)
                val gridStep = 16.dp.toPx()
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridStep
                }
                var y = 0f
                while (y < height) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridStep
                }

                // Draw Isoelectric reference line
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.5f
                )

                // Generate Waveform Path
                val path = Path()
                val sweepX = sweepProgress * width

                val steps = 250
                val dx = width / steps

                for (i in 0..steps) {
                    val px = i * dx
                    // Synthesize ECG heartbeat cycle
                    val phase = (px % 120f) / 120f
                    var py = midY

                    when (ecgPreset) {
                        "ST Elevation MI" -> {
                            py += when {
                                phase in 0.15f..0.22f -> -12f * sin((phase - 0.15f) / 0.07f * Math.PI).toFloat() // P wave
                                phase in 0.32f..0.34f -> 8f // Q
                                phase in 0.34f..0.38f -> -55f // R peak
                                phase in 0.38f..0.40f -> 12f // S
                                phase in 0.40f..0.65f -> -28f // Marked ST Elevation!
                                phase in 0.65f..0.80f -> -18f * sin((phase - 0.65f) / 0.15f * Math.PI).toFloat() // T wave
                                else -> 0f
                            }
                        }
                        "Atrial Fibrillation" -> {
                            val fWaves = 3f * sin(px * 0.4f)
                            py += fWaves + when {
                                phase in 0.35f..0.37f -> 6f
                                phase in 0.37f..0.41f -> -48f // R peak with irregular distance
                                phase in 0.41f..0.43f -> 10f
                                phase in 0.50f..0.65f -> -8f * sin((phase - 0.50f) / 0.15f * Math.PI).toFloat()
                                else -> 0f
                            }
                        }
                        "VTach" -> {
                            py += -45f * sin(px * 0.12f)
                        }
                        else -> { // Normal Sinus
                            py += when {
                                phase in 0.15f..0.22f -> -10f * sin((phase - 0.15f) / 0.07f * Math.PI).toFloat() // P
                                phase in 0.32f..0.34f -> 6f // Q
                                phase in 0.34f..0.38f -> -50f // R
                                phase in 0.38f..0.40f -> 10f // S
                                phase in 0.55f..0.72f -> -12f * sin((phase - 0.55f) / 0.17f * Math.PI).toFloat() // T
                                else -> 0f
                            }
                        }
                    }

                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF38BDF8),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Draw Sweep Bar Glow
                drawLine(
                    color = Color(0xFFF43F5E),
                    start = Offset(sweepX, 0f),
                    end = Offset(sweepX, height),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }
    }
}
