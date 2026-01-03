package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale

// 🎨 OG Facebook bubble colors
private val MyBubbleColor = Color(0xFFDCF0FF)     // light blue
private val FriendBubbleColor = Color(0xFFF0F0F0) // light gray

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

    val listState = rememberLazyListState()
    val context = LocalContext.current

    /* -------------------- LOAD HISTORY -------------------- */
    LaunchedEffect(friendUsername) {
        messages = withContext(Dispatchers.IO) {
            AuthApi.fetchConversation(friendUsername)
        }
        loading = false
    }

    /* -------------------- SOCKET LISTENER (SCOPED) -------------------- */
    val socketListener: (ChatMessage) -> Unit = remember(friendUsername) {
        { msg ->
            // 🔒 STRICT FILTER — ONLY THIS CONVERSATION
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
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    /* -------------------- UI -------------------- */
    Column(modifier = Modifier.fillMaxSize()) {

        Surface(
            color = FacebookBlue,
            shadowElevation = 6.dp
        ) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                .padding(12.dp),
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
                        color = if (isMe) MyBubbleColor else FriendBubbleColor,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = msg.content,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                placeholder = { Text("Message…") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                enabled = input.isNotBlank(),
                onClick = {
                    val text = input.trim()
                    input = ""

                    SocketManager.sendMessage(
                        to = friendUsername,
                        content = text
                    )
                }
            ) {
                Text("Send")
            }
        }
    }
}