package foo.schmitt.doorbell.domain

data class RingNotification(
    val sequence: Int,
    val bellNr: Int
)
