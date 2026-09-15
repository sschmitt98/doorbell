package foo.schmitt.doorbell.ws.model

import kotlinx.serialization.Serializable
import foo.schmitt.doorbell.domain.RingNotification as DomainRingNotification

/**
 * Notification that a bell was rung.
 */
@Serializable
data class RingNotification(
    override val sequence: Int,
    val bellNr: Int
) : Notification

fun RingNotification.toDomain(): DomainRingNotification {
    return DomainRingNotification(
        sequence = sequence,
        bellNr = bellNr
    )
}
