package foo.schmitt.doorbell.ws.model

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger

/**
 * Server to Device Command.
 */
@Serializable
sealed interface Command {
    val sequence: Int
}

private val seq = AtomicInteger()

fun nextCommandSequence(): Int = seq.incrementAndGet()
