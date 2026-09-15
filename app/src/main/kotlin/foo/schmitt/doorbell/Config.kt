package foo.schmitt.doorbell

data class Config(
    val port: Int,
    val apiPassword: String,
    val websocketPassword: String,
    val metricsPassword: String,
    val telegram: TelegramConfig
) {
    companion object {
        fun fromEnv(): Config = Config(
            port = readEnvInt("PORT", 8080),
            apiPassword = readEnvString("API_PASSWORD", "password"),
            websocketPassword = readEnvString("WEBSOCKET_PASSWORD", "websocketpassword"),
            metricsPassword = readEnvString("METRICS_PASSWORD", "metricspassword"),
            telegram = TelegramConfig(
                telegramEnabled = readEnvBoolean("TELEGRAM_ENABLED", false),
                telegramBotToken = readEnvString("TELEGRAM_BOT_TOKEN", "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"),
                telegramChatId = readEnvString("TELEGRAM_CHAT_ID", "-1234561234")
            )
        )

        private fun readEnvString(name: String, default: String): String {
            return System.getenv(name).takeUnless { it.isNullOrEmpty() } ?: default
        }

        private fun readEnvInt(name: String, default: Int): Int {
            return System.getenv(name).takeUnless { it.isNullOrEmpty() }?.toIntOrNull() ?: default
        }

        private fun readEnvBoolean(name: String, default: Boolean): Boolean {
            return System.getenv(name).takeUnless { it.isNullOrEmpty() }?.toBoolean() ?: default
        }
    }
}

data class TelegramConfig(
    val telegramEnabled: Boolean,
    val telegramBotToken: String,
    val telegramChatId: String
)
