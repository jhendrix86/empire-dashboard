package com.empire.dashboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empire.dashboard.data.EmpireApi
import com.empire.dashboard.data.RunProgress
import com.empire.dashboard.ui.components.EmpireCard
import com.empire.dashboard.ui.components.SectionHeader
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireGreen
import com.empire.dashboard.ui.theme.EmpireRed
import com.empire.dashboard.ui.theme.EmpireSurface
import kotlinx.coroutines.delay

@Composable
fun ProgressScreen(apiUrl: String = "http://localhost:8765") {
    val api = remember { EmpireApi(apiUrl) }
    var progress by remember { mutableStateOf<RunProgress?>(null) }
    var isPolling by remember { mutableStateOf(true) }
    var logTail by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(isPolling) {
        while (isPolling) {
            api.getRunProgress().onSuccess { p ->
                progress = p
                logTail = (logTail + p.newLogLines).takeLast(15)
                if (p.status == "done" || p.status == "error") {
                    isPolling = false
                }
            }
            delay(1500)
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "LIVE PIPELINE PROGRESS",
                color = EmpireGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )
        }
        
        progress?.let { p ->
            item {
                EmpireCard {
                    SectionHeader("Overall Progress")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${p.progressPct.toInt()}% Complete",
                                color = EmpireGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = p.status.uppercase(),
                                color = when (p.status) {
                                    "running" -> EmpireGreen
                                    "done" -> EmpireGreen
                                    "error" -> EmpireRed
                                    else -> Color.White.copy(0.6f)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = (p.progressPct / 100f).coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = EmpireGold,
                            trackColor = Color.White.copy(0.1f)
                        )
                    }
                }
            }
            
            item {
                EmpireCard {
                    SectionHeader("Steps")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        p.steps.forEach { step ->
                            StepBadge(
                                name = step.name,
                                status = step.status,
                                detail = step.detail
                            )
                        }
                    }
                }
            }
            
            item {
                EmpireCard {
                    SectionHeader("Live Log")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(0.3f), shape = MaterialTheme.shapes.small)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (logTail.isEmpty()) {
                            Text(
                                text = "Awaiting logs...",
                                color = Color.White.copy(0.4f),
                                fontSize = 12.sp
                            )
                        } else {
                            logTail.forEach { line ->
                                Text(
                                    text = line,
                                    color = when {
                                        line.contains("[error]", ignoreCase = true) -> EmpireRed
                                        line.contains("[done]", ignoreCase = true) -> EmpireGreen
                                        line.contains("[warn]", ignoreCase = true) -> Color.Yellow
                                        else -> Color.White.copy(0.7f)
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        } ?: run {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmpireGold, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading progress...", color = Color.White.copy(0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBadge(name: String, status: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = when (status) {
                    "done" -> EmpireGreen.copy(0.2f)
                    "running" -> EmpireGold.copy(0.2f)
                    "error" -> EmpireRed.copy(0.2f)
                    else -> Color.White.copy(0.1f)
                },
                shape = MaterialTheme.shapes.small
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = when (status) {
                "done" -> "✓"
                "running" -> "⟳"
                "error" -> "✗"
                else -> "○"
            },
            color = when (status) {
                "done" -> EmpireGreen
                "running" -> EmpireGold
                "error" -> EmpireRed
                else -> Color.White.copy(0.5f)
            },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Column {
            Text(
                text = name.replace("-", " ").uppercase(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = detail,
                color = Color.White.copy(0.6f),
                fontSize = 11.sp
            )
        }
    }
}
