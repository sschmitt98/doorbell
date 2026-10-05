package foo.schmitt.doorbell.ws

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import foo.schmitt.doorbell.ErrorPrincipal
import foo.schmitt.doorbell.Metrics
import foo.schmitt.doorbell.Principal
import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.ws.model.Command
import foo.schmitt.doorbell.ws.model.Notification
import foo.schmitt.doorbell.ws.model.RingNotification
import foo.schmitt.doorbell.ws.model.toDomain
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.util.Collections

class WebsocketController(
    val meterRegistry: PrometheusMeterRegistry,
    val metrics: Metrics,
    val ringListener: RingListener
) {
    private val log: Logger = getLogger(this::class.java)

    private val sessions = Collections.synchronizedSet<WebSocketServerSession>(HashSet())

    init {
        configureMetrics()
    }

    val routes: Route.() -> Unit = {
        webSocket("/ws") {
            when (val p = call.principal<Principal>()) {
                is WebsocketPrincipal -> {
                    handleConnection()
                }

                is ErrorPrincipal -> {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "invalid authentication: ${p.error}"))
                }

                else -> {
                    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "internal error"))
                }
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleConnection() {
        sessions.add(this)
        log.debug("set ws to {}", this)

        try {
            for (frame in incoming) {
                log.debug("received {} from device", frame)

                frame as? Frame.Text ?: continue
                val text = frame.readText()

                handleMessage(text)
                    .onLeft { log.error("error while handling websocket: {}", it) }

            }
        } catch (e: Exception) {
            log.warn("Exception during device websocket handling", e)
        } finally {
            log.debug("Closed connection {} of device for reason {}", this, closeReason.await())

            val removed = sessions.remove(this)

            if (removed) {
                log.debug("Removed {} from sessions", this)
            } else {
                log.warn("Could not remove {} as it is not in sessions", this)
            }
        }
    }

    private suspend fun handleMessage(text: String): Either<String, Unit> {
        return deserializeNotification(text)
            .onRight { log.debug("deserialized notification: {}", it) }
            .map { notification ->
                when (notification) {
                    is RingNotification -> {
                        // run in independent coroutine
                        coroutineScope {
                            metrics.incrementRingCounter(notification.bellNr)
                            ringListener.onRing(notification.toDomain())
                        }
                    }
                }
            }
    }

    private fun deserializeNotification(text: String): Either<String, Notification> {
        return try {
            Json.decodeFromString<Notification>(text).right()
        } catch (e: SerializationException) {
            "Error while deserializing: ${e.localizedMessage}".left()
        }
    }

    suspend fun sendToAll(command: Command): Either<String, Unit> {
        val text = Json.encodeToString(command)

        if (sessions.isEmpty()) {
            return "No devices connected".left()
        }

        sessions.forEach { session ->
            try {
                session.send(Frame.Text(text))
            } catch (exception: Exception) {
                log.error("error while sending notification to $session", exception)
            }
        }
        return Unit.right()
    }

    private fun configureMetrics() {
        meterRegistry.gauge("doorbell.websocket.connected", Unit) {
            sessions.size.toDouble()
        }
    }
}
