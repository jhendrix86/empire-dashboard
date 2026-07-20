package com.empire.dashboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empire.dashboard.data.EmpireStatus
import com.empire.dashboard.ui.components.*
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireGreen
import com.empire.dashboard.ui.theme.EmpireRed

@Composable
fun DashboardScreen(status: EmpireStatus, onStartPipeline: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "EMPIRE DASHBOARD",
                        color = EmpireGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = status.serverTime.take(19).replace("T", "  "),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PulsingStatusDot(isLive = status.serverTime.isNotEmpty())
                    Text(text = "LIVE", color = EmpireGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // GO Button
        item {
            Button(
                onClick = { onStartPipeline(); onNavigate("Progress") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmpireGold)
            ) {
                Text("🚀 START PIPELINE", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }

        // Quick stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBadge(
                    label = "Last Run",
                    value = status.lastPipelineRun?.takeLast(6) ?: "--",
                    color = EmpireGold
                )
                StatBadge(
                    label = "Launch",
                    value = status.launchStep.replaceFirstChar { it.uppercase() },
                    color = if (status.launchStep == "executed") EmpireGreen else Color.White.copy(0.5f)
                )
                StatBadge(
                    label = "Bundle",
                    value = if (status.bundle?.bundleExists == true) "Ready" else "None",
                    color = if (status.bundle?.bundleExists == true) EmpireGreen else EmpireRed
                )
            }
        }

        // Selected niche
        status.selectedNiche?.let { niche ->
            item {
                EmpireCard {
                    SectionHeader("Active Niche")
                    Text(
                        text = niche.subNiche,
                        color = EmpireGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = niche.niche,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Audience: ${niche.audience}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Problem: ${niche.coreProblem}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ScoreBar(label = "Overall Score", value = niche.score, color = EmpireGold)
                    ScoreBar(label = "Demand",        value = niche.demand,      color = EmpireGreen)
                    ScoreBar(label = "Speed",         value = niche.speed,       color = EmpireGreen)
                    ScoreBar(label = "Brand Fit",     value = niche.brandFit,    color = EmpireGold)
                    ScoreBar(label = "Competition",   value = niche.competition, color = Color(0xFFFFAB40))
                    ScoreBar(label = "Legal Risk",    value = niche.legalRisk,   color = EmpireRed)
                }
            }
        }

        // Bundle info
        status.bundle?.let { bundle ->
            item {
                EmpireCard {
                    SectionHeader("Product Bundle")
                    Text(
                        text = bundle.productName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "v${bundle.version}  •  ${bundle.generatedAt.take(10)}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SHA256: ${bundle.checksumSha256.take(16)}…",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${bundle.copiedFiles.size} files packaged",
                        color = EmpireGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Recent pipeline runs
        if (status.recentPipelines.isNotEmpty()) {
            item {
                EmpireCard {
                    SectionHeader("Recent Pipeline Runs")
                    status.recentPipelines.take(5).forEachIndexed { i, run ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "#${i + 1}  ${run.runId}",
                                color = if (i == 0) EmpireGold else Color.White.copy(0.7f),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                text = run.date.take(10),
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                        if (i < status.recentPipelines.size - 1) {
                            HorizontalDivider(color = Color.White.copy(0.05f))
                        }
                    }
                }
            }
        }
    }
}
