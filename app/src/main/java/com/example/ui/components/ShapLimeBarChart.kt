package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.MultimodalInferenceEngine
import com.example.ai.ShapFeatureImpact
import org.json.JSONArray
import kotlin.math.abs

@Composable
fun ShapLimeBarChart(
    shapJson: String,
    limeJson: String,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = SHAP, 1 = LIME

    val shapItems = remember(shapJson) {
        val list = mutableListOf<ShapFeatureImpact>()
        try {
            val arr = JSONArray(shapJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ShapFeatureImpact(
                        featureName = obj.optString("feature", ""),
                        impactValue = obj.optDouble("impact", 0.0).toFloat(),
                        description = obj.optString("description", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        list
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shap_lime_card")
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPLAINABLE AI (HEURISTIC DEMO)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.width(170.dp),
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("SHAP", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("LIME", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // SHAP View
                Text(
                    text = "Heuristic SHAP Feature Contribution (Demo)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))

                shapItems.forEach { item ->
                    val isPositiveRisk = item.impactValue > 0
                    val barWidthFraction = abs(item.impactValue).coerceIn(0.05f, 1.0f)
                    val barColor = if (isPositiveRisk) Color(0xFFF43F5E) else Color(0xFF4ADE80)

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.featureName,
                                fontSize = 12.sp,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${if (isPositiveRisk) "+" else ""}${"%.2f".format(item.impactValue)}",
                                fontSize = 12.sp,
                                color = barColor,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barWidthFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(barColor)
                            )
                        }
                        if (item.description.isNotEmpty()) {
                            Text(
                                text = item.description,
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                // LIME View
                Text(
                    text = "Heuristic LIME Local Decision Rule Weights (Demo)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF030712))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "1. [HEURISTIC] Systolic BP > 140 mmHg ➔ Weight: +0.31 (Cardiovascular)",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. [HEURISTIC] ECG ST Segment / AFib Pattern ➔ Weight: +0.27 (Electrophysiology)",
                        fontSize = 11.sp,
                        color = Color(0xFFFB7185),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3. [HEURISTIC] NIHSS Score > 4 ➔ Weight: +0.24 (Neurological Deficit)",
                        fontSize = 11.sp,
                        color = Color(0xFFC084FC),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Disclaimer: These SHAP and LIME details are generated heuristically for demonstrative visualization purposes and do not represent actual computed mathematical feature weights from on-device models.",
                fontSize = 9.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
