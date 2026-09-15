package foo.schmitt.doorbell.ws.model

import kotlinx.serialization.Serializable

/**
 * Server to Device Command.
 */
@Serializable
sealed interface Command {
    val sequence: Int
}
