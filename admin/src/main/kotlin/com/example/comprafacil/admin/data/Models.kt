package com.example.comprafacil.admin.data

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val price: Double,
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
