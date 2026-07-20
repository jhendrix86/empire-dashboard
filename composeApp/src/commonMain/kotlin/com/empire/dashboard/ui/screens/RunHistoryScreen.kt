package com.empire.dashboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empire.dashboard.data.RunEntry
import com.empire.dashboard.ui.components.EmpireCard
import com.empire.dashboard.ui.components.SectionHeader
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireGreen

@Composable
fun RunHistoryScreen(pipelines: List<RunEntry>, bundles: List<RunEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader("Pipeline Run History") }

        if (pipelines.isEmpty()) {
            item {
                Text(
                    text = "No pipeline runs found.",
                    color = Color.White.copy(0.5f),
                    fontSize = 13.sp
                )
            }
        }

        itemsIndexed(pipelines) { index, run ->
            EmpireCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = run.runId,
                        color = if (index == 0) EmpireGold else Color.White.copy(0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                    )
                    if (index == 0) {
                        Text(
                            text = "LATEST",
                            color = EmpireGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text = run.date.take(19).replace("T", "  "),
                    color = Color.White.copy(0.4f),
                    fontSize = 11.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("Product Bundle History")
        }

        if (bundles.isEmpty()) {
            item {
                Text(
                    text = "No bundle runs found.",
                    color = Color.White.copy(0.5f),
                    fontSize = 13.sp
                )
            }
        }

        itemsIndexed(bundles) { index, run ->
            EmpireCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = run.runId,
                        color = if (index == 0) EmpireGold else Color.White.copy(0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    if (index == 0) {
                        Text(
                            text = "LATEST",
                            color = EmpireGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text = run.date.take(19).replace("T", "  "),
                    color = Color.White.copy(0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
