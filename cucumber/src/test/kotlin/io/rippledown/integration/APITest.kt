package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.ConclusionsViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.main.Api
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class APITest {
    private val caseName = "Case1"
    private val uiTestBase = UITestBase()
    private val originalText = "Go to Bondi."

    @BeforeTest
    fun setup() {
        with(uiTestBase) {
            serverProxy.start()
            resetKB()
            labProxy.provideCase(caseName)
        }
    }

    @AfterTest
    fun cleanup() {
        uiTestBase.serverProxy.shutdown()
    }

    @Test
    fun `should save edited interpretation`() {
        runBlocking {
            with(RESTClient()) {

                // Given
                createRuleToAddText(caseName, originalText)
                val viewableCase = getCaseWithName(caseName)!!
                viewableCase.viewableInterpretation.verifiedText shouldBe null
                viewableCase.viewableInterpretation.latestText() shouldBe originalText

                // When
                val editedText = "Go to Bondi. And bring your flippers."
                viewableCase.viewableInterpretation.verifiedText = editedText
                val returned = Api().saveVerifiedInterpretation(viewableCase.viewableInterpretation)

                // Then
                returned.verifiedText shouldBe editedText
                returned.latestText() shouldBe editedText
            }
        }
    }


}