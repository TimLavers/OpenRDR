package io.rippledown.integration

import io.kotest.matchers.shouldBe
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.ConclusionsViewPO
import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.TabularCondition
import io.rippledown.model.condition.tabular.chain.Current
import io.rippledown.model.condition.tabular.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.tabular.predicate.LessThanOrEquals
import io.rippledown.model.condition.tabular.predicate.Normal
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
        condition1 = conditionFactory.getOrCreate(TabularCondition(null, tsh, Normal, Current))
        condition2 = conditionFactory.getOrCreate(TabularCondition(null, tsh, LessThanOrEquals(0.7), Current))
        condition3 = conditionFactory.getOrCreate(TabularCondition(null, abc, GreaterThanOrEquals(6.1), Current))
        condition4 = conditionFactory.getOrCreate(TabularCondition(null, abc, LessThanOrEquals(7.1), Current))
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
            val tshComment = conclusionFactory.getOrCreate(tshComment)
            startSessionToAddConclusionForCurrentCase(tshComment)
            addConditionForCurrentSession(condition1)
            addConditionForCurrentSession(condition2)
            commitCurrentSession()
        }
    }

    private fun buildRuleForABC() {
        with(RESTClient()) {
            getCaseWithName(caseName)
            val abcConclusion = conclusionFactory.getOrCreate(abcComment)
            startSessionToAddConclusionForCurrentCase(abcConclusion)
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
        labProxy.provideCase(caseName)
    }
}