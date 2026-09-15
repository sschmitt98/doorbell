package foo.schmitt.doorbell.domain

interface RingListener {
    suspend fun onRing(notification: RingNotification)
}