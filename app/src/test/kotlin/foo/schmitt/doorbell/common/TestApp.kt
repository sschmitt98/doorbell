package foo.schmitt.doorbell.common

import foo.schmitt.doorbell.App
import foo.schmitt.doorbell.Config
import foo.schmitt.doorbell.TelegramConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

private fun testSetup(): App {
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

    return App(config)
}

internal fun withTestApp(
    block: suspend ApplicationTestBuilder.() -> Unit
) {
    val app = testSetup()

    testApplication {
        application {
            app.moduleConfiguration(this)
        }
        block()
    }
}
