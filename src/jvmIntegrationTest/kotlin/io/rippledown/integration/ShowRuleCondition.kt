package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.ConclusionsViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.GreaterThanOrEqualTo
import io.rippledown.model.condition.IsNormal
import io.rippledown.model.condition.LessThanOrEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test


// ORD2
internal class ShowRuleCondition : UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var conclusionsViewPO: ConclusionsViewPO
    private val caseName = "Case1"
    private val tshComment = "Normal TSH"
    private val abcComment = "Unusual ABC value"
    private val tsh = Attribute("TSH")
    private val abc = Attribute("ABC")
    private val condition1 = IsNormal(tsh)
    private val condition2 = LessThanOrEqualTo(tsh, 0.7)
    private val condition3 = GreaterThanOrEqualTo(abc, 6.1)
    private val condition4 = LessThanOrEqualTo(abc, 7.1)

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCase()
        buildRuleForTSH()
        buildRuleForABC()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver).apply { waitForNumberOfCasesToBe(1) }
        caseViewPO = CaseViewPO(driver)
        conclusionsViewPO = ConclusionsViewPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    private fun buildRuleForTSH() {
        with(RESTClient()) {
            getCaseWithName(caseName)
            startSessionToAddConclusionForCurrentCase(Conclusion(tshComment))
            addConditionForCurrentSession(condition1)
            addConditionForCurrentSession(condition2)
            commitCurrentSession()
        }
    }

    private fun buildRuleForABC() {
        with(RESTClient()) {
            getCaseWithName(caseName)
            startSessionToAddConclusionForCurrentCase(Conclusion(abcComment))
            addConditionForCurrentSession(condition3)
            addConditionForCurrentSession(condition4)
            commitCurrentSession()
        }
    }

    @Test
    fun shouldShowConditionsForEachConclusion() {
        caseViewPO.nameShown() shouldBe caseName //sanity check
        with(conclusionsViewPO) {
            selectConclusionsTab()
            clickComment(tshComment)
            requireConditionsToBeShown(
                condition1.asText(),
                condition2.asText(),
            )
            clickComment(abcComment)
            requireConditionsToBeShown(
                condition3.asText(),
                condition4.asText()
            )
        }
    }

    private fun setupCase() {
        labProxy.copyCase(caseName)
    }
}