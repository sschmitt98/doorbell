package foo.schmitt.doorbell.listener

import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.domain.RingNotification
import foo.schmitt.doorbell.ws.WebsocketController
import foo.schmitt.doorbell.ws.model.BellCommand
import foo.schmitt.doorbell.ws.model.nextCommandSequence

class WebsocketBroadcastingRingListener(
    private val websocketController: WebsocketController
) : RingListener {
    override suspend fun onRing(notification: RingNotification) {
        val bellCommand = BellCommand(nextCommandSequence(), notification.bellNr)
        websocketController.sendToAll(bellCommand)
    }
}
