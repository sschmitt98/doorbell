package foo.schmitt.doorbell.ws.model

import kotlinx.serialization.Serializable

/**
 * Command to instruct devices that can make a sound to make one.
 */
@Serializable
data class BellCommand(
    override val sequence: Int,
    val soundNr: Int
) : Command
