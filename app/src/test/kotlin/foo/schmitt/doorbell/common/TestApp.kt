package foo.schmitt.doorbell.common

import foo.schmitt.doorbell.App
import foo.schmitt.doorbell.BroadcastConfig
import foo.schmitt.doorbell.Config
import foo.schmitt.doorbell.Metrics
import foo.schmitt.doorbell.TelegramConfig
import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.listener.LoggingRingListener
import foo.schmitt.doorbell.listener.MultiListener
import foo.schmitt.doorbell.listener.WebsocketBroadcastingRingListener
import foo.schmitt.doorbell.ws.WebsocketController
import foo.schmitt.doorbell.ws.model.resetCommandSequence
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

private fun testSetup(ringListener: RingListener, broadcastEnabled: Boolean): App {
    val config = Config(
        port = -1,
        apiPassword = "testingApiPassword",
        websocketPassword = "testingWebsocketPassword",
        metricsPassword = "testingMetricsPassword",
        telegram = TelegramConfig(
            telegramEnabled = false,
            telegramBotToken = "",
            telegramChatId = ""
        ),
        broadcast = BroadcastConfig(
            broadcastEnabled = true,
        )
    )

    val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val metrics = Metrics(meterRegistry)
    val multiListener = MultiListener()
    val websocketController = WebsocketController(meterRegistry, metrics, multiListener)

    multiListener.registerListener(ringListener)

    if (broadcastEnabled) {
        multiListener.registerListener(WebsocketBroadcastingRingListener(websocketController))
    }

    return App(config, websocketController, meterRegistry, multiListener)
}

internal fun withTestApp(
    ringListener: RingListener = LoggingRingListener(),
    broadcastEnabled: Boolean = false,
    block: suspend ApplicationTestBuilder.() -> Unit
) {
    resetCommandSequence()

    val app = testSetup(ringListener, broadcastEnabled)

    testApplication {
        application {
            app.moduleConfiguration(this)
        }
        block()
    }
}

internal fun ApplicationTestBuilder.c(): HttpClient {
    return createClient {
        install(ContentNegotiation)
        install(WebSockets)
    }
}

internal fun HttpRequestBuilder.apiAuth() = basicAuth("api", "testingApiPassword")
internal fun HttpRequestBuilder.websocketAuth() = basicAuth("websocket", "testingWebsocketPassword")
internal fun HttpRequestBuilder.metricsAuth() = basicAuth("metrics", "testingMetricsPassword")
