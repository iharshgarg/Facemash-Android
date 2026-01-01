package com.facemash.app

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {

    private var socket: Socket? = null

    fun connect() {
        if (socket?.connected() == true) return

        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            forceNew = true
            reconnection = true

            // 🔐 CRITICAL: attach session cookie
            extraHeaders = mapOf(
                "Cookie" to listOf(ApiClient.getCookieHeader() ?: "")
            )
        }

        socket = IO.socket(ApiClient.BASE_URL, opts)
        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    fun sendMessage(to: String, content: String) {
        val payload = JSONObject().apply {
            put("to", to)
            put("content", content)
        }
        socket?.emit("private message", payload)
    }

    fun onMessage(callback: (ChatMessage) -> Unit) {
        socket?.on("private message") { args ->
            if (args.isEmpty()) return@on

            val obj = args[0] as JSONObject
            callback(
                ChatMessage(
                    sender = obj.getString("sender"),
                    content = obj.getString("content"),
                    timestamp = obj.getString("timestamp")
                )
            )
        }
    }

    fun clearListeners() {
        socket?.off("private message")
    }
}