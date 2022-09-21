package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.NoCaseViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.IsNormal
import kotlinx.coroutines.runBlocking
import kotlin.test.*


// ORD2
internal class ShowCaseInterpretation : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private val comment = "Normal TSH."

    @BeforeTest
    fun setup() {
        resetKB()
        setupCases()
        buildRule()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        pause()//todo use Awaitility
        caseQueuePO.refresh()
        caseListPO = caseQueuePO.review()
    }

    @AfterTest
    fun cleanup() {
        driverClose()
    }

    private fun buildRule() {
        val restClient = RESTClient()
        restClient.getCaseWithName("Case2")
        restClient.startSessionToAddConclusionForCurrentCase(Conclusion(comment))
        val condition = IsNormal(Attribute("TSH"))
        restClient.addConditionForCurrentSession(condition)
        restClient.commitCurrentSession()
    }

    @Test
    fun caseShowsCommentAddedByRule() {
        buildRule()
        val caseView1 = caseListPO.select("Case1")
        caseView1.interpretationText() shouldBe comment
        val caseView2 = caseListPO.select("Case2")
        caseView2.interpretationText() shouldBe comment
        val caseView3 = caseListPO.select("Case3")
        caseView3.interpretationText() shouldBe ""
    }

    private fun setupCases() {
        labServerProxy.cleanCasesDir()
        labServerProxy.copyCase("Case1")
        labServerProxy.copyCase("Case2")
        labServerProxy.copyCase("Case3")
    }
}