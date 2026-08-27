package com.example.shopeeclone.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.User
import com.example.shopeeclone.data.repository.AuthRepository
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val user = mutableStateOf<User?>(null)
    val isLoading = mutableStateOf(true)
    val isSaving = mutableStateOf(false)
    val statusMessage = mutableStateOf<String?>(null)

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            isLoading.value = true
            user.value = authRepository.getUserProfile()
            isLoading.value = false
        }
    }

    fun save(name: String, phone: String, address: String, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving.value = true
            statusMessage.value = null
            val result = authRepository.updateUserProfile(name, phone, address)
            isSaving.value = false
            result.onSuccess {
                statusMessage.value = "Profile updated!"
                onDone()
            }.onFailure {
                statusMessage.value = "Failed: ${it.message}"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    // Populate the form once the profile finishes loading, without
    // overwriting the user's in-progress edits on recomposition.
    LaunchedEffect(viewModel.user.value) {
        if (!initialized) {
            viewModel.user.value?.let {
                name = it.name
                phone = it.phone
                address = it.address
                initialized = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                OutlinedTextField(
                    value = viewModel.user.value?.email ?: "",
                    onValueChange = {},
                    label = { Text("Email") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shipping Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                viewModel.statusMessage.value?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.save(name, phone, address, onDone = {}) },
                    enabled = name.isNotBlank() && !viewModel.isSaving.value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.isSaving.value) "Saving..." else "Save Changes")
                }
            }
        }
    }
}
