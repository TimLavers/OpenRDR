package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBModelIntegrationTest {
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        val kbInfo = KBInfo("id123", "MyKB")
        kb = KB(InMemoryKB(kbInfo))
    }

    @Test
    fun `should delegate starting a conversation to the ChatManager using Gemini`() = runTest {
        //Given
        val case = createCase("Case")

        //When
        val response = kb.startConversation(case)

        //Then
        response shouldBe "Would you like to add a comment to the report?"//todo use some known constant
    }

    @Test
    fun `should delegate user message to the ChatManager using Gemini`() = runTest {
        //Given
        val case = createCase("Case")
        kb.startConversation(case)
        val userExpression = "Please add a comment to go to Bondi"

        //When
        val response = kb.responseToUserMessage(userExpression)

        //Then
        response shouldBe "Please confirm that you want to add the comment: 'to go to Bondi.'"
    }

    private fun glucose() = kb.attributeManager.getOrCreate("Glucose")

    private fun createCase(
        caseName: String,
        attribute: Attribute = glucose(),
        value: String = "0.667",
        range: ReferenceRange? = null,
        id: Long? = null
    ): RDRCase {
        with(RDRCaseBuilder()) {
            val testResult = TestResult(value, range)
            addResult(attribute, defaultDate, testResult)
            return build(caseName, id)
        }
    }
}