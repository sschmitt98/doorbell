package foo.schmitt.doorbell

import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.listener.TelegramRingListener
import foo.schmitt.doorbell.ws.WebsocketController
import foo.schmitt.doorbell.ws.WebsocketPrincipal
import foo.schmitt.doorbell.ws.model.Notification
import foo.schmitt.doorbell.ws.model.RingNotification
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.slf4j.MDC
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

class App(
    private val config: Config,
    private val ringListener: RingListener = TelegramRingListener(config.telegram)
) {
    private val log: Logger = getLogger(this::class.java)

    private fun ApplicationRequest.toLogStringWithColors(): String = "${httpMethod.value} - ${path()}"

    private val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

//    private val ringListener: RingListener = LoggingRingListener()

    private val websocketController = WebsocketController(meterRegistry, ringListener)

    val moduleConfiguration: Application.() -> Unit = {
        install(XForwardedHeaders)

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = false
            })
        }

        install(MicrometerMetrics) {
            registry = meterRegistry
        }

        install(Authentication) {
            basic("api-basic") {
                realm = "doorbell api"
                validate { credentials ->
                    if (credentials.name == "api" && credentials.password == config.apiPassword) {
                        ApiPrincipal()
                    } else {
                        null
                    }
                }
            }

            basic("websocket-basic") {
                realm = "doorbell websocket"
                validate { credentials ->
                    if (credentials.name == "websocket" && credentials.password == config.websocketPassword) {
                        WebsocketPrincipal()
                    } else {
                        null
                    }
                }
            }

            basic("metrics-basic") {
                realm = "doorbell metrics"
                validate { credentials ->
                    if (credentials.name == "metrics"
                        && credentials.password == config.metricsPassword
                    ) {
                        MetricsPrincipal()
                    } else {
                        null
                    }
                }
            }
        }

        install(WebSockets) {
            pingPeriod = 5.seconds
            timeout = 10.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        install(CallLogging) {
            level = Level.DEBUG
            disableDefaultColors()
            mdc("startTimestamp") { System.nanoTime().toString() }
            format { call ->
                val startTimestamp = MDC.get("startTimestamp").toLong()
                val endTimestamp = System.nanoTime()
                val delayMillis = (endTimestamp - startTimestamp) / 1_000_000.0

                when (val status = call.response.status() ?: "Unhandled") {
                    HttpStatusCode.Found -> "${status as HttpStatusCode} (${delayMillis}ms): " +
                            "${call.request.toLogStringWithColors()} -> ${call.response.headers[HttpHeaders.Location]}"

                    "Unhandled" -> "$status (${delayMillis}ms): ${call.request.toLogStringWithColors()}"
                    else -> "${status as HttpStatusCode} (${delayMillis}ms): ${call.request.toLogStringWithColors()}"
                }
            }
            filter { call ->
                call.request.path().startsWith("/api")
            }
        }

        routing {
            route("/api/v1") {
                authenticate("api-basic") {
                    get("/hello") {
                        call.respond("Hello, world!")
                    }
                    post("/reboot") {
                        // TODO implement
                    }
                }
            }
            authenticate("websocket-basic") {
                websocketController.routes(this)
            }
            authenticate("metrics-basic") {
                get("/metrics") {
                    call.respond(meterRegistry.scrape())
                }
            }
        }
    }

    fun start() {
        log.info("Starting App...")
        log.debug("Config: {}", config)

        log.debug("example WebSocket Notification: {}", Json.encodeToString<Notification>(RingNotification(42, 1)))

        embeddedServer(Netty, configure = {
            connector {
                port = config.port
            }
            enableHttp2 = true
            enableH2c = true
        }) {
            moduleConfiguration()
        }.start(wait = true)
    }
}

fun main() {
    App(Config.fromEnv()).start()
}
