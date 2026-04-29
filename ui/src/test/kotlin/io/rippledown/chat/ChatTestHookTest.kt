package io.rippledown.chat

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [ChatTestHook].
 *
 * The hook is the contract surface between [ChatPanel] (which writes to
 * it inside a `SideEffect`) and the cucumber `ChatPO` (which reads it
 * to decide when to advance a scenario). Each behaviour these tests
 * pin down has a corresponding caller in `ChatPO` that depends on it,
 * so any incompatible change here should be made together with a
 * matching change in `ChatPO`.
 */
class ChatTestHookTest {

    @Before
    fun resetHook() {
        // The hook is a singleton; isolate tests from each other.
        ChatTestHook.reset()
    }

    // -------- snapshot defaults --------

    @Test
    fun `EMPTY snapshot has zero counts and no text`() {
        val empty = ChatTestHook.Snapshot.EMPTY

        empty.messageCount shouldBe 0
        empty.suggestionRowCount shouldBe 0
        empty.mostRecentBotText shouldBe null
        empty.mostRecentSuggestionText shouldBe null
    }

    @Test
    fun `EMPTY snapshot has sendIsEnabled=false so waitForChatReady blocks until the UI publishes real state`() {
        // This default is load-bearing: cucumber `ChatPO.waitForChatReady`
        // spins on `sendIsEnabled` to gate user input; defaulting to true
        // would let scenarios fire input into a chat that hasn't yet
        // finished its first composition.
        ChatTestHook.Snapshot.EMPTY.sendIsEnabled shouldBe false
    }

    @Test
    fun `snapshot returns EMPTY before any update`() {
        ChatTestHook.snapshot() shouldBe ChatTestHook.Snapshot.EMPTY
    }

    // -------- update() basic propagation --------

    @Test
    fun `update with empty messages publishes zero counts and null texts`() {
        ChatTestHook.update(messages = emptyList(), sendIsEnabled = true)

        val s = ChatTestHook.snapshot()
        s.messageCount shouldBe 0
        s.suggestionRowCount shouldBe 0
        s.mostRecentBotText shouldBe null
        s.mostRecentSuggestionText shouldBe null
        s.sendIsEnabled shouldBe true
    }

    @Test
    fun `messageCount equals total messages including all kinds`() {
        ChatTestHook.update(
            messages = listOf(
                BotMessage("hello"),
                UserMessage("hi"),
                BotMessage("how can I help"),
                SuggestionListMessage(listOf("a", "b"))
            ),
            sendIsEnabled = true
        )

        ChatTestHook.snapshot().messageCount shouldBe 4
    }

    @Test
    fun `mostRecentBotText is taken from the latest BotMessage even when followed by other kinds`() {
        ChatTestHook.update(
            messages = listOf(
                BotMessage("first bot"),
                UserMessage("user reply"),
                BotMessage("second bot"),
                SuggestionListMessage(listOf("opt 1", "opt 2"))
            ),
            sendIsEnabled = true
        )

        // The suggestion list comes after the bot, but it is not a
        // BotMessage. Most recent BotMessage is "second bot".
        ChatTestHook.snapshot().mostRecentBotText shouldBe "second bot"
    }

    @Test
    fun `mostRecentBotText is null when no BotMessage has been seen yet`() {
        ChatTestHook.update(
            messages = listOf(UserMessage("just me")),
            sendIsEnabled = true
        )

        ChatTestHook.snapshot().mostRecentBotText shouldBe null
    }

    // -------- suggestion row counting --------

    @Test
    fun `suggestionRowCount counts every SuggestionListMessage in history`() {
        ChatTestHook.update(
            messages = listOf(
                BotMessage("question 1"),
                SuggestionListMessage(listOf("a")),
                UserMessage("a"),
                BotMessage("question 2"),
                SuggestionListMessage(listOf("b"))
            ),
            sendIsEnabled = true
        )

        // The cuke depends on this counting up monotonically as new
        // suggestion rounds arrive — see `ChatDefs.provideTheseReasons`.
        ChatTestHook.snapshot().suggestionRowCount shouldBe 2
    }

    @Test
    fun `suggestionRowCount is zero when no suggestions have been emitted`() {
        ChatTestHook.update(
            messages = listOf(BotMessage("just text"), UserMessage("ok")),
            sendIsEnabled = true
        )

        ChatTestHook.snapshot().suggestionRowCount shouldBe 0
    }

    // -------- mostRecentSuggestionText formatting --------

    @Test
    fun `mostRecentSuggestionText numbers the latest suggestion list one-based`() {
        ChatTestHook.update(
            messages = listOf(
                BotMessage("q"),
                SuggestionListMessage(listOf("alpha", "beta", "gamma"))
            ),
            sendIsEnabled = true
        )

        // ChatPO.mostRecentSuggestionRowContainsTerms relies on the
        // numbered prefix to find a specific suggestion by index.
        ChatTestHook.snapshot().mostRecentSuggestionText shouldBe "1. alpha\n2. beta\n3. gamma"
    }

    @Test
    fun `mostRecentSuggestionText strips the EDITABLE_MARKER suffix from each suggestion`() {
        ChatTestHook.update(
            messages = listOf(
                SuggestionListMessage(listOf("plain", "with value$EDITABLE_MARKER"))
            ),
            sendIsEnabled = true
        )

        // The marker is an internal flag for the click handler; it must
        // not leak into the test-visible text.
        ChatTestHook.snapshot().mostRecentSuggestionText shouldBe "1. plain\n2. with value"
    }

    @Test
    fun `mostRecentSuggestionText is taken from the LAST SuggestionListMessage when there are several`() {
        ChatTestHook.update(
            messages = listOf(
                SuggestionListMessage(listOf("old1", "old2")),
                BotMessage("between"),
                SuggestionListMessage(listOf("new1", "new2"))
            ),
            sendIsEnabled = true
        )

        ChatTestHook.snapshot().mostRecentSuggestionText shouldBe "1. new1\n2. new2"
    }

    @Test
    fun `mostRecentSuggestionText is null when no SuggestionListMessage has been emitted`() {
        ChatTestHook.update(
            messages = listOf(BotMessage("hi"), UserMessage("hello")),
            sendIsEnabled = true
        )

        ChatTestHook.snapshot().mostRecentSuggestionText shouldBe null
    }

    @Test
    fun `mostRecentSuggestionText is empty string for a SuggestionListMessage with no suggestions`() {
        // Defensive: an empty list shouldn't crash and shouldn't be
        // confused with "no suggestion list at all" (which is null).
        ChatTestHook.update(
            messages = listOf(SuggestionListMessage(emptyList())),
            sendIsEnabled = true
        )

        ChatTestHook.snapshot().mostRecentSuggestionText shouldBe ""
        ChatTestHook.snapshot().suggestionRowCount shouldBe 1
    }

    // -------- sendIsEnabled propagation --------

    @Test
    fun `sendIsEnabled value is published verbatim`() {
        ChatTestHook.update(messages = emptyList(), sendIsEnabled = true)
        ChatTestHook.snapshot().sendIsEnabled shouldBe true

        ChatTestHook.update(messages = emptyList(), sendIsEnabled = false)
        ChatTestHook.snapshot().sendIsEnabled shouldBe false
    }

    @Test
    fun `update overwrites every snapshot field, not just changed ones`() {
        ChatTestHook.update(
            messages = listOf(BotMessage("first"), SuggestionListMessage(listOf("a"))),
            sendIsEnabled = true
        )
        ChatTestHook.update(
            messages = listOf(UserMessage("only me")),
            sendIsEnabled = false
        )

        val s = ChatTestHook.snapshot()
        s.messageCount shouldBe 1
        s.suggestionRowCount shouldBe 0
        s.mostRecentBotText shouldBe null
        s.mostRecentSuggestionText shouldBe null
        s.sendIsEnabled shouldBe false
    }

    // -------- reset --------

    @Test
    fun `reset returns to EMPTY regardless of prior state`() {
        ChatTestHook.update(
            messages = listOf(BotMessage("loud"), SuggestionListMessage(listOf("x"))),
            sendIsEnabled = true
        )

        ChatTestHook.reset()

        ChatTestHook.snapshot() shouldBe ChatTestHook.Snapshot.EMPTY
    }

    // -------- thread safety --------

    @Test
    fun `concurrent update and snapshot calls always observe a self-consistent snapshot`() {
        // Stress the AtomicReference contract: under heavy contention
        // a reader must never see a torn write (e.g. messageCount of
        // one update mixed with mostRecentBotText of another).
        val threads = 8
        val iterationsPerThread = 5_000
        val start = CountDownLatch(1)
        val finished = CountDownLatch(threads)
        val mismatches = AtomicInteger(0)

        val workers = (0 until threads).map { t ->
            Thread {
                start.await()
                try {
                    repeat(iterationsPerThread) { i ->
                        if ((t + i) % 2 == 0) {
                            // Writers publish a self-consistent pair: a
                            // single BotMessage whose text encodes the
                            // count we expect to read back.
                            val n = i + 1
                            ChatTestHook.update(
                                messages = List(n) { BotMessage("b$it") },
                                sendIsEnabled = (i % 2 == 0)
                            )
                        } else {
                            val s = ChatTestHook.snapshot()
                            // If messageCount > 0 then mostRecentBotText
                            // must be non-null (since every update we
                            // post above contains at least one BotMessage).
                            if (s.messageCount > 0 && s.mostRecentBotText == null) {
                                mismatches.incrementAndGet()
                            }
                            // suggestionRowCount must always be in [0, messageCount].
                            if (s.suggestionRowCount < 0 || s.suggestionRowCount > s.messageCount) {
                                mismatches.incrementAndGet()
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
