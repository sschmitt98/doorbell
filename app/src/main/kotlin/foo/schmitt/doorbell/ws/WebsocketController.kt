package foo.schmitt.doorbell.ws

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.ws.model.Notification
import foo.schmitt.doorbell.ws.model.RingNotification
import foo.schmitt.doorbell.ws.model.toDomain
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

class WebsocketController(
    val meterRegistry: PrometheusMeterRegistry,
    val ringListener: RingListener
) {
    private val log: Logger = getLogger(this::class.java)

    private var ws: WebSocketServerSession? = null

    init {
        configureMetrics()
    }

    val routes: Route.() -> Unit = {
        webSocket("/ws") {
            removeExistingWsConnectionIfExists()
            ws = this
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

                if (ws == this) {
                    ws = null
                    log.debug("Set ws to null")
                } else {
                    log.warn(
                        "Not removing ws connection, as {} is not current connection",
                        this
                    )
                }
            }
        }
    }

    private fun removeExistingWsConnectionIfExists() {
        ws?.let {
            it.coroutineContext.cancel()
            log.warn("Removed websocket to device as new connection was established.")
            ws = null
            log.debug("set ws to null")
        } ?: run {
            log.debug("No existing connection.")
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

    // TODO return Either<Error, Unit>
    suspend fun RoutingContext.sendTextOrHandleError(text: String) {
        log.debug("ws is {}", ws)
        ws?.let {
            it.send(Frame.Text(text))
            log.debug("sent \"$text\"")
            call.respond(HttpStatusCode.OK)
        } ?: run {
            log.error("No connection for device.")
            call.respond(HttpStatusCode.ServiceUnavailable, "No connection for device.")
        }
    }

    private fun configureMetrics() {
        meterRegistry.gauge("doorbell.websocket.connected", Unit) {
            if (ws != null) {
                1.0
            } else {
                0.0
            }
        }
        // TODO add counter for bell rings (with bellNr tag)
    }
}
