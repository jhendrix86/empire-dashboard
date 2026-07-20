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
import com.empire.dashboard.data.EmpireApi
import com.empire.dashboard.data.EmpireRepository
import com.empire.dashboard.data.EmpireState
import com.empire.dashboard.data.EmpireStatus
import com.empire.dashboard.ui.screens.*
import com.empire.dashboard.ui.theme.EmpireAccent
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireSurface
import com.empire.dashboard.ui.theme.EmpireTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun App(serverUrl: String = "http://localhost:8765") {
    val repo = remember { EmpireRepository(com.empire.dashboard.data.EmpireApi(serverUrl)) }
    val state by repo.statusStream(5000L)
        .catch { emit(EmpireState.Error(it.message ?: "Stream error")) }
        .collectAsState(initial = EmpireState.Loading)
    
    val api = remember { EmpireApi(serverUrl) }

    EmpireTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EmpireSurface
        ) {
            when (val s = state) {
                is EmpireState.Loading -> LoadingScreen()
                is EmpireState.Error   -> ErrorScreen(s.message)
                is EmpireState.Success -> EmpireNavHost(s.status, serverUrl = serverUrl, api = api)
            }
        }
    }
}

@Composable
private fun EmpireNavHost(status: EmpireStatus, serverUrl: String, api: EmpireApi) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var navigateTo by remember { mutableStateOf<String?>(null) }
    
    val tabs = listOf("Dashboard", "Progress", "Customers", "Revenue", "Niches", "History")

    LaunchedEffect(navigateTo) {
        navigateTo?.let { dest ->
            selectedTab = tabs.indexOf(dest)
            navigateTo = null
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = EmpireAccent) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label, fontSize = 10.sp) },
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
                0 -> DashboardScreen(
                    status = status,
                    onStartPipeline = {
                        scope.launch {
                            api.startPipeline()
                        }
                    },
                    onNavigate = { navigateTo = it }
                )
                1 -> ProgressScreen(apiUrl = serverUrl)
                2 -> CustomersScreen(apiUrl = serverUrl)
                3 -> RevenueScreen(apiUrl = serverUrl)
                4 -> NichesScreen(status.topNiches)
                5 -> RunHistoryScreen(status.recentPipelines, status.recentBundles)
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
