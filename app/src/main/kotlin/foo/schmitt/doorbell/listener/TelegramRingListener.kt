package foo.schmitt.doorbell.listener

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import foo.schmitt.doorbell.TelegramConfig
import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.domain.RingNotification
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

class TelegramRingListener(val config: TelegramConfig) : RingListener {
    private val log: Logger = getLogger(this::class.java)

    private val bot = TelegramBot(config.telegramBotToken)

    init {
        if (!config.telegramEnabled) {
            throw IllegalStateException("Telegram disabled")
        }
    }

    override suspend fun onRing(notification: RingNotification) {
        log.debug("sending message...")
        val result = message("Ring at Doorbell ${notification.bellNr} (Seq. ${notification.sequence})")
            .sendReturning(
                to = config.telegramChatId,
                bot
            )
            .await()
        log.debug("...result: {}", result)
    }
}
