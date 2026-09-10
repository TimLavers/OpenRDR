package steps

import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.constants.chat.CONFIRM
import io.rippledown.integration.pageobjects.ChatPO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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
