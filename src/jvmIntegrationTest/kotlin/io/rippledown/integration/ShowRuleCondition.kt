package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.IsNormal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


// ORD2
internal class ShowRuleCondition : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    val caseName = "Case1"
    private val comment = "Normal TSH."

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCase()
        buildRule()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseQueuePO.waitForNumberWaitingToBe(1)
        caseListPO = CaseListPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    private fun buildRule() {
        val restClient = RESTClient()
        restClient.getCaseWithName(caseName)
        restClient.startSessionToAddConclusionForCurrentCase(Conclusion(comment))
        val condition = IsNormal(Attribute("TSH"))
        restClient.addConditionForCurrentSession(condition)
        restClient.commitCurrentSession()
    }

    @Test
    fun caseShowsCommentAddedByRule() {
        val caseView = caseListPO.select(caseName)
        caseView.interpretationText() shouldBe comment
        stop()
    }

    private fun setupCase() {
        labProxy.copyCase(caseName)
    }
}