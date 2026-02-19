package com.example.comprafacil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.comprafacil.data.Profile
import com.example.comprafacil.data.SupabaseConfig
import com.example.comprafacil.ui.AuthViewModel
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchProfile() {
        scope.launch {
            try {
                val userId = SupabaseConfig.client.auth.currentUserOrNull()?.id ?: return@launch
                profile = SupabaseConfig.client.postgrest["profiles"]
                    .select { filter { eq("id", userId) } }.decodeSingleOrNull<Profile>()

                if (profile == null) {
                    // Create default profile
                    val newProfile = Profile(id = userId, full_name = "Usuário")
                    SupabaseConfig.client.postgrest["profiles"].insert(newProfile)
                    profile = newProfile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchProfile()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = profile?.avatar_url ?: "https://via.placeholder.com/120",
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { /* Logic to pick image and upload to storage */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(profile?.full_name ?: "Usuário", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(SupabaseConfig.client.auth.currentUserOrNull()?.email ?: "", color = Color.Gray)

                Spacer(modifier = Modifier.height(32.dp))

                ProfileMenuItem(icon = Icons.Default.Edit, title = "Editar Perfil") {}
                ProfileMenuItem(icon = Icons.Default.Logout, title = "Sair", color = Color.Red) {
                    authViewModel.signOut()
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, color: Color = Color.Black, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = color)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp).rotate(180f))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
