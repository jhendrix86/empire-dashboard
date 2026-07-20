package com.empire.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empire.dashboard.ui.theme.EmpireCard
import com.empire.dashboard.ui.theme.EmpireGold

@Composable
fun EmpireCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EmpireCard),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color = EmpireGold) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = EmpireGold,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun PulsingStatusDot(isLive: Boolean) {
    val color = if (isLive) Color(0xFF00E676) else Color(0xFFFF5252)
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
fun ScoreBar(label: String, value: Double, max: Double = 10.0, color: Color = EmpireGold) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(text = "%.1f".format(value), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (value / max).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
    }
}
