package foo.schmitt.doorbell.ws.model

/**
 * Command the device to reboot itself.
 */
data class RebootCommand(
    override val sequence: Int
) : Command
