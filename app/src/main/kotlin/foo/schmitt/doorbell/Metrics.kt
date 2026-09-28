package foo.schmitt.doorbell

import io.ktor.util.collections.ConcurrentMap
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class Metrics(
    private val meterRegistry: PrometheusMeterRegistry
) {
    private val bellRingCounters = ConcurrentMap<Int, Counter>()

    fun incrementRingCounter(bellNr: Int) {
        bellRingCounters
            .getOrPut(bellNr) {
                Counter.builder("doorbell_rings_total")
                    .description("How many times the bell has been rung")
                    .tags(listOf(Tag.of("bellNr", bellNr.toString())))
                    .register(meterRegistry)
            }
            .increment()
    }
}
