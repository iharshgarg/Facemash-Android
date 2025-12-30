package com.facemash.app

data class ChatMessage(
    val sender: String,
    val content: String,
    val timestamp: String
)

data class ConversationResponse(
    val messages: List<ChatMessage>
)