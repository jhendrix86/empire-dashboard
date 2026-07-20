package com.empire.dashboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empire.dashboard.data.EmpireApi
import com.empire.dashboard.data.RevenueData
import com.empire.dashboard.ui.components.EmpireCard
import com.empire.dashboard.ui.components.SectionHeader
import com.empire.dashboard.ui.components.StatBadge
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireGreen
import com.empire.dashboard.ui.theme.EmpireRed
import kotlinx.coroutines.launch

@Composable
fun RevenueScreen(apiUrl: String = "http://localhost:8765") {
    val api = remember { EmpireApi(apiUrl) }
    val scope = rememberCoroutineScope()
    var revenue by remember { mutableStateOf<RevenueData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    var formType by remember { mutableStateOf("sale") }
    
    fun reload() {
        scope.launch {
            api.getRevenue().onSuccess { data ->
                revenue = data
                loading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        reload()
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
                text = "REVENUE TRACKING",
                color = EmpireGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = 2.sp
            )
        }
        
        revenue?.let { r ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBadge(
                        label = "Total Revenue",
                        value = "$${String.format("%.0f", r.totalRevenue)}",
                        color = EmpireGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "Refunds",
                        value = "$${String.format("%.0f", r.totalRefunds)}",
                        color = EmpireRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBadge(
                        label = "Sales",
                        value = r.salesCount.toString(),
                        color = EmpireGold,
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "Refund Count",
                        value = r.refundCount.toString(),
                        color = Color.White.copy(0.5f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { formType = "sale"; showForm = !showForm },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (formType == "sale" && showForm) EmpireGreen else Color.White.copy(0.1f)
                        )
                    ) {
                        Text("Record Sale", color = if (formType == "sale" && showForm) Color.Black else EmpireGreen, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { formType = "refund"; showForm = !showForm },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (formType == "refund" && showForm) EmpireRed else Color.White.copy(0.1f)
                        )
                    ) {
                        Text("Record Refund", color = if (formType == "refund" && showForm) Color.Black else EmpireRed, fontSize = 11.sp)
                    }
                }
            }
            
            if (showForm) {
                item {
                    TransactionForm(
                        api = api,
                        type = formType,
                        onDone = {
                            showForm = false
                            reload()
                        }
                    )
                }
            }
            
            item {
                EmpireCard {
                    SectionHeader("Recent Transactions")
                    if (r.history.isEmpty()) {
                        Text("No transactions yet", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            r.history.takeLast(10).reversed().forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(0.05f), MaterialTheme.shapes.small)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = entry.type.uppercase(),
                                            color = if (entry.type == "sale") EmpireGreen else EmpireRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = entry.at.take(19).replace("T", " "),
                                            color = Color.White.copy(0.5f),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "$${String.format("%.2f", entry.amount)}",
                                        color = if (entry.type == "sale") EmpireGreen else EmpireRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } ?: run {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmpireGold)
                }
            }
        }
    }
}

@Composable
private fun TransactionForm(api: EmpireApi, type: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    
    EmpireCard {
        SectionHeader("Record ${type.capitalize()}")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.White.copy(0.15f)
                )
            )
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.White.copy(0.15f)
                )
            )
            TextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.White.copy(0.15f)
                )
            )
            
            if (error.isNotEmpty()) {
                Text(error, color = Color.Red, fontSize = 12.sp)
            }
            
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        error = "Amount must be > 0"
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        val result = if (type == "sale") {
                            api.recordSale(amt, email.ifEmpty { null }, note.ifEmpty { null })
                        } else {
                            api.recordRefund(amt, email.ifEmpty { null }, note.ifEmpty { null })
                        }
                        result.onSuccess { onDone() }
                            .onFailure { error = "Failed: ${it.message}"; saving = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "sale") EmpireGreen else EmpireRed
                )
            ) {
                Text("Record", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }
