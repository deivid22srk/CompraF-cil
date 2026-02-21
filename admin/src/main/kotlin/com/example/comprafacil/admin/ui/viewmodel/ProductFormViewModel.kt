package com.example.comprafacil.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Category
import com.example.comprafacil.core.data.Product
import com.example.comprafacil.core.data.ProductImage
import com.example.comprafacil.core.data.ProductVariation
import com.example.comprafacil.core.data.repository.ProductRepository
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class ProductFormViewModel(private val repository: ProductRepository = ProductRepository()) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    init {
        fetchCategories()
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            try {
                _categories.value = repository.getCategories()
            } catch (e: Exception) {}
        }
    }

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _product.value = repository.getProductById(productId)
            } catch (e: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }

    fun saveProduct(
        id: String?,
        name: String,
        description: String,
        price: Double,
        stockQuantity: Int,
        soldBy: String?,
        category: Category?,
        variations: List<ProductVariation>,
        newImages: List<ByteArray>,
        existingImageUrls: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val uploadedUrls = mutableListOf<String>()
                for (bytes in newImages) {
                    val fileName = "products/${UUID.randomUUID()}.jpg"
                    val bucket = SupabaseConfig.client.storage.from("product-images")
                    bucket.upload(fileName, bytes)
                    uploadedUrls.add(bucket.publicUrl(fileName))
                }

                val allImageUrls = existingImageUrls + uploadedUrls

                val productData = Product(
                    id = id,
                    name = name,
                    description = description,
                    price = price,
                    stock_quantity = stockQuantity,
                    sold_by = soldBy,
                    category_id = category?.id,
                    image_url = allImageUrls.firstOrNull() ?: "",
                    variations = variations.ifEmpty { null }
                )

                val savedProduct = if (id == null) {
                    repository.insertProduct(productData)
                } else {
                    // When updating, we might want to exclude 'images' field if it's not in the table
                    // but 'variations' is in the table.
                    repository.updateProduct(productData.copy(images = null))
                }

                val productId = savedProduct.id!!

                // Handle image updates in product_images table
                if (id != null) {
                    repository.deleteProductImages(productId)
                }

                if (allImageUrls.isNotEmpty()) {
                    val imagesToInsert = allImageUrls.map { url ->
                        ProductImage(product_id = productId, image_url = url)
                    }
                    repository.insertProductImages(imagesToInsert)
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erro ao salvar produto")
            } finally {
                _loading.value = false
            }
        }
    }
}
