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
data class Order(
    val id: String? = null,
    val user_id: String? = null,
    val whatsapp: String,
    val location: String,
    val total_price: Double,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String? = "pendente",
    val created_at: String? = null
)
