package com.example.comprafacil.admin.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Category
import com.example.comprafacil.core.data.Product
import com.example.comprafacil.core.data.ProductImage
import com.example.comprafacil.core.data.repository.ProductRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class AddProductViewModel(private val repository: ProductRepository = ProductRepository()) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

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

    fun addProduct(
        name: String,
        description: String,
        price: Double,
        stockQuantity: Int,
        soldBy: String?,
        category: Category?,
        images: List<ByteArray>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uploading.value = true
            try {
                val imageUrls = mutableListOf<String>()
                for (bytes in images) {
                    val fileName = "products/${UUID.randomUUID()}.jpg"
                    val bucket = SupabaseConfig.client.storage.from("product-images")
                    bucket.upload(fileName, bytes)
                    imageUrls.add(bucket.publicUrl(fileName))
                }

                val productData = Product(
                    name = name,
                    description = description,
                    price = price,
                    stock_quantity = stockQuantity,
                    sold_by = soldBy,
                    category_id = category?.id,
                    image_url = imageUrls.firstOrNull() ?: ""
                )

                val insertedProduct = repository.insertProduct(productData)
                val productId = insertedProduct.id!!

                if (imageUrls.isNotEmpty()) {
                    val imagesToInsert = imageUrls.map { url ->
                        ProductImage(product_id = productId, image_url = url)
                    }
                    SupabaseConfig.client.from("product_images").insert(imagesToInsert)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erro ao adicionar produto")
            } finally {
                _uploading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(viewModel: AddProductViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var soldBy by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val uploading by viewModel.uploading.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        selectedImages = uris
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Adicionar Produto", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Produto") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Preço (ex: 29.90)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = stockQuantity, onValueChange = { stockQuantity = it }, label = { Text("Estoque Disponível") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = soldBy, onValueChange = { soldBy = it }, label = { Text("Vendido por (Nome da Loja/Vendedor)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "Selecione uma Categoria",
                onValueChange = {},
                readOnly = true,
                label = { Text("Categoria") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Selecionar Imagens (${selectedImages.size})")
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(selectedImages) { uri ->
                Text(uri.toString(), fontSize = 10.sp, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uploading) {
            CircularProgressIndicator(color = Color(0xFFFF9800))
        } else {
            Button(
                onClick = {
                    if (name.isBlank() || price.isBlank()) {
                        Toast.makeText(context, "Nome e preço são obrigatórios", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val imageBytesList = selectedImages.mapNotNull { uri ->
                        context.contentResolver.openInputStream(uri)?.readBytes()
                    }

                    viewModel.addProduct(
                        name = name,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        stockQuantity = stockQuantity.toIntOrNull() ?: 0,
                        soldBy = soldBy.ifBlank { null },
                        category = selectedCategory,
                        images = imageBytesList,
                        onSuccess = {
                            Toast.makeText(context, "Produto adicionado!", Toast.LENGTH_LONG).show()
                            name = ""; description = ""; price = ""; stockQuantity = ""; soldBy = ""; selectedCategory = null; selectedImages = emptyList()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Erro: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CADASTRAR PRODUTO", fontWeight = FontWeight.Bold)
            }
        }
    }
}
