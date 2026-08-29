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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeartBrainGnnCanvas(
    crosstalkIndex: Float, // 0.0 to 1.0
    crosstalkExplanation: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gnnPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gnnPulse"
    )

    Box(
        modifier = modifier
            .testTag("gnn_canvas_container")
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF030712))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HEART-BRAIN AXIS (HEURISTIC SIMULATION)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Graph Crosstalk Coupling Index: ${(crosstalkIndex * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (crosstalkIndex > 0.65f) Color(0xFFF43F5E) else Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("gnn_graph_canvas")
            ) {
                val width = size.width
                val height = size.height

                // Node Positions
                val heartNode = Offset(width * 0.22f, height * 0.68f) // Cardiac Node
                val carotidNode = Offset(width * 0.50f, height * 0.45f) // Carotid Artery Conduit
                val brainNode = Offset(width * 0.78f, height * 0.25f) // Cerebral Cortex Node

                // Edge 1: Heart -> Carotid
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = heartNode,
                    end = carotidNode,
                    strokeWidth = (2f + (crosstalkIndex * 4f)).dp.toPx()
                )

                // Edge 2: Carotid -> Brain
                drawLine(
                    color = Color(0xFF818CF8),
                    start = carotidNode,
                    end = brainNode,
                    strokeWidth = (2f + (crosstalkIndex * 4f)).dp.toPx()
                )

                // Animated Pulse Packet along Heart -> Brain path
                val packet1X = heartNode.x + (carotidNode.x - heartNode.x) * pulse
                val packet1Y = heartNode.y + (carotidNode.y - heartNode.y) * pulse
                drawCircle(
                    color = Color(0xFFF43F5E),
                    center = Offset(packet1X, packet1Y),
                    radius = 5.dp.toPx()
                )

                val packet2X = carotidNode.x + (brainNode.x - carotidNode.x) * pulse
                val packet2Y = carotidNode.y + (brainNode.y - carotidNode.y) * pulse
                drawCircle(
                    color = Color(0xFFF43F5E),
                    center = Offset(packet2X, packet2Y),
                    radius = 5.dp.toPx()
                )

                // Draw Node Circles
                // Heart Node
                drawCircle(
                    color = Color(0xFFE11D48),
                    center = heartNode,
                    radius = 16.dp.toPx()
                )
                // Carotid Node
                drawCircle(
                    color = Color(0xFF0284C7),
                    center = carotidNode,
                    radius = 14.dp.toPx()
                )
                // Brain Node
                drawCircle(
                    color = Color(0xFF6366F1),
                    center = brainNode,
                    radius = 16.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = crosstalkExplanation,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Notice: GNN crosstalk index is a heuristic clinical rule-based calculation for demo purposes and is not computed from a trained graph neural network.",
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
