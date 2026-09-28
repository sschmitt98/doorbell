package foo.schmitt.doorbell

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class Metrics(
    meterRegistry: PrometheusMeterRegistry
) {
    val counter: io.micrometer.core.instrument.Counter = io.micrometer.core.instrument.Counter.builder("doorbell_rings_total")
        .description("How many times the bell has been rung")
        .register(meterRegistry)
}
