package com.example.comprafaciladmin

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String? = null,
    val name: String,
    val created_at: String? = null
)

@Serializable
data class Product(
    val id: String? = null,
    val name: String,
    val description: String,
    val price: Double,
    val image_url: String? = null,
    val category_id: String? = null,
    val created_at: String? = null
)
