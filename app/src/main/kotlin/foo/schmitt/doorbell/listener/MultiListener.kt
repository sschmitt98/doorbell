package foo.schmitt.doorbell.listener

import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.domain.RingNotification
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

class MultiListener : RingListener {
    private val log: Logger = getLogger(this::class.java)
    private val listeners = mutableListOf<RingListener>()

    override suspend fun onRing(notification: RingNotification) {
        coroutineScope {
            listeners.forEach {
                launch {
                    try {
                        it.onRing(notification)
                    } catch (e: Exception) {
                        log.error("Was not able to run onRing for $notification at $it", e)
                    }
                }
            }
        }
    }

    fun registerListener(listener: RingListener) {
        listeners.add(listener)
    }
}
