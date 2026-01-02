package com.facemash.app

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

object SocketManager {

    private var socket: Socket? = null

    // 🔒 single source of truth for listeners
    private val messageListeners = CopyOnWriteArrayList<(ChatMessage) -> Unit>()

    fun connect() {
        if (socket?.connected() == true) return

        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            forceNew = true
            reconnection = true

            // 🔐 attach session cookie (CRITICAL)
            extraHeaders = mapOf(
                "Cookie" to listOf(ApiClient.getCookieHeader() ?: "")
            )
        }

        socket = IO.socket(ApiClient.BASE_URL, opts)

        socket?.on("private message") { args ->
            if (args.isEmpty()) return@on
            val obj = args[0] as JSONObject

            val msg = ChatMessage(
                sender = obj.getString("sender"),
                content = obj.getString("content"),
                timestamp = obj.getString("timestamp")
            )

            messageListeners.forEach { it(msg) }
        }

        socket?.connect()
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        messageListeners.clear()
    }

    fun sendMessage(to: String, content: String) {
        if (content.isBlank()) return

        val payload = JSONObject().apply {
            put("to", to)
            put("content", content)
        }

        socket?.emit("private message", payload)
    }

    fun addMessageListener(listener: (ChatMessage) -> Unit) {
        messageListeners.add(listener)
    }

    fun removeMessageListener(listener: (ChatMessage) -> Unit) {
        messageListeners.remove(listener)
    }
}