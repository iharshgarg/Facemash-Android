package com.facemash.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.socket.emitter.Emitter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    friendUsername: String,
    currentUsername: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }

    // ✅ DEFINE LISTENER FIRST
    val onNewMessage = remember {
        Emitter.Listener { args ->
            val data = args[0] as org.json.JSONObject

            val sender = data.getString("sender")
            val content = data.getString("content")

            if (sender == friendUsername || sender == currentUsername) {
                messages = messages + ChatMessage(
                    sender = sender,
                    content = content,
                    timestamp = data.optString("timestamp")
                )
            }
        }
    }

    // 🔌 CONNECT SOCKET + LOAD HISTORY
    LaunchedEffect(Unit) {
        ChatSocket.connect()

        messages = withContext(Dispatchers.IO) {
            AuthApi.fetchConversation(friendUsername)
        }

        ChatSocket.socket.on("private message", onNewMessage)
    }

    // 🧹 CLEANUP
    DisposableEffect(Unit) {
        onDispose {
            ChatSocket.socket.off("private message", onNewMessage)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text(friendUsername) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            items(messages) { msg ->
                Text(
                    text = msg.content,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign =
                        if (msg.sender == currentUsername)
                            androidx.compose.ui.text.style.TextAlign.End
                        else
                            androidx.compose.ui.text.style.TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                enabled = input.isNotBlank(),
                onClick = {
                    ChatSocket.socket.emit(
                        "private message",
                        org.json.JSONObject().apply {
                            put("to", friendUsername)
                            put("content", input)
                        }
                    )
                    input = ""
                }
            ) {
                Text("Send")
            }
        }
    }
}