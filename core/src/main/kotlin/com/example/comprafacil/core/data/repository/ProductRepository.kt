package com.example.comprafacil.core.data.repository

import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Product
import com.example.comprafacil.core.data.Category
import com.example.comprafacil.core.data.ProductImage
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class ProductRepository {
    private val client = SupabaseConfig.client

    suspend fun getProducts(
        categoryId: String? = null,
        from: Long = 0,
        to: Long = 19
    ): List<Product> {
        return client.from("products").select(Columns.raw("*, images:product_images(*)")) {
            if (categoryId != null) {
                filter { eq("category_id", categoryId) }
            }
            range(from, to)
        }.decodeAs<List<Product>>()
    }

    suspend fun getProductById(id: String): Product? {
        return client.from("products").select(Columns.raw("*, images:product_images(*)")) {
            filter { eq("id", id) }
        }.decodeSingleOrNull<Product>()
    }

    suspend fun getCategories(): List<Category> {
        return client.from("categories").select().decodeAs<List<Category>>()
    }

    suspend fun deleteProduct(id: String) {
        client.from("products").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun insertProduct(product: Product): Product {
        return client.from("products").insert(product) {
            select()
        }.decodeSingle<Product>()
    }

    suspend fun updateProduct(product: Product): Product {
        return client.from("products").update(product) {
            filter { eq("id", product.id!!) }
            select()
        }.decodeSingle<Product>()
    }

    suspend fun deleteProductImages(productId: String) {
        client.from("product_images").delete {
            filter { eq("product_id", productId) }
        }
    }

    suspend fun insertProductImages(images: List<ProductImage>) {
        client.from("product_images").insert(images)
    }
}
