package com.example.shopeeclone.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopeeclone.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            val result = repository.login(email, password)
            isLoading.value = false
            result.onSuccess { onSuccess() }
                .onFailure { errorMessage.value = it.message ?: "Login failed" }
        }
    }

    fun signup(name: String, email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            val result = repository.signUp(name, email, password)
            isLoading.value = false
            result.onSuccess { onSuccess() }
                .onFailure { errorMessage.value = it.message ?: "Sign up failed" }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}
