package foo.schmitt.doorbell.listener

import assertk.assertThat
import assertk.assertions.isEqualTo
import foo.schmitt.doorbell.domain.RingListener
import foo.schmitt.doorbell.domain.RingNotification
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class MultiListenerTest {

    @Test
    fun `when onRing then runs all registered listeners`() {
        // given
        val countingListener1 = CountingListener()
        val countingListener2 = CountingListener()
        val countingListener3 = CountingListener()

        val multiListener = MultiListener()

        multiListener.registerListener(countingListener1)
        multiListener.registerListener(countingListener2)
        multiListener.registerListener(countingListener3)

        val notification = RingNotification(1, 1)

        // when
        runTest {
            multiListener.onRing(notification)
        }

        // then
        assertThat(countingListener1.counter.get()).isEqualTo(1)
        assertThat(countingListener2.counter.get()).isEqualTo(1)
        assertThat(countingListener3.counter.get()).isEqualTo(1)
    }

    @Test
    fun `given throwing RingListener when onRing then still runs other registered listeners`() {
        // given
        val countingListener1 = CountingListener()
        val countingListener2 = CountingListener()
        val countingListener3 = CountingListener()

        val throwingListener = ThrowingListener()

        val multiListener = MultiListener()
        multiListener.registerListener(countingListener1)
        multiListener.registerListener(throwingListener)
        multiListener.registerListener(countingListener2)
        multiListener.registerListener(countingListener3)

        val notification = RingNotification(1, 1)

        // when
        runTest {
            multiListener.onRing(notification)
        }

        // then
        assertThat(countingListener1.counter.get()).isEqualTo(1)
        assertThat(countingListener2.counter.get()).isEqualTo(1)
        assertThat(countingListener3.counter.get()).isEqualTo(1)
    }

    class CountingListener : RingListener {
        val counter: AtomicInteger = AtomicInteger()
        override suspend fun onRing(notification: RingNotification) {
            counter.incrementAndGet()
        }
    }

    class ThrowingListener : RingListener {
        override suspend fun onRing(notification: RingNotification) {
            throw RuntimeException("Exception thrown")
        }
    }
}
