package com.empire.dashboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empire.dashboard.data.SelectedNiche
import com.empire.dashboard.ui.components.*
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireGreen
import com.empire.dashboard.ui.theme.EmpireRed

@Composable
fun NichesScreen(niches: List<SelectedNiche>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionHeader("Niche Opportunity Rankings") }

        if (niches.isEmpty()) {
            item {
                Text(
                    text = "No niche data. Run niche-trend-autopilot.ps1 first.",
                    color = Color.White.copy(0.5f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }

        itemsIndexed(niches) { index, niche ->
            EmpireCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${index + 1}  ${niche.subNiche}",
                            color = if (index == 0) EmpireGold else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = niche.niche,
                            color = Color.White.copy(0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "%.2f".format(niche.score),
                        color = scoreColor(niche.score),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                ScoreBar("Demand",      niche.demand,      color = EmpireGreen)
                ScoreBar("Speed",       niche.speed,       color = EmpireGreen)
                ScoreBar("Brand Fit",   niche.brandFit,    color = EmpireGold)
                ScoreBar("Competition", niche.competition, color = Color(0xFFFFAB40))
                ScoreBar("Legal Risk",  niche.legalRisk,   color = EmpireRed)
            }
        }
    }
}

private fun scoreColor(score: Double) = when {
    score >= 8.0 -> EmpireGreen
    score >= 6.0 -> EmpireGold
    else         -> EmpireRed
}
