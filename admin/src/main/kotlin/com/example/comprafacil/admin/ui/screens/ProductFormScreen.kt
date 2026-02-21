package com.example.comprafacil.admin.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comprafacil.admin.ui.viewmodel.ProductFormViewModel
import com.example.comprafacil.core.data.Category
import com.example.comprafacil.core.data.ProductVariation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: String? = null,
    onSuccess: () -> Unit,
    viewModel: ProductFormViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var soldBy by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var variations by remember { mutableStateOf<List<ProductVariation>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val product by viewModel.product.collectAsState()

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
        }
    }

    LaunchedEffect(product) {
        product?.let {
            name = it.name
            description = it.description ?: ""
            price = it.price.toString()
            stockQuantity = (it.stock_quantity ?: 0).toString()
            soldBy = it.sold_by ?: ""
            variations = it.variations ?: emptyList()
            existingImageUrls = it.images?.map { img -> img.image_url } ?: listOfNotNull(it.image_url)
            // Note: category needs careful matching
        }
    }

    // Match category once categories are loaded and product is loaded
    LaunchedEffect(categories, product) {
        if (categories.isNotEmpty() && product != null) {
            selectedCategory = categories.find { it.id == product?.category_id }
        }
    }

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
        Text(
            if (productId == null) "Adicionar Produto" else "Editar Produto",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF9800)
        )
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

        Spacer(modifier = Modifier.height(16.dp))

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
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Variações (Opcional)", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        variations.forEachIndexed { index, variation ->
            VariationItem(
                variation = variation,
                onUpdate = { updated ->
                    variations = variations.toMutableList().apply { set(index, updated) }
                },
                onRemove = {
                    variations = variations.toMutableList().apply { removeAt(index) }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { variations = variations + ProductVariation(name = "", values = emptyList()) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Variação (ex: Cor)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Selecionar Novas Imagens (${selectedImages.size})")
        }

        if (existingImageUrls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Imagens atuais: ${existingImageUrls.size}", fontSize = 12.sp, color = Color.Gray)
            // Option to clear images could be added here
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (loading) {
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

                    viewModel.saveProduct(
                        id = productId,
                        name = name,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        stockQuantity = stockQuantity.toIntOrNull() ?: 0,
                        soldBy = soldBy.ifBlank { null },
                        category = selectedCategory,
                        variations = variations.filter { it.name.isNotBlank() && it.values.isNotEmpty() },
                        newImages = imageBytesList,
                        existingImageUrls = if (selectedImages.isNotEmpty()) emptyList() else existingImageUrls, // Simplified: replace all if new ones selected
                        onSuccess = {
                            Toast.makeText(context, "Produto salvo!", Toast.LENGTH_LONG).show()
                            onSuccess()
                        },
                        onError = { error ->
                            Toast.makeText(context, "Erro: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (productId == null) "CADASTRAR PRODUTO" else "ATUALIZAR PRODUTO", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VariationItem(
    variation: ProductVariation,
    onUpdate: (ProductVariation) -> Unit,
    onRemove: () -> Unit
) {
    var valuesText by remember { mutableStateOf(variation.values.joinToString(", ")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = variation.name,
                    onValueChange = { onUpdate(variation.copy(name = it)) },
                    label = { Text("Nome (ex: Cor)") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Cor, Tamanho, etc") }
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = valuesText,
                onValueChange = {
                    valuesText = it
                    val values = it.split(",").map { v -> v.trim() }.filter { v -> v.isNotBlank() }
                    onUpdate(variation.copy(values = values))
                },
                label = { Text("Valores (separados por vírgula)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Azul, Vermelho, Verde") }
            )
        }
    }
}
