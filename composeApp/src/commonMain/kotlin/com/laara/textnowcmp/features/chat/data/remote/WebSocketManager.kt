package com.laara.textnowcmp.features.chat.data.remote

import com.laara.textnowcmp.core.util.TokenProvider
import com.laara.textnowcmp.features.chat.data.remote.dto.WsMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.concurrent.Volatile

class WebSocketManager(
    private val wsBaseUrl: String,
    private val tokenProvider: TokenProvider,
) : KoinComponent {

    private val plainClient: HttpClient by inject(org.koin.core.qualifier.named("plain"))

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: WebSocketSession? = null

    @Volatile
    private var isConnected = false

    private val _incoming = MutableSharedFlow<WsMessage>(extraBufferCapacity = 64)
    val incoming: SharedFlow<WsMessage> = _incoming

    fun connect() {
        if (isConnected) return
        scope.launch {
            connectWithRetry()
        }
    }

    private suspend fun connectWithRetry() {
        var retryDelay = 1000L
        while (true) {
            try {
                val token = tokenProvider.getAccessToken() ?: run {
                    println("[WS] No access token, skipping connection")
                    return
                }

                println("[WS] Connecting to $wsBaseUrl?token=***")
                session = plainClient.webSocketSession("$wsBaseUrl?token=$token")
                isConnected = true
                retryDelay = 1000L
                println("[WS] ✅ Connected")

                listenForMessages()

                // If we reach here, connection was closed
                isConnected = false
                println("[WS] Connection closed, reconnecting in ${retryDelay}ms...")
            } catch (e: Exception) {
                isConnected = false
                println("[WS] ❌ Connection failed: ${e.message}")
            }

            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(30_000L)
        }
    }

    private suspend fun listenForMessages() {
        val currentSession = session ?: return
        for (frame in currentSession.incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                try {
                    val message = json.decodeFromString<WsMessage>(text)
                    println("[WS] ← ${message.type}: ${message.content ?: message.messageId}")
                    _incoming.emit(message)
                } catch (e: Exception) {
                    println("[WS] Parse error: ${e.message}")
                }
            }
        }
    }

    suspend fun send(message: WsMessage) {
        val currentSession = session
        if (currentSession == null || !isConnected) {
            println("[WS] Cannot send — not connected")
            return
        }
        try {
            val text = json.encodeToString(WsMessage.serializer(), message)
            currentSession.send(Frame.Text(text))
            println("[WS] → ${message.type}: ${message.content ?: message.messageId}")
        } catch (e: Exception) {
            println("[WS] Send error: ${e.message}")
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                session?.close()
            } catch (_: Exception) {}
            session = null
            isConnected = false
            println("[WS] Disconnected")
        }
    }
}
