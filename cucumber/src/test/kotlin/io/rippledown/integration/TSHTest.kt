package io.rippledown.integration

import io.rippledown.examples.vltsh.TSHCases
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal open class TSHTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    lateinit var caseViewPO: CaseViewPO
    lateinit var interpretationViewPO: InterpretationViewPO
    lateinit var dataShown: Map<String, List<String>>

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCases()
//        setupWebDriver()
        caseQueuePO = CaseQueuePO()
//        interpretationViewPO = InterpretationViewPO()
        pause()//todo use Awaitility
//        caseListPO = CaseListPO()
    }

    @AfterTest
    fun cleanup() {
        serverProxy.shutdown()
    }

    fun selectCaseAndCheckName(name: String) {
//        caseViewPO = caseListPO.select(name)
        dataShown = caseViewPO.valuesShown()
        assertEquals(name, caseViewPO.nameShown())
    }

    open fun setupCases() {
        labProxy.cleanCasesDir()
        val tshCases = TSHCases(RestClientAttributeFactory(restClient))
        labProxy.provideCase(tshCases.TSH1)
        labProxy.provideCase(tshCases.TSH2)
        labProxy.provideCase(tshCases.TSH3)
        labProxy.provideCase(tshCases.TSH4)
        labProxy.provideCase(tshCases.TSH5)
        labProxy.provideCase(tshCases.TSH6)
        labProxy.provideCase(tshCases.TSH7)
        labProxy.provideCase(tshCases.TSH8)
        labProxy.provideCase(tshCases.TSH9)
        labProxy.provideCase(tshCases.TSH10)
        labProxy.provideCase(tshCases.TSH11)
        labProxy.provideCase(tshCases.TSH12)
        labProxy.provideCase(tshCases.TSH13)
        labProxy.provideCase(tshCases.TSH14)
        labProxy.provideCase(tshCases.TSH15)
        labProxy.provideCase(tshCases.TSH16)
        labProxy.provideCase(tshCases.TSH17)
        labProxy.provideCase(tshCases.TSH18)
        labProxy.provideCase(tshCases.TSH19)
        labProxy.provideCase(tshCases.TSH20)
        labProxy.provideCase(tshCases.TSH21)
        labProxy.provideCase(tshCases.TSH22)
        labProxy.provideCase(tshCases.TSH23)
        labProxy.provideCase(tshCases.TSH24)
        labProxy.provideCase(tshCases.TSH25)
        labProxy.provideCase(tshCases.TSH26)
        labProxy.provideCase(tshCases.TSH27)
        labProxy.provideCase(tshCases.TSH28)
        labProxy.provideCase(tshCases.TSH29)
        labProxy.provideCase(tshCases.TSH30)
        labProxy.provideCase(tshCases.TSH31)
        labProxy.provideCase(tshCases.TSH32)
        labProxy.provideCase(tshCases.TSH33)
        labProxy.provideCase(tshCases.TSH35)
    }
}