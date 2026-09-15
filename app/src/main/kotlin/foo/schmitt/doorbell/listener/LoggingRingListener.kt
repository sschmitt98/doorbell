package foo.schmitt.doorbell.listener

import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.domain.RingNotification
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

class LoggingRingListener : RingListener {
    private val log: Logger = getLogger(this::class.java)

    override suspend fun onRing(notification: RingNotification) {
        log.debug("onRing")
    }
}
