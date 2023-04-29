package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.ConclusionsDialogPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
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
    private lateinit var conclusionsDialogPO: ConclusionsDialogPO
    private val caseName = "Case1"
    private val tshComment = "Normal TSH"
    private val abcComment = "Unusual ABC value"
    private lateinit var tsh : Attribute
    private lateinit var abc : Attribute
    private lateinit var condition1: Condition
    private lateinit var condition2 : Condition
    private lateinit var condition3 : Condition
    private lateinit var condition4 : Condition

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        tsh = attributeFactory.create("TSH")
        abc = attributeFactory.create("ABC")
        condition1 = conditionFactory.create(IsNormal(null, tsh))
        condition2 = conditionFactory.create(LessThanOrEqualTo(null, tsh, 0.7))
        condition3 = conditionFactory.create(GreaterThanOrEqualTo(null, abc, 6.1))
        condition4 = conditionFactory.create(LessThanOrEqualTo(null, abc, 7.1))
        setupCase()
        buildRuleForTSH()
        buildRuleForABC()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver).apply { waitForNumberWaitingToBe(1) }
        caseViewPO = CaseViewPO(driver)
        conclusionsDialogPO = ConclusionsDialogPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    private fun buildRuleForTSH() {
        with(RESTClient()) {
            getCaseWithName(caseName)
            val tshComment = conclusionFactory.create(tshComment)
            startSessionToAddConclusionForCurrentCase(tshComment)
            addConditionForCurrentSession(condition1)
            addConditionForCurrentSession(condition2)
            commitCurrentSession()
        }
    }

    private fun buildRuleForABC() {
        with(RESTClient()) {
            getCaseWithName(caseName)
            val abcConclusion = conclusionFactory.create(abcComment)
            startSessionToAddConclusionForCurrentCase(abcConclusion)
            addConditionForCurrentSession(condition3)
            addConditionForCurrentSession(condition4)
            commitCurrentSession()
        }
    }

    @Test
    fun shouldOpenAndCloseConclusionsDialog() {
        caseViewPO.nameShown() shouldBe caseName //sanity check
        with(conclusionsDialogPO) {
            clickOpen()
            waitForDialogToOpen()
            clickClose()
            waitForDialogToClose()
        }
    }

    @Test
    fun shouldShowConditionsForEachConclusion() {
        caseViewPO.nameShown() shouldBe caseName //sanity check
        with(conclusionsDialogPO) {
            clickOpen()
            waitForDialogToOpen()
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
            clickClose()
        }
    }

    private fun setupCase() {
        labProxy.copyCase(caseName)
    }
}