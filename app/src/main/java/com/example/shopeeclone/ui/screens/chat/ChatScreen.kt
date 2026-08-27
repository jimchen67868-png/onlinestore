package com.example.shopeeclone.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shopeeclone.data.model.Message
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    val messages = mutableStateOf<List<Message>>(emptyList())
    val isSending = mutableStateOf(false)
    val currentUserId: String? get() = authRepository.currentUserId

    private var pollingJob: Job? = null

    fun startPolling(buyerId: String, sellerId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                messages.value = chatRepository.getMessages(buyerId, sellerId)
                delay(3000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    fun send(buyerId: String, buyerName: String, sellerId: String, sellerName: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            isSending.value = true
            chatRepository.sendMessage(buyerId, buyerName, sellerId, sellerName, content)
            messages.value = chatRepository.getMessages(buyerId, sellerId)
            isSending.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    buyerId: String,
    buyerName: String,
    sellerId: String,
    sellerName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val currentUserId = viewModel.currentUserId
    val title = if (currentUserId == sellerId) buyerName else sellerName

    LaunchedEffect(buyerId, sellerId) {
        viewModel.startPolling(buyerId, sellerId)
    }

    LaunchedEffect(viewModel.messages.value.size) {
        if (viewModel.messages.value.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.value.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { "Chat" }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.send(buyerId, buyerName, sellerId, sellerName, input)
                            input = ""
                        },
                        enabled = input.isNotBlank() && !viewModel.isSending.value
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.messages.value, key = { it.id }) { message ->
                val isMine = message.senderId == currentUserId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .widthIn(max = 260.dp)
                    ) {
                        Text(
                            message.content,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
