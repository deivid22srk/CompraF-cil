package com.example.comprafacil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.comprafacil.ui.AuthViewModel

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthViewModel.AuthState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (isLogin) "Bem-vindo de volta!" else "Crie sua conta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state is AuthViewModel.AuthState.Loading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        } else {
            Button(
                onClick = {
                    if (isLogin) viewModel.signIn(email, password)
                    else viewModel.signUp(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(if (isLogin) "ENTRAR" else "CADASTRAR", color = MaterialTheme.colorScheme.onSecondary)
            }
        }

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Não tem uma conta? Cadastre-se" else "Já tem uma conta? Entre", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state is AuthViewModel.AuthState.Error) {
            Text((state as AuthViewModel.AuthState.Error).message, color = Color.Red)
        }
    }
}
