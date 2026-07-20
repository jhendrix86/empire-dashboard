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
import com.empire.dashboard.data.Customer
import com.empire.dashboard.data.EmpireApi
import com.empire.dashboard.ui.components.EmpireCard
import com.empire.dashboard.ui.components.SectionHeader
import com.empire.dashboard.ui.theme.EmpireGold
import com.empire.dashboard.ui.theme.EmpireGreen
import com.empire.dashboard.ui.theme.EmpireSurface
import kotlinx.coroutines.launch

@Composable
fun CustomersScreen(apiUrl: String = "http://localhost:8765") {
    val api = remember { EmpireApi(apiUrl) }
    val scope = rememberCoroutineScope()
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        api.getCustomers().onSuccess { response ->
            customers = response.customers
            loading = false
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CUSTOMERS",
                        color = EmpireGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${customers.size} customers",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(containerColor = EmpireGold)
                ) {
                    Text(if (showForm) "Hide" else "Add", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        if (showForm) {
            item {
                AddCustomerForm(api = api) {
                    showForm = false
                    scope.launch {
                        api.getCustomers().onSuccess { response ->
                            customers = response.customers
                        }
                    }
                }
            }
        }
        
        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmpireGold)
                }
            }
        } else if (customers.isEmpty()) {
            item {
                EmpireCard {
                    Text("No customers yet", color = Color.White.copy(0.6f), fontSize = 14.sp)
                }
            }
        } else {
            items(customers) { customer ->
                EmpireCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = customer.name.ifEmpty { "Anonymous" },
                            color = EmpireGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = customer.email,
                            color = Color.White.copy(0.7f),
                            fontSize = 11.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Amount: \$${String.format("%.2f", customer.amountPaid)}",
                                color = EmpireGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Source: ${customer.source}",
                                color = Color.White.copy(0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomerForm(api: EmpireApi, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    
    EmpireCard {
        SectionHeader("Add Customer")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.White.copy(0.15f)
                )
            )
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.White.copy(0.15f)
                )
            )
            TextField(
                value = product,
                onValueChange = { product = it },
                label = { Text("Product") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.White.copy(0.15f)
                )
            )
            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount Paid") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White.copy(0.1f),
                    focusedContainerColor = Color.White.copy(0.15f)
                )
            )
            TextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Source") },
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
                    if (email.isEmpty()) {
                        error = "Email is required"
                        return@Button
                    }
                    saving = true
                    scope.launch {
                        api.addCustomer(
                            email = email,
                            name = name.ifEmpty { null },
                            product = product.ifEmpty { null },
                            amountPaid = amount.toDoubleOrNull(),
                            source = source.ifEmpty { null }
                        ).onSuccess {
                            onSuccess()
                        }.onFailure {
                            error = "Failed: ${it.message}"
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                colors = ButtonDefaults.buttonColors(containerColor = EmpireGold)
            ) {
                Text(if (saving) "Saving..." else "Add Customer", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
