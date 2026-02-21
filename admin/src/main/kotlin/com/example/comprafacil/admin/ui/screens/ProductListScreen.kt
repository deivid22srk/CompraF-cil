package com.example.comprafacil.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.comprafacil.core.data.Product
import com.example.comprafacil.core.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductListViewModel(private val repository: ProductRepository = ProductRepository()) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _products.value = repository.getProducts()
            } catch (e: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun deleteProduct(id: String) {
        repository.deleteProduct(id)
        fetchProducts()
    }
}

@Composable
fun ProductListScreen(viewModel: ProductListViewModel = viewModel()) {
    val products by viewModel.products.collectAsState()
    val loading by viewModel.loading.collectAsState()

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text("Produtos Cadastrados", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(products) { product ->
                ProductAdminItem(product, viewModel)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProductAdminItem(product: Product, viewModel: ProductListViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.image_url,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, color = Color.White)
                Text("R$ ${product.price}", color = Color(0xFFFF9800))
                Text("Estoque: ${product.stock_quantity}", color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = {
                scope.launch {
                    try {
                        viewModel.deleteProduct(product.id!!)
                        Toast.makeText(context, "Excluído", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao excluir", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}
