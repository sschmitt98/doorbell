package foo.schmitt.doorbell.ws.model

import kotlinx.serialization.Serializable

/**
 * Command the device to reboot itself.
 */
@Serializable
data class RebootCommand(
    override val sequence: Int
) : Command
