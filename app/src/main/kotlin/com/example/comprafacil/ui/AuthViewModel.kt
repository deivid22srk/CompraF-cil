package com.example.comprafacil.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comprafacil.data.SupabaseConfig
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _currentUser = mutableStateOf<UserInfo?>(null)
    val currentUser: State<UserInfo?> = _currentUser

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        _currentUser.value = SupabaseConfig.client.auth.currentUserOrNull()
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                SupabaseConfig.client.auth.signUpWith(Email) {
                    this.email = email
                    password = pass
                }
                _currentUser.value = SupabaseConfig.client.auth.currentUserOrNull()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                SupabaseConfig.client.auth.signInWith(Email) {
                    this.email = email
                    password = pass
                }
                _currentUser.value = SupabaseConfig.client.auth.currentUserOrNull()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            SupabaseConfig.client.auth.signOut()
            _currentUser.value = null
        }
    }
}
