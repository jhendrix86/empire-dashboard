package com.empire.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empire.dashboard.data.EmpireRepository
import com.empire.dashboard.data.EmpireState
import com.empire.dashboard.data.EmpireStatus
import com.empire.dashboard.ui.screens.DashboardScreen
import com.empire.dashboard.ui.screens.NichesScreen
import com.empire.dashboard.ui.screens.RunHistoryScreen
import com.empire.dashboard.ui.theme.EmpireAccent
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireSurface
import com.empire.dashboard.ui.theme.EmpireTheme
import kotlinx.coroutines.flow.catch

@Composable
fun App(serverUrl: String = "http://localhost:8765") {
    val repo = remember { EmpireRepository(com.empire.dashboard.data.EmpireApi(serverUrl)) }
    val state by repo.statusStream(5000L)
        .catch { emit(EmpireState.Error(it.message ?: "Stream error")) }
        .collectAsState(initial = EmpireState.Loading)

    EmpireTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EmpireSurface
        ) {
            when (val s = state) {
                is EmpireState.Loading -> LoadingScreen()
                is EmpireState.Error   -> ErrorScreen(s.message)
                is EmpireState.Success -> EmpireNavHost(s.status)
            }
        }
    }
}

@Composable
private fun EmpireNavHost(status: EmpireStatus) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Niches", "History")

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = EmpireAccent) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label, fontSize = 11.sp) },
                        icon = {},
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = EmpireGold,
                            unselectedTextColor = Color.White.copy(0.5f),
                            indicatorColor = EmpireGold.copy(0.2f)
                        )
                    )
                }
            }
        },
        containerColor = EmpireSurface
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(status)
                1 -> NichesScreen(status.topNiches)
                2 -> RunHistoryScreen(status.recentPipelines, status.recentBundles)
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = EmpireGold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connecting to Empire API…", color = Color.White.copy(0.6f), fontSize = 14.sp)
            Text("localhost:8765", color = Color.White.copy(0.3f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚡", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "API OFFLINE",
                color = Color(0xFFFF5252),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White.copy(0.5f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Start the server:\n.\\empire-api-server.ps1",
                color = Color.White.copy(0.4f),
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
