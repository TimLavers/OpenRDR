package io.rippledown.integration.pageobjects

import io.rippledown.integration.pageobjects.POTiming.LOG_THRESHOLD_MS
import io.rippledown.integration.pageobjects.POTiming.time
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Generic per-method timing log for cucumber page-object helpers. Use
 * [time] to wrap a call whose cost we want to observe.
 *
 * Calls that exceed [LOG_THRESHOLD_MS] emit a per-invocation line to
 * stderr:
 *
 *   POTIMING <wallClock> <method> total=<ms> hit=<value>
 *
 * On JVM shutdown a per-method summary is printed (count, total ms,
 * avg ms, max ms) sorted by total cost. Mirrors [ChatPOTiming], but is
 * intentionally a separate object so `CHATPO_TIMING` output in existing
 * logs stays distinguishable while we diagnose other page objects.
 */
internal object POTiming {

    private const val LOG_THRESHOLD_MS = 50L

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
            System.err.println("--- PO timing summary ---")
            stats.entries
                .sortedByDescending { it.value.totalNanos.get() }
                .forEach { (name, s) ->
                    val totalMs = s.totalNanos.get() / 1_000_000.0
                    val maxMs = s.maxNanos.get() / 1_000_000.0
                    val count = s.count.get()
                    val avgMs = if (count > 0) totalMs / count else 0.0
                    System.err.printf(
                        "  %-54s n=%d total=%.1fms avg=%.1fms max=%.1fms%n",
                        name, count, totalMs, avgMs, maxMs
                    )
                }
            System.err.println("-------------------------")
        })
    }

    inline fun <T> time(method: String, block: () -> T): T {
        val t0 = System.nanoTime()
        try {
            val result = block()
            record(method, t0, result)
            return result
        } catch (t: Throwable) {
            record(method, t0, "THREW ${t::class.simpleName}")
            throw t
        }
    }

    fun record(method: String, t0Nanos: Long, hit: Any?) {
        val now = System.nanoTime()
        val totalNanos = now - t0Nanos

        val s = stats.computeIfAbsent(method) { Stats() }
        s.count.incrementAndGet()
        s.totalNanos.addAndGet(totalNanos)
        var prev = s.maxNanos.get()
        while (totalNanos > prev && !s.maxNanos.compareAndSet(prev, totalNanos)) {
            prev = s.maxNanos.get()
        }

        if (totalNanos >= LOG_THRESHOLD_MS * 1_000_000L) {
            val ts = LocalTime.now().format(timeFmt)
            System.err.printf(
                "POTIMING %s %s total=%.1fms hit=%s%n",
                ts, method, totalNanos / 1_000_000.0, hit
            )
        }
    }
}
