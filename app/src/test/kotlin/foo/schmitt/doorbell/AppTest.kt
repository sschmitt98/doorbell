package foo.schmitt.doorbell

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import assertk.assertions.isTrue
import foo.schmitt.doorbell.common.apiAuth
import foo.schmitt.doorbell.common.c
import foo.schmitt.doorbell.common.metricsAuth
import foo.schmitt.doorbell.common.websocketAuth
import foo.schmitt.doorbell.common.withTestApp
import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.domain.RingNotification
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class AppTest {
    @Test
    fun `when getting health then returns http ok`() = withTestApp {
        // when
        val response = c().get("/health")

        // then
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isEmpty()
    }

    @Test
    fun `when getting metrics then returns http ok`() = withTestApp {
        // when
        val response = c().get("/metrics") {
            metricsAuth()
        }

        // then
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isNotEmpty()
    }

    @Test
    fun `given no authentication when getting metrics then returns http unauthorized`() = withTestApp {
        // when
        val response = c().get("/metrics")

        // then
        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(response.bodyAsText()).isEmpty()
    }

    @Test
    fun `given invalid authentication when getting metrics then returns http unauthorized`() = withTestApp {
        // when
        val response = c().get("/metrics") {
            basicAuth("no.one", "password123")
        }

        // then
        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(response.bodyAsText()).isEmpty()
    }

    @Test
    fun `given no authentication and no upgrade header when connecting to websocket then returns http bad request`() =
        withTestApp {
            // when
            val response = c().get("/ws")

            // then
            assertThat(response.status).isEqualTo(HttpStatusCode.BadRequest)
            assertThat(response.bodyAsText()).isEmpty()
        }

    @Test
    fun `given invalid authentication when connecting to websocket then closes connection`() = withTestApp {
        // when & then
        c().webSocket("/ws", {
            basicAuth("no.body", "secret123")
        }) {
            try {
                incoming.receive()
            } catch (_: ClosedReceiveChannelException) {
            }
            assertThat(incoming.isClosedForReceive).isTrue()
        }
    }

    @Test
    fun `given valid authentication when connecting to websocket then connects`() = withTestApp {
        // given

        // when & then
        c().webSocket("/ws", {
            websocketAuth()
        }) {
            outgoing.send(Frame.Text("Hello World"))
        }
    }

    @Test
    fun `when sending RingNotification then calls RingListener`() {
        val testRingListener = TestRingListener()

        withTestApp(testRingListener) {
            // given

            // when & then
            c().webSocket("/ws", {
                websocketAuth()
            }) {
                assertThat(testRingListener.counter.get()).isEqualTo(0)
                outgoing.send(
                    Frame.Text(
                        """{"type":"foo.schmitt.doorbell.ws.model.RingNotification","sequence":42,"bellNr":1}"""
                    )
                )

                testRingListener.handledEvents.receive() // wait for event handling
                assertThat(testRingListener.counter.get()).isEqualTo(1)
                assertThat(getRingCounter()).isEqualTo(1)

                outgoing.send(
                    Frame.Text(
                        """{"type":"foo.schmitt.doorbell.ws.model.RingNotification","sequence":43,"bellNr":1}"""
                    )
                )

                testRingListener.handledEvents.receive() // wait for event handling
                assertThat(testRingListener.counter.get()).isEqualTo(2)
                assertThat(getRingCounter()).isEqualTo(2)

                assertThat(testRingListener.handledEvents.tryReceive().getOrNull()).isNull()
            }
        }
    }

    class TestRingListener : RingListener {
        val counter = AtomicInteger(0)
        val handledEvents = Channel<String>(UNLIMITED)

        override suspend fun onRing(notification: RingNotification) {
            counter.incrementAndGet()
            handledEvents.send(notification.toString())
        }
    }

    suspend fun ApplicationTestBuilder.getRingCounter(): Int? {
        val response = c().get("/metrics") {
            metricsAuth()
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        return response.bodyAsText()
            .split("\n")
            .find { it.startsWith("doorbell_rings_total") }
            ?.split(" ")
            ?.last()
            ?.toDouble()
            ?.toInt()
    }

    @Test
    fun `when reboot then sends reboot command to websocket`() = withTestApp {
        // when & then
        c().webSocket("/ws", {
            websocketAuth()
        }) {
            val res = c().post("/api/v1/reboot") {
                apiAuth()
            }

            assertThat(res.status).isEqualTo(HttpStatusCode.OK)

            val frame = incoming.receive()
            frame as? Frame.Text ?: throw IllegalStateException("frame is not a text")
            val text = frame.readText()

            assertThat(text).isEqualTo(
                """{"type":"foo.schmitt.doorbell.ws.model.RebootCommand","sequence":1}"""
            )
        }
    }

    @Test
    fun `given invalid auth when reboot then returns http unauthorized`() = withTestApp {
        // when
        val res = c().post("/api/v1/reboot") {
            basicAuth("no.one", "password123")
        }

        // then
        assertThat(res.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(res.bodyAsText()).isEmpty()
    }

    @Test
    fun `given no auth when reboot then returns http unauthorized`() = withTestApp {
        // when
        val res = c().post("/api/v1/reboot") {
            // no auth
        }

        // then
        assertThat(res.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(res.bodyAsText()).isEmpty()
    }
}
