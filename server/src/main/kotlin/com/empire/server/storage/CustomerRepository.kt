package com.empire.server.storage

import com.empire.dashboard.data.Customer
import com.empire.dashboard.data.CustomersResponse
import com.empire.server.config.AppConfig
import java.io.File
import java.time.Instant

class CustomerRepository(dataDir: File = AppConfig.dataDir) {
    private val store = JsonFileStore(
        file = File(dataDir, "customers.json"),
        serializer = CustomersResponse.serializer(),
        default = { CustomersResponse() }
    )

    fun all(): CustomersResponse = store.read()

    fun add(email: String, name: String?, product: String?, amountPaid: Double?, source: String?): Customer {
        val customer = Customer(
            email = email,
            name = name.orEmpty(),
            product = product.orEmpty(),
            amountPaid = amountPaid ?: 0.0,
            source = source.orEmpty(),
            dateAdded = Instant.now().toString()
        )
        store.update { current ->
            val updated = current.customers + customer
            current.copy(count = updated.size, customers = updated)
        }
        return customer
    }
}
