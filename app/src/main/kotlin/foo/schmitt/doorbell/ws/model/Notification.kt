package foo.schmitt.doorbell.ws.model

import kotlinx.serialization.Serializable

/**
 * Device to Server Notification.
 */
@Serializable
sealed interface Notification {
    val sequence: Int

}
