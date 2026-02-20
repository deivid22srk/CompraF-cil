package com.example.comprafacil.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.comprafacil.SupabaseConfig
import com.example.comprafacil.data.Address
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressScreen(onBack: () -> Unit) {
    val client = SupabaseConfig.client
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Address?>(null) }

    fun fetchAddresses() {
        scope.launch {
            try {
                val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                addresses = client.from("addresses").select {
                    filter { eq("user_id", userId) }
                }.decodeAs<List<Address>>()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar endereços", Toast.LENGTH_SHORT).show()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchAddresses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Endereços", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAddress = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (addresses.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nenhum endereço cadastrado", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(addresses) { address ->
                    AddressItem(
                        address = address,
                        onEdit = {
                            editingAddress = address
                            showAddDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    client.from("addresses").delete {
                                        filter { eq("id", address.id!!) }
                                    }
                                    fetchAddresses()
                                    Toast.makeText(context, "Endereço removido", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao remover", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddressDialog(
                address = editingAddress,
                onDismiss = { showAddDialog = false },
                onSave = { name, phone, line, lat, lon ->
                    scope.launch {
                        try {
                            val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                            val newAddress = Address(
                                id = editingAddress?.id,
                                user_id = userId,
                                name = name,
                                phone = phone,
                                address_line = line,
                                latitude = lat,
                                longitude = lon
                            )

                            if (editingAddress == null) {
                                client.from("addresses").insert(newAddress)
                            } else {
                                client.from("addresses").update(newAddress) {
                                    filter { eq("id", editingAddress?.id!!) }
                                }
                            }

                            fetchAddresses()
                            showAddDialog = false
                            Toast.makeText(context, "Endereço salvo!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AddressItem(address: Address, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(address.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(address.address_line, fontSize = 14.sp, color = Color.Gray)
                Text(address.phone, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddressDialog(
    address: Address?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double?, Double?) -> Unit
) {
    var name by remember { mutableStateOf(address?.name ?: "") }
    var phone by remember { mutableStateOf(address?.phone ?: "") }
    var line by remember { mutableStateOf(address?.address_line ?: "") }
    var lat by remember { mutableStateOf(address?.latitude?.toString() ?: "") }
    var lon by remember { mutableStateOf(address?.longitude?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (address == null) "Novo Endereço" else "Editar Endereço") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome (ex: Casa, Trabalho)") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefone/WhatsApp") })
                OutlinedTextField(value = line, onValueChange = { line = it }, label = { Text("Endereço Completo") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text("Lon") }, modifier = Modifier.weight(1f))
                }
                Text("Dica: Use latitude/longitude para localização exata no mapa.", fontSize = 10.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && line.isNotBlank()) {
                    onSave(name, phone, line, lat.toDoubleOrNull(), lon.toDoubleOrNull())
                }
            }) {
                Text("SALVAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}
