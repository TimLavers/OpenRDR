package io.rippledown.integration.pageobjects

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-invocation timing log for the polling helpers in [ChatPO]. Cucumber
 * step-defs poll these methods at ~10ms intervals via Awaitility, so each
 * invocation is normally cheap. When the accessibility tree is huge (e.g.
 * the Einstein case with ~130 attributes) individual calls can take
 * hundreds of ms, which would explain wall-clock gaps in the cuke run that
 * are NOT due to LLM latency.
 *
 * Every call logs a single line:
 *
 *   CHATPO_TIMING <wallClock> <method> total=<ms> count=<ms> wait=<ms> body=<ms> hit=<bool> terms=<list>
 *
 * - `total` is the wall time of the whole helper (count + wait + body).
 * - `count` is the time spent in `numberOfChatMessages()` if applicable.
 * - `wait`  is the time the test thread spent waiting for the EDT to
 *   pick up the dispatched task (`GuiActionRunner.execute` queue +
 *   any pending compose/layout work blocking the EDT).
 * - `body`  is the time spent inside the EDT lambda actually walking
 *   the accessibility tree.
 * - `hit` is the boolean the helper returned (i.e. did the await succeed
 *   on this poll?).
 *
 * On JVM shutdown a summary per method (count, total ms, max ms) is printed
 * to stderr so the dominant cost can be spotted at a glance.
 */
internal object ChatPOTiming {
    private data class Stats(
        val count: AtomicLong = AtomicLong(),
        val totalNanos: AtomicLong = AtomicLong(),
        val maxNanos: AtomicLong = AtomicLong()
    )

    private val stats = ConcurrentHashMap<String, Stats>()
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            if (stats.isEmpty()) return@Thread
            System.err.println("--- ChatPO timing summary ---")
            stats.entries
                .sortedByDescending { it.value.totalNanos.get() }
                .forEach { (name, s) ->
                    val totalMs = s.totalNanos.get() / 1_000_000.0
                    val maxMs = s.maxNanos.get() / 1_000_000.0
                    val count = s.count.get()
                    val avgMs = if (count > 0) totalMs / count else 0.0
                    System.err.printf(
                        "  %-44s n=%d total=%.1fms avg=%.1fms max=%.1fms%n",
                        name, count, totalMs, avgMs, maxMs
                    )
                }
            System.err.println("-----------------------------")
        })
    }

    fun log(
        method: String,
        t0Nanos: Long,
        tAfterCountNanos: Long,
        tEdtStartNanos: Long,
        terms: List<String>,
        hit: Boolean
    ) {
        val now = System.nanoTime()
        val totalNanos = now - t0Nanos
        val countNanos = (tAfterCountNanos - t0Nanos).coerceAtLeast(0)
        // tEdtStartNanos may be 0 if the EDT lambda never ran (shouldn't
        // happen in practice). Guard so we don't report a wildly negative
        // wait time that would dominate the per-method max.
        val edtKnown = tEdtStartNanos > 0
        val waitNanos = if (edtKnown) (tEdtStartNanos - tAfterCountNanos).coerceAtLeast(0) else 0L
        val bodyNanos =
            if (edtKnown) (now - tEdtStartNanos).coerceAtLeast(0) else (now - tAfterCountNanos).coerceAtLeast(0)

        val s = stats.computeIfAbsent(method) { Stats() }
        s.count.incrementAndGet()
        s.totalNanos.addAndGet(totalNanos)
        // Atomic max
        var prev = s.maxNanos.get()
        while (totalNanos > prev && !s.maxNanos.compareAndSet(prev, totalNanos)) {
            prev = s.maxNanos.get()
        }

        // Per-call line, only when the call was relatively expensive, to
        // avoid flooding the log with sub-millisecond polls.
        if (totalNanos >= 50_000_000L) { // >= 50 ms
            val ts = LocalTime.now().format(timeFmt)
            System.err.printf(
                "CHATPO_TIMING %s %s total=%.1fms count=%.1fms wait=%.1fms body=%.1fms hit=%s terms=%s%n",
                ts, method,
                totalNanos / 1_000_000.0,
                countNanos / 1_000_000.0,
                waitNanos / 1_000_000.0,
                bodyNanos / 1_000_000.0,
                hit, terms
            )
        }
    }
}
