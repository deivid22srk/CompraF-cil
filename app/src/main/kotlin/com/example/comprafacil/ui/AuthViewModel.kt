package com.example.comprafacil.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comprafacil.SupabaseConfig
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                SupabaseConfig.client.auth.signUpWith(Email) {
                    this.email = email
                    password = pass
                }
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                SupabaseConfig.client.auth.signInWith(Email) {
                    this.email = email
                    password = pass
                }
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            SupabaseConfig.client.auth.signOut()
            _state.value = AuthState.Idle
        }
    }
}
