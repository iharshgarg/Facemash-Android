package com.facemash.app

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URI

object ChatSocket {

    lateinit var socket: Socket
        private set

    fun connect() {
        if (::socket.isInitialized && socket.connected()) return

        val headers = mutableMapOf<String, List<String>>()

        // 🔑 MANUALLY SEND SESSION COOKIE
        ApiClient.getCookieHeader()?.let { cookie ->
            headers["Cookie"] = listOf(cookie)
        }

        val opts = IO.Options().apply {
            transports = arrayOf("websocket") // 🔑 important
            forceNew = false
            reconnection = true
            extraHeaders = headers
        }

        socket = IO.socket(URI(ApiClient.BASE_URL), opts)

        socket.on(Socket.EVENT_CONNECT) {
            println("✅ Socket connected")
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            println("❌ Socket disconnected")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            println("⚠️ Socket connect error")
        }

        socket.connect()
    }

    fun disconnect() {
        if (::socket.isInitialized) {
            socket.disconnect()
            socket.off()
        }
    }
}