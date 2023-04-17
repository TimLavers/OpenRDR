package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.condition.IsNormal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


// ORD2
internal class ShowCaseInterpretation : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private val comment = "Normal TSH."

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCases()
        buildRule()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseQueuePO.waitForNumberWaitingToBe(3)
        caseListPO = CaseListPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    private fun buildRule() {
        val restClient = RESTClient()
        restClient.getCaseWithName("Case2")
        val conclusion = conclusionFactory.create(comment)
        restClient.startSessionToAddConclusionForCurrentCase(conclusion)
        val tsh = attributeFactory.create("TSH")
        val condition = IsNormal(tsh)
        restClient.addConditionForCurrentSession(condition)
        restClient.commitCurrentSession()
    }

    @Test
    fun caseShowsCommentAddedByRule() {
        val caseView1 = caseListPO.select("Case1")
        caseView1.interpretationText() shouldBe comment
        val caseView2 = caseListPO.select("Case2")
        caseView2.interpretationText() shouldBe comment
        val caseView3 = caseListPO.select("Case3")
        caseView3.interpretationText() shouldBe ""

        caseView3.interpretationArea().getCssValue("font-family") shouldBe "monospace"
    }

    private fun setupCases() {
        labProxy.copyCase("Case1")
        labProxy.copyCase("Case2")
        labProxy.copyCase("Case3")
    }
}