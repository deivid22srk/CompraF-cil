package com.example.comprafaciladmin

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductSerializationTest {
    @Test
    fun testProductSerialization() {
        val product = Product(
            name = "Admin Test Product",
            description = "Admin Test Description",
            price = 49.99
        )
        val json = Json.encodeToString(product)
        val decoded = Json.decodeFromString<Product>(json)
        assertEquals(product.name, decoded.name)
        assertEquals(product.price, decoded.price, 0.0)
    }
}
