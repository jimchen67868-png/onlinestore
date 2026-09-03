package com.example.shopeeclone.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shopeeclone.data.model.Message
import com.example.shopeeclone.data.model.Order
import com.example.shopeeclone.data.repository.AuthRepository
import com.example.shopeeclone.data.repository.ChatRepository
import com.example.shopeeclone.data.repository.MediaRepository
import com.example.shopeeclone.data.repository.OrderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val mediaRepository: MediaRepository = MediaRepository(),
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {
    val messages = mutableStateOf<List<Message>>(emptyList())
    val isSending = mutableStateOf(false)
    val myOrders = mutableStateOf<List<Order>>(emptyList())
    val isLoadingOrders = mutableStateOf(false)
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

    fun sendMedia(
        context: Context,
        uri: Uri,
        buyerId: String,
        buyerName: String,
        sellerId: String,
        sellerName: String
    ) {
        viewModelScope.launch {
            isSending.value = true
            val senderId = authRepository.currentUserId
            if (senderId != null) {
                val kind = mediaRepository.mediaKind(context, uri) ?: "image"
                val uploadResult = mediaRepository.upload(context, uri, senderId)
                uploadResult.onSuccess { url ->
                    chatRepository.sendMessage(
                        buyerId, buyerName, sellerId, sellerName,
                        content = "", mediaUrl = url, mediaType = kind
                    )
                    messages.value = chatRepository.getMessages(buyerId, sellerId)
                }
            }
            isSending.value = false
        }
    }

    fun sendProductCard(
        buyerId: String,
        buyerName: String,
        sellerId: String,
        sellerName: String,
        productId: String,
        productName: String,
        productPrice: Double,
        productImageUrl: String
    ) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                buyerId, buyerName, sellerId, sellerName,
                content = "",
                mediaType = "product",
                productId = productId,
                productName = productName,
                productPrice = productPrice,
                productImageUrl = productImageUrl
            )
            messages.value = chatRepository.getMessages(buyerId, sellerId)
        }
    }

    fun loadMyOrders() {
        viewModelScope.launch {
            isLoadingOrders.value = true
            val userId = authRepository.currentUserId ?: ""
            myOrders.value = orderRepository.getOrdersForUser(userId)
            isLoadingOrders.value = false
        }
    }

    fun sendOrderCard(buyerId: String, buyerName: String, sellerId: String, sellerName: String, order: Order) {
        viewModelScope.launch {
            val firstItemName = order.items.firstOrNull()?.product?.name ?: "Order"
            val extra = if (order.items.size > 1) " + ${order.items.size - 1} more item(s)" else ""
            chatRepository.sendMessage(
                buyerId, buyerName, sellerId, sellerName,
                content = "",
                mediaType = "order",
                orderId = order.id,
                orderTotal = order.totalAmount,
                orderStatus = order.status.label,
                orderSummary = firstItemName + extra
            )
            messages.value = chatRepository.getMessages(buyerId, sellerId)
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
    onProductClick: (String) -> Unit,
    onOrderClick: () -> Unit,
    productId: String = "",
    productName: String = "",
    productPrice: String = "",
    productImageUrl: String = "",
    viewModel: ChatViewModel = viewModel()
) {
    var input by remember { mutableStateOf("") }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showOrderPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val currentUserId = viewModel.currentUserId
    val title = if (currentUserId == sellerId) buyerName else sellerName
    val context = LocalContext.current

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.sendMedia(context, uri, buyerId, buyerName, sellerId, sellerName)
        }
    }

    LaunchedEffect(buyerId, sellerId) {
        viewModel.startPolling(buyerId, sellerId)
    }

    // Auto-send a product preview card when arriving here from a specific product's
    // "Chat" button. Runs once per screen visit.
    LaunchedEffect(Unit) {
        if (productId.isNotBlank()) {
            viewModel.sendProductCard(
                buyerId, buyerName, sellerId, sellerName,
                productId, productName,
                productPrice.toDoubleOrNull() ?: 0.0,
                productImageUrl
            )
        }
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
                    IconButton(
                        onClick = {
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        enabled = !viewModel.isSending.value
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Attach photo or video")
                    }
                    IconButton(
                        onClick = {
                            viewModel.loadMyOrders()
                            showOrderPicker = true
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Attach order")
                    }
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
                    when {
                        message.mediaType == "order" -> {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .clickable { onOrderClick() }
                                    .padding(12.dp)
                            ) {
                                Text("Order #${message.orderId.take(8)}", fontWeight = FontWeight.Bold)
                                Text(message.orderSummary, style = MaterialTheme.typography.bodySmall)
                                Text("Status: ${message.orderStatus}", style = MaterialTheme.typography.bodySmall)
                                message.orderTotal?.let {
                                    Text("Total: $${"%.2f".format(it)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        message.mediaType == "product" -> {
                            Row(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .clickable { onProductClick(message.productId) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (message.productImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = message.productImageUrl,
                                        contentDescription = message.productName,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(message.productName, fontWeight = FontWeight.Medium, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                                    message.productPrice?.let {
                                        Text("$${it}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        message.mediaUrl.isNotBlank() -> {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 220.dp)
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                            ) {
                                when (message.mediaType) {
                                    "video" -> AndroidView(
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        factory = { ctx ->
                                            android.widget.VideoView(ctx).apply {
                                                setVideoURI(Uri.parse(message.mediaUrl))
                                                setMediaController(android.widget.MediaController(ctx).also { it.setAnchorView(this) })
                                            }
                                        }
                                    )
                                    else -> AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = "Attached photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clickable { fullscreenImageUrl = message.mediaUrl }
                                    )
                                }
                            }
                        }
                        else -> {
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
    }

    if (showOrderPicker) {
        AlertDialog(
            onDismissRequest = { showOrderPicker = false },
            title = { Text("Select an order to share") },
            text = {
                if (viewModel.isLoadingOrders.value) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (viewModel.myOrders.value.isEmpty()) {
                    Text("You don't have any orders yet.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                        items(viewModel.myOrders.value, key = { it.id }) { order ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendOrderCard(buyerId, buyerName, sellerId, sellerName, order)
                                        showOrderPicker = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("Order #${order.id.take(8)}", fontWeight = FontWeight.Medium)
                                Text(
                                    "${order.items.size} item(s) · $${"%.2f".format(order.totalAmount)} · ${order.status.label}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrderPicker = false }) { Text("Cancel") }
            }
        )
    }

    fullscreenImageUrl?.let { url ->
        Dialog(
            onDismissRequest = { fullscreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullscreenImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
