package foo.schmitt.doorbell.common

import foo.schmitt.doorbell.App
import foo.schmitt.doorbell.Config
import foo.schmitt.doorbell.TelegramConfig
import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.listener.LoggingRingListener
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

private fun testSetup(ringListener: RingListener): App {
    val config = Config(
        port = -1,
        apiPassword = "testingApiPassword",
        websocketPassword = "testingWebsocketPassword",
        metricsPassword = "testingMetricsPassword",
        telegram = TelegramConfig(
            telegramEnabled = false,
            telegramBotToken = "",
            telegramChatId = ""
        )
    )

    return App(config, ringListener)
}

internal fun withTestApp(
    ringListener: RingListener = LoggingRingListener(),
    block: suspend ApplicationTestBuilder.() -> Unit
) {
    val app = testSetup(ringListener)

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
