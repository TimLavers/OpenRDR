package io.rippledown.integration

import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.examples.vltsh.TSHCases
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
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        interpretationViewPO = InterpretationViewPO(driver)
        pause()//todo use Awaitility
        caseListPO = CaseListPO(driver)
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    fun selectCaseAndCheckName(name: String) {
        caseViewPO = caseListPO.select(name)
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
    }
}