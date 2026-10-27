package foo.schmitt.doorbell.listener

import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.domain.RingNotification
import foo.schmitt.doorbell.ws.WebsocketController
import foo.schmitt.doorbell.ws.model.BellCommand
import foo.schmitt.doorbell.ws.model.nextCommandSequence
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

class WebsocketBroadcastingRingListener(
    private val websocketController: WebsocketController
) : RingListener {
    private val log: Logger = getLogger(this::class.java)

    override suspend fun onRing(notification: RingNotification) {
        val bellCommand = BellCommand(nextCommandSequence(), notification.bellNr)
        websocketController.sendToAll(bellCommand)
            .onLeft { log.error("error while broadcasting ring: {}", it) }
            .onRight { log.debug("broadcasted {} successfully", notification) }
    }
}
