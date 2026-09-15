@file:DependsOn("io.ktor:ktor-client-core-jvm:3.5.2")
@file:DependsOn("io.ktor:ktor-client-auth-jvm:3.5.2")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.5.2")
@file:DependsOn("io.ktor:ktor-client-websockets-jvm:3.5.2")
@file:DependsOn("com.beust:klaxon:5.6")

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val client = HttpClient(CIO) {
    install(WebSockets)
    install(Auth) {
        basic {
            credentials {
                BasicAuthCredentials(username = "websocket", password = "websocketpassword")
            }
        }
    }
}

suspend fun DefaultWebSocketSession.outputMessages() {
    try {
        for (frame in incoming) {
            try {
                when (frame) {
                    is Frame.Binary -> println("Binary: ${frame.readBytes()}")
                    is Frame.Text -> {
                        val text = frame.readText()
                        println("Text: $text")

                        val parser: Parser = Parser.default()
                        val json: JsonObject = parser.parse(StringBuilder(text)) as JsonObject

                        println("Json: $json")
                    }

                    is Frame.Close -> println("Close: ${frame.readReason()}")
                    is Frame.Ping -> println("Ping: ${frame.readBytes()}")
                    is Frame.Pong -> println("Pong: ${frame.readBytes()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("inner error while receiving: " + e.localizedMessage)
            }
        }
    } catch (e: Exception) {
        println("outer error while receiving: " + e.localizedMessage)
    }
}

suspend fun DefaultWebSocketSession.inputMessages() {
    while (true) {
        val message = readlnOrNull()
        if (!message.isNullOrEmpty()) {
            try {
                send(message)
            } catch (e: Exception) {
                println("error while sending: " + e.localizedMessage)
            }
        } else {
            return
        }
    }
}

runBlocking {
    client.webSocket(method = HttpMethod.Get, host = "localhost", port = 8080, path = "/ws") {
        println("connection established")
        val messageOutputRoutine = launch { outputMessages() }
        val messageInputRoutine = launch { inputMessages() }

        messageInputRoutine.join()
        messageOutputRoutine.cancelAndJoin()
    }
}
client.close()
println("connection closed")