package io.rippledown.integration

import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.examples.vltsh.TSHCases
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal open class TSHTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    lateinit var caseViewPO: CaseViewPO
    lateinit var dataShown: Map<String, List<String>>

    @BeforeTest
    fun setup() {
        serverProxy.start()
        resetKB()
        setupCases()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
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
        labProxy.writeCaseToInputDir(tshCases.TSH1)
        labProxy.writeCaseToInputDir(tshCases.TSH2)
        labProxy.writeCaseToInputDir(tshCases.TSH3)
        labProxy.writeCaseToInputDir(tshCases.TSH4)
        labProxy.writeCaseToInputDir(tshCases.TSH5)
        labProxy.writeCaseToInputDir(tshCases.TSH6)
        labProxy.writeCaseToInputDir(tshCases.TSH7)
        labProxy.writeCaseToInputDir(tshCases.TSH8)
        labProxy.writeCaseToInputDir(tshCases.TSH9)
        labProxy.writeCaseToInputDir(tshCases.TSH10)
        labProxy.writeCaseToInputDir(tshCases.TSH11)
        labProxy.writeCaseToInputDir(tshCases.TSH12)
        labProxy.writeCaseToInputDir(tshCases.TSH13)
        labProxy.writeCaseToInputDir(tshCases.TSH14)
        labProxy.writeCaseToInputDir(tshCases.TSH15)
        labProxy.writeCaseToInputDir(tshCases.TSH16)
        labProxy.writeCaseToInputDir(tshCases.TSH17)
    }
}