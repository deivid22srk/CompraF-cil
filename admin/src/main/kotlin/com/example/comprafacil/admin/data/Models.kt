package com.example.comprafacil.admin.data

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val price: Double,
    val image_url: String? = null,
    val stock_quantity: Int? = 0,
    val sold_by: String? = null,
    val category_id: String? = null,
    val created_at: String? = null,
    val images: List<ProductImage>? = null
)

@Serializable
data class ProductImage(
    val id: String? = null,
    val product_id: String,
    val image_url: String
)

@Serializable
data class Category(
    val id: String? = null,
    val name: String,
    val created_at: String? = null
)

@Serializable
data class Address(
    val id: String? = null,
    val user_id: String,
    val name: String,
    val receiver_name: String? = null,
    val phone: String,
    val address_line: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val created_at: String? = null
)

@Serializable
data class Order(
    val id: String? = null,
    val user_id: String? = null,
    val customer_name: String? = null,
    val whatsapp: String,
    val location: String,
    val total_price: Double,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val payment_method: String = "dinheiro",
    val status: String? = "pendente",
    val created_at: String? = null
)

@Serializable
data class OrderStatusHistory(
    val id: String? = null,
    val order_id: String,
    val status: String,
    val notes: String? = null,
    val created_at: String? = null
)

@Serializable
data class AppConfig(
    val key: String,
    val value: kotlinx.serialization.json.JsonElement,
    val updated_at: String? = null
)
