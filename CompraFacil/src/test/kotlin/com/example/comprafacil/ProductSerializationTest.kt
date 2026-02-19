package com.example.comprafacil

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductSerializationTest {
    @Test
    fun testProductSerialization() {
        val product = Product(
            name = "Test Product",
            description = "Test Description",
            price = 99.99
        )
        val json = Json.encodeToString(product)
        val decoded = Json.decodeFromString<Product>(json)
        assertEquals(product.name, decoded.name)
        assertEquals(product.price, decoded.price, 0.0)
    }
}
