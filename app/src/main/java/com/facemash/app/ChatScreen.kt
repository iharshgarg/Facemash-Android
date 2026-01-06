package com.facemash.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MyBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 4.dp
)

private val FriendBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 4.dp,
    bottomEnd = 16.dp
)

private val MyBubbleColor = Color(0xFFD2E2F2)
private val FriendBubbleColor = Color(0xFFE8EAED)
private val FacebookBlue = Color(0xFF3B5998)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    friendUsername: String,
    currentUsername: String,
    onBack: () -> Unit
) {

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var firstScrollDone by remember { mutableStateOf(false) }
    var listViewportHeight by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    BackHandler { onBack() }

    /* -------------------- LOAD HISTORY -------------------- */
    LaunchedEffect(friendUsername) {
        messages = withContext(Dispatchers.IO) {
            AuthApi.fetchConversation(friendUsername)
        }
        loading = false
    }

    /* -------------------- SOCKET LISTENER -------------------- */
    val socketListener: (ChatMessage) -> Unit = remember(friendUsername) {
        { msg ->
            if (
                msg.sender == friendUsername ||
                msg.sender == currentUsername
            ) {
                messages = messages + msg
            }
        }
    }

    DisposableEffect(friendUsername) {
        SocketManager.addMessageListener(socketListener)
        onDispose {
            SocketManager.removeMessageListener(socketListener)
        }
    }

    /* -------------------- AUTO SCROLL -------------------- */
    LaunchedEffect(messages.size, listViewportHeight) {
        if (messages.isNotEmpty()) {
            if (!firstScrollDone) {
                listState.scrollToItem(messages.lastIndex)
                firstScrollDone = true
            } else {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    fun sendMessage() {
        val text = input.trim()
        if (text.isBlank()) return

        input = ""
        focusManager.clearFocus()

        SocketManager.sendMessage(
            to = friendUsername,
            content = text
        )
    }

    /* -------------------- UI -------------------- */
    Column(modifier = Modifier.fillMaxSize()) {

        Surface(color = FacebookBlue, shadowElevation = 6.dp) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiClient.BASE_URL}/dp/$friendUsername")
                                .addHeader("Cookie", ApiClient.getCookieHeader() ?: "")
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = friendUsername,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FacebookBlue
                )
            )
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
                .onSizeChanged { listViewportHeight = it.height },
            state = listState
        ) {
            items(messages) { msg ->
                val isMe = msg.sender == currentUsername

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 260.dp),
                        color = if (isMe) MyBubbleColor else FriendBubbleColor,
                        shape = if (isMe) MyBubbleShape else FriendBubbleShape,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = msg.content,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        /* -------------------- INPUT BAR -------------------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = { sendMessage() }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                enabled = input.isNotBlank(),
                onClick = { sendMessage() }
            ) {
                Text("Send")
            }
        }
    }
}