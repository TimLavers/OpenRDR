package io.rippledown.cornerstone

import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.utils.createViewableCase
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [CornerstoneTestHook].
 *
 * The hook is the contract surface between `OpenRDRUI` (which writes
 * the latest [CornerstoneStatus] to it on every recomposition) and the
 * cucumber `CornerstonePO` (which reads it to assert what cornerstone
 * is being shown). Each behaviour locked in here has a corresponding
 * caller in `CornerstonePO` that depends on it.
 */
class CornerstoneTestHookTest {

    @Before
    fun resetHook() {
        // The hook is a singleton; isolate tests from each other.
        CornerstoneTestHook.reset()
    }

    // -------- snapshot defaults --------

    @Test
    fun `EMPTY snapshot has no case name and a sentinel index`() {
        val empty = CornerstoneTestHook.Snapshot.EMPTY

        empty.cornerstoneCaseName shouldBe null
        empty.indexOfCornerstoneToReview shouldBe -1
        empty.numberOfCornerstones shouldBe 0
    }

    @Test
    fun `EMPTY snapshot reports isShowing=false`() {
        // `CornerstonePO.requireNoCornerstoneCases` keys off this flag.
        CornerstoneTestHook.Snapshot.EMPTY.isShowing shouldBe false
    }

    @Test
    fun `snapshot returns EMPTY before any update`() {
        CornerstoneTestHook.snapshot() shouldBe CornerstoneTestHook.Snapshot.EMPTY
    }

    // -------- update() — populated status --------

    @Test
    fun `update with a populated status publishes the case name index and total`() {
        val status = CornerstoneStatus(
            cornerstoneToReview = createViewableCase("Planck", 42),
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )

        CornerstoneTestHook.update(status)

        val s = CornerstoneTestHook.snapshot()
        s.cornerstoneCaseName shouldBe "Planck"
        s.indexOfCornerstoneToReview shouldBe 0
        s.numberOfCornerstones shouldBe 3
    }

    @Test
    fun `update with a populated status reports isShowing=true`() {
        CornerstoneTestHook.update(
            CornerstoneStatus(
                cornerstoneToReview = createViewableCase("Einstein", 1),
                indexOfCornerstoneToReview = 1,
                numberOfCornerstones = 2
            )
        )

        CornerstoneTestHook.snapshot().isShowing shouldBe true
    }

    @Test
    fun `update with a different status overwrites all snapshot fields`() {
        CornerstoneTestHook.update(
            CornerstoneStatus(
                cornerstoneToReview = createViewableCase("Planck", 42),
                indexOfCornerstoneToReview = 0,
                numberOfCornerstones = 3
            )
        )
        CornerstoneTestHook.update(
            CornerstoneStatus(
                cornerstoneToReview = createViewableCase("Einstein", 1),
                indexOfCornerstoneToReview = 2,
                numberOfCornerstones = 5
            )
        )

        val s = CornerstoneTestHook.snapshot()
        s.cornerstoneCaseName shouldBe "Einstein"
        s.indexOfCornerstoneToReview shouldBe 2
        s.numberOfCornerstones shouldBe 5
    }

    // -------- update() — null / empty status (rule session ended) --------

    @Test
    fun `update with null status resets to EMPTY`() {
        CornerstoneTestHook.update(
            CornerstoneStatus(
                cornerstoneToReview = createViewableCase("Planck", 42),
                indexOfCornerstoneToReview = 0,
                numberOfCornerstones = 3
            )
        )

        CornerstoneTestHook.update(null)

        // Null status models "no rule session in progress" — the cuke
        // `requireNoCornerstoneCases` check must observe isShowing=false.
        CornerstoneTestHook.snapshot() shouldBe CornerstoneTestHook.Snapshot.EMPTY
    }

    @Test
    fun `update with a status that has no cornerstoneToReview resets to EMPTY`() {
        CornerstoneTestHook.update(
            CornerstoneStatus(
                cornerstoneToReview = createViewableCase("Planck", 42),
                indexOfCornerstoneToReview = 0,
                numberOfCornerstones = 3
            )
        )

        // Rule session ended: server pushes a status with no case to
        // review (and zero remaining cornerstones).
        CornerstoneTestHook.update(
            CornerstoneStatus(
                cornerstoneToReview = null,
                indexOfCornerstoneToReview = -1,
                numberOfCornerstones = 0
            )
        )

        CornerstoneTestHook.snapshot() shouldBe CornerstoneTestHook.Snapshot.EMPTY
    }

    // -------- reset --------

    @Test
    fun `reset returns to EMPTY regardless of prior state`() {
        CornerstoneTestHook.update(
            CornerstoneStatus(
                cornerstoneToReview = createViewableCase("Planck", 42),
                indexOfCornerstoneToReview = 0,
                numberOfCornerstones = 3
            )
        )

        CornerstoneTestHook.reset()

        CornerstoneTestHook.snapshot() shouldBe CornerstoneTestHook.Snapshot.EMPTY
    }

    // -------- thread safety --------

    @Test
    fun `concurrent update and snapshot never produce a torn snapshot`() {
        val threads = 8
        val iterationsPerThread = 5_000
        val start = CountDownLatch(1)
        val finished = CountDownLatch(threads)
        val mismatches = AtomicInteger(0)

        val populated = CornerstoneStatus(
            cornerstoneToReview = createViewableCase("Planck", 42),
            indexOfCornerstoneToReview = 0,
            numberOfCornerstones = 3
        )

        val workers = (0 until threads).map { t ->
            Thread {
                start.await()
                try {
                    repeat(iterationsPerThread) { i ->
                        when ((t + i) % 3) {
                            0 -> CornerstoneTestHook.update(populated)
                            1 -> CornerstoneTestHook.update(null)
                            2 -> {
                                val s = CornerstoneTestHook.snapshot()
                                // Invariant: a non-null case name <=>
                                // isShowing==true. Any other combination
                                // means we observed a torn snapshot.
                                val expected = s.cornerstoneCaseName != null
                                if (s.isShowing != expected) {
                                    mismatches.incrementAndGet()
                                }
                                // EMPTY-or-Planck only: no other case
                                // name is ever published in this test,
                                // so any other value indicates torn data.
                                val name = s.cornerstoneCaseName
                                if (name != null && name != "Planck") {
                                    mismatches.incrementAndGet()
                                }
                            }
                        }
                    }
                } finally {
                    finished.countDown()
                }
            }.also { it.isDaemon = true; it.start() }
        }

        start.countDown()
        finished.await(30, TimeUnit.SECONDS) shouldBe true
        workers.forEach { it.join(1_000) }

        mismatches.get() shouldBe 0
    }
}
