package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MriScanViewerCanvas(
    mriType: String,
    hasInfarct: Boolean,
    infarctVolumeCc: Float,
    gradCamCoordinatesJson: String,
    isRealDicom: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showGradCamOverlay by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "heatmapPulse")
    val heatmapPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heatmapPulse"
    )

    Box(
        modifier = modifier
            .testTag("mri_scan_viewer_container")
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF020617))
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
                Column {
                    Text(
                        text = "AXIAL BRAIN SLICE ($mriType)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFFE2E8F0),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (infarctVolumeCc < 0.0f) "MRI PARSE ERROR" 
                               else if (hasInfarct) "INFARCT DETECTED • ${"%.1f".format(infarctVolumeCc)} cm³" 
                               else "NO ACUTE ISCHEMIA DETECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (infarctVolumeCc < 0.0f || hasInfarct) Color(0xFFF43F5E) else Color(0xFF4ADE80),
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                if (infarctVolumeCc >= 0.0f) {
                    FilterChip(
                        selected = showGradCamOverlay,
                        onClick = { showGradCamOverlay = !showGradCamOverlay },
                        label = {
                            Text(
                                text = "Heuristic Heatmap",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("gradcam_toggle_chip")
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("mri_scan_canvas")
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f
                val centerY = height / 2f

                // Draw Brain Oval Silhouette (Axial Slice Outer Hemisphere)
                drawOval(
                    color = if (infarctVolumeCc < 0.0f) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFF1E293B),
                    topLeft = Offset(centerX - (width * 0.35f), centerY - (height * 0.42f)),
                    size = androidx.compose.ui.geometry.Size(width * 0.70f, height * 0.84f),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Interhemispheric Fissure
                drawLine(
                    color = if (infarctVolumeCc < 0.0f) Color(0xFF334155).copy(alpha = 0.2f) else Color(0xFF334155),
                    start = Offset(centerX, centerY - (height * 0.40f)),
                    end = Offset(centerX, centerY + (height * 0.40f)),
                    strokeWidth = 2.dp.toPx()
                )

                // Lateral Ventricles (Bilateral brain fluid cavities)
                drawOval(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerX - 24.dp.toPx(), centerY - 30.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 45.dp.toPx())
                )
                drawOval(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerX + 8.dp.toPx(), centerY - 30.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 45.dp.toPx())
                )

                // Draw Infarct Lesion & Grad-CAM Heatmap Overlay
                if (infarctVolumeCc >= 0.0f && hasInfarct && showGradCamOverlay) {
                    val lesionX = centerX - (width * 0.12f)
                    val lesionY = centerY - (height * 0.08f)
                    val radius = (32.dp.toPx()) * heatmapPulse

                    // Grad-CAM Radial Heatmap Gradient (Red to Orange to Transparent Yellow)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFDF43F5E), // Intense Red Core
                                Color(0xDCFB923C), // Penumbra Orange
                                Color(0x80FACC15), // Outer Yellow
                                Color.Transparent
                            ),
                            center = Offset(lesionX, lesionY),
                            radius = radius * 1.4f
                        ),
                        center = Offset(lesionX, lesionY),
                        radius = radius * 1.4f
                    )

                    // Target Bounding Reticle
                    drawCircle(
                        color = Color(0xFFF43F5E),
                        center = Offset(lesionX, lesionY),
                        radius = radius * 0.8f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            if (infarctVolumeCc < 0.0f) {
                Text(
                    text = "❌ MRI Import/Parse Error: The loaded DICOM file could not be parsed.",
                    fontSize = 11.sp,
                    color = Color(0xFFF43F5E),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            } else if (isRealDicom) {
                Text(
                    text = "✓ Image Source: Real imported DICOM scan.",
                    fontSize = 10.sp,
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            } else {
                Text(
                    text = "⚠️ Input Blocked: Brain MRI slice generated from preset (Direct DICOM file parsing is currently unavailable).",
                    fontSize = 10.sp,
                    color = Color(0xFFEA580C),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "Notice: Heatmap coordinates are heuristically projected for visualization and are not a computed Grad-CAM activation map.",
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}
