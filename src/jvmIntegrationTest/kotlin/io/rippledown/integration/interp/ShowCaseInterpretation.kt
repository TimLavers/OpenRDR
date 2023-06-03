package io.rippledown.integration.interp

import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.condition.IsNormal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


// ORD2
internal class ShowCaseInterpretation : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private lateinit var interpretationViewPO: InterpretationViewPO
    private val comment = "Normal TSH."

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCases()
        buildRule()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseQueuePO.waitForNumberOfCasesToBe(3)
        caseListPO = CaseListPO(driver)
        interpretationViewPO = InterpretationViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    private fun buildRule() {
        val restClient = RESTClient()
        restClient.getCaseWithName("Case2")
        val conclusion = conclusionFactory.getOrCreate(comment)
        restClient.startSessionToAddConclusionForCurrentCase(conclusion)
        val tsh = attributeFactory.create("TSH")
        val condition = IsNormal(null, tsh)
        restClient.addConditionForCurrentSession(condition)
        restClient.commitCurrentSession()
    }

    @Test
    fun caseShowsCommentAddedByRule() {
        caseListPO.select("Case1")
        interpretationViewPO.interpretationText() shouldBe comment
        caseListPO.select("Case2")
        interpretationViewPO.interpretationText() shouldBe comment
        caseListPO.select("Case3")
        interpretationViewPO.interpretationText() shouldBe ""
    }

    private fun setupCases() {
        labProxy.copyCase("Case1")
        labProxy.copyCase("Case2")
        labProxy.copyCase("Case3")
    }
}