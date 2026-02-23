package com.example.comprafacil.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.comprafacil.ui.components.ManualImageCropper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.comprafacil.core.SupabaseConfig
import com.example.comprafacil.core.data.Profile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit, onOrdersClick: () -> Unit, onAddressesClick: () -> Unit) {
    val client = SupabaseConfig.client
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<Profile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showCropper by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            showCropper = true
        }
    }

    fun uploadBitmap(bitmap: Bitmap) {
        scope.launch {
            uploading = true
            showCropper = false
            try {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val bytes = stream.toByteArray()

                val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                val fileName = "avatars/$userId-${UUID.randomUUID()}.jpg"
                val bucket = client.storage.from("product-images")
                bucket.upload(fileName, bytes)
                val publicUrl = bucket.publicUrl(fileName)

                client.from("profiles").upsert(
                    mapOf("id" to userId, "avatar_url" to publicUrl)
                )

                profile = profile?.copy(avatar_url = publicUrl) ?: Profile(id = userId, avatar_url = publicUrl)
                Toast.makeText(context, "Foto atualizada!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao enviar: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                uploading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val user = client.auth.currentUserOrNull()
        if (user != null) {
            try {
                profile = client.from("profiles").select {
                    filter { eq("id", user.id) }
                }.decodeSingleOrNull<Profile>()
            } catch (e: Exception) {
                // handle error
            }
        }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (profile?.avatar_url != null) {
                    AsyncImage(
                        model = profile?.avatar_url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                }

                if (uploading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }

                Box(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.secondary, CircleShape).padding(4.dp),
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile?.full_name ?: "Usuário",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = {
                    newName = profile?.full_name ?: ""
                    showNameDialog = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Text(client.auth.currentUserOrNull()?.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            ProfileMenuItem(Icons.Default.ShoppingBag, "Meus Pedidos") { onOrdersClick() }
            ProfileMenuItem(Icons.Default.LocationOn, "Endereços") { onAddressesClick() }
            ProfileMenuItem(Icons.Default.Payment, "Formas de Pagamento") {}
            ProfileMenuItem(Icons.Default.Notifications, "Notificações") {}
            ProfileMenuItem(Icons.Default.Help, "Ajuda e Suporte") {}

            Spacer(modifier = Modifier.height(32.dp))

            if (showCropper && selectedUri != null) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showCropper = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false
                    )
                ) {
                    ManualImageCropper(
                        imageUri = selectedUri!!,
                        onCropSuccess = { uploadBitmap(it) },
                        onCancel = { showCropper = false }
                    )
                }
            }

            if (showNameDialog) {
                AlertDialog(
                    onDismissRequest = { showNameDialog = false },
                    title = { Text("Alterar Nome") },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Seu Nome") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            scope.launch {
                                try {
                                    val userId = client.auth.currentUserOrNull()?.id ?: return@launch
                                    client.from("profiles").upsert(
                                        mapOf("id" to userId, "full_name" to newName)
                                    )
                                    profile = profile?.copy(full_name = newName) ?: Profile(id = userId, full_name = newName)
                                    showNameDialog = false
                                    Toast.makeText(context, "Nome atualizado!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao salvar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("SALVAR")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNameDialog = false }) {
                            Text("CANCELAR")
                        }
                    }
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        client.auth.signOut()
                        onLogout()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAIR DA CONTA", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
