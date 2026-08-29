package com.example.shopeeclone.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Message
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val conversations = mutableStateOf<List<Message>>(emptyList())
    val isLoading = mutableStateOf(true)
    val currentUserId: String? get() = authRepository.currentUserId

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            isLoading.value = true
            conversations.value = chatRepository.getConversations()
            isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onOpenChat: (String, String, String, String) -> Unit, // buyerId, buyerName, sellerId, sellerName
    viewModel: ChatListViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Chats") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (viewModel.isLoading.value) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (viewModel.conversations.value.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No conversations yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(viewModel.conversations.value, key = { it.buyerId + it.sellerId }) { convo ->
                    val displayName = if (convo.sellerId == viewModel.currentUserId) convo.buyerName else convo.sellerName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenChat(convo.buyerId, convo.buyerName, convo.sellerId, convo.sellerName)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName.ifBlank { "Unknown" }, fontWeight = FontWeight.Medium)
                            val preview = when {
                                convo.content.isNotBlank() -> convo.content
                                convo.mediaType == "video" -> "[Video]"
                                convo.mediaType == "image" -> "[Photo]"
                                else -> ""
                            }
                            Text(preview, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
