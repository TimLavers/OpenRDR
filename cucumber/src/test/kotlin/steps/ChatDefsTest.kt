package steps

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.rippledown.constants.chat.AI_UNAVAILABLE_MESSAGE
import io.rippledown.constants.chat.CONFIRM
import io.rippledown.integration.pageobjects.ChatPO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class ChatDefsTest {
    private lateinit var page: ChatPO
    private lateinit var defs: ChatDefs
    private val sentMessages = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        page = mockk()
        mockkStatic(::chatPO)
        every { chatPO() } returns page
        every { page.enterChatText(any()) } answers { sentMessages.add(firstArg()) }
        every { page.clickSend() } just Runs
        defs = spyk(ChatDefs())
        every { defs.waitForBotQuestion() } just Runs
        every { defs.waitForBotSuggestions() } just Runs
        every { defs.waitForBotToSayDone() } just Runs
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::chatPO)
    }

    @Test
    fun `click a knowledge base row through the page object`() {
        // Given
        every { page.clickKbChoice("Zoo Animals") } just Runs

        // When
        defs.clickKbChoice("Zoo Animals")

        // Then
        verify(exactly = 1) { page.clickKbChoice("Zoo Animals") }
        verify(exactly = 0) { page.enterChatText(any()); page.clickSend() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `a remove or replace request waits for its own suggestions before the cornerstone assertion`(replace: Boolean) {
        // Given a pending chat turn; suggestions from an earlier request do not count as its response.
        val sent = CountDownLatch(1)
        val currentSuggestions = AtomicBoolean(false)
        every { page.clickSend() } answers { sent.countDown() }
        every { page.terminalFailureText() } returns null
        every { page.suggestionsAreForLatestRequest() } answers { currentSuggestions.get() }
        every { page.mostRecentSuggestionRowContainsTerms(listOf("1.")) } returns true
        every { defs.waitForBotSuggestions() } answers { callOriginal() }
        val executor = Executors.newSingleThreadExecutor()
        val request = executor.submit { requestCommentChange(replace) }
        try {
            // When the server has accepted the message but has not answered yet.
            sent.await(5, SECONDS) shouldBe true

            // Then the step cannot return and start the short UI assertion while the chat is pending.
            shouldThrow<TimeoutException> { request.get(250, MILLISECONDS) }

            // When suggestions for this request arrive, the step can finish without sending extra messages.
            currentSuggestions.set(true)
            request.get(5, SECONDS)
            sentMessages shouldBe listOf(if (replace) "Replace the comment by \"Comment 2.\"" else "Remove the comment")
        } finally {
            currentSuggestions.set(true)
            request.cancel(true)
            executor.shutdownNow()
            executor.awaitTermination(5, SECONDS)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `a failed remove or replace request reports the chat failure instead of waiting for a cornerstone`(replace: Boolean) {
        // Given a terminal chat failure instead of the requested rule session.
        every { page.terminalFailureText() } returns AI_UNAVAILABLE_MESSAGE
        every { page.suggestionsAreForLatestRequest() } returns false
        every { defs.waitForBotSuggestions() } answers { callOriginal() }

        // When
        val failure = shouldThrow<AssertionError> { requestCommentChange(replace) }

        // Then
        failure.message shouldContain AI_UNAVAILABLE_MESSAGE
        failure.message shouldContain "suggested conditions for the latest request"
        sentMessages.size shouldBe 1
    }

    private fun requestCommentChange(replace: Boolean) {
        if (replace) defs.requestTheOnlyCommentBeReplacedWithoutConfirmationBy("Comment 2.")
        else defs.requestOnlyCommentBeRemovedWithoutConfirmation()
    }

    @ParameterizedTest
    @CsvSource("true, true", "true, false", "false, true", "false, false")
    fun `confirm only when asked before allowing the cornerstone report change`(
        anotherComment: Boolean,
        confirmationRequested: Boolean
    ) {
        // Given a bot that may request confirmation or proceed directly to suggestions.
        every { page.mostRecentBotRowContainsTerms(listOf(CONFIRM)) } returns confirmationRequested
        every { page.suggestionsAreForLatestRequest() } returns !confirmationRequested

        // When adding either the first or another comment and allowing the report change.
        if (anotherComment) {
            defs.addAnotherCommentUsingChatAndAllowCornerstoneReportChange("CDE is also normal.")
        } else {
            defs.addCommentUsingChatAndAllowCornerstoneReportChange("CDE is also normal.")
        }

        // Then confirm exactly when asked, before declining reasons and allowing the change.
        sentMessages shouldBe buildList {
            add("Add the comment: \"CDE is also normal.\"")
            if (confirmationRequested) add("yes")
            add("no")
            add("allow")
        }
        verifyOrder {
            defs.waitForBotSuggestions()
            defs.decline()
            defs.waitForBotQuestionToAllowReportChangeToCornerstoneThenConfirm()
            defs.waitForBotToSayDone()
        }
    }
}
