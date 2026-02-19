package com.example.comprafacil

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long? = null,
    val name: String,
    val created_at: String? = null
)

@Serializable
data class Product(
    val id: Long? = null,
    val name: String,
    val description: String,
    val price: Double,
    val image_url: String? = null,
    val category_id: Long? = null,
    val created_at: String? = null
)
