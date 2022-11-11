package io.rippledown.integration

import io.rippledown.examples.vltsh.*
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal open class TSHTest: UITestBase() {

    lateinit var caseQueuePO: CaseQueuePO
    lateinit var caseListPO: CaseListPO
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
        labProxy.writeCaseToInputDir(TSH1)
        labProxy.writeCaseToInputDir(TSH2)
        labProxy.writeCaseToInputDir(TSH3)
        labProxy.writeCaseToInputDir(TSH4)
        labProxy.writeCaseToInputDir(TSH5)
        labProxy.writeCaseToInputDir(TSH6)
        labProxy.writeCaseToInputDir(TSH7)
        labProxy.writeCaseToInputDir(TSH8)
        labProxy.writeCaseToInputDir(TSH9)
        labProxy.writeCaseToInputDir(TSH10)
        labProxy.writeCaseToInputDir(TSH11)
        labProxy.writeCaseToInputDir(TSH12)
        labProxy.writeCaseToInputDir(TSH13)
        labProxy.writeCaseToInputDir(TSH14)
        labProxy.writeCaseToInputDir(TSH15)
        labProxy.writeCaseToInputDir(TSH16)
        labProxy.writeCaseToInputDir(TSH17)
    }
}