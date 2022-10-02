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
        resetKB()
        setupCases()
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

    fun selectCaseAndCheckName(name: String) {
        caseViewPO = caseListPO.select(name)
        dataShown = caseViewPO.valuesShown()
        assertEquals(name, caseViewPO.nameShown())
    }

    open fun setupCases() {
        labServerProxy.cleanCasesDir()
        labServerProxy.writeCaseToInputDir(TSH1)
        labServerProxy.writeCaseToInputDir(TSH2)
        labServerProxy.writeCaseToInputDir(TSH3)
        labServerProxy.writeCaseToInputDir(TSH4)
        labServerProxy.writeCaseToInputDir(TSH5)
        labServerProxy.writeCaseToInputDir(TSH6)
        labServerProxy.writeCaseToInputDir(TSH7)
        labServerProxy.writeCaseToInputDir(TSH8)
        labServerProxy.writeCaseToInputDir(TSH9)
        labServerProxy.writeCaseToInputDir(TSH10)
        labServerProxy.writeCaseToInputDir(TSH11)
        labServerProxy.writeCaseToInputDir(TSH12)
        labServerProxy.writeCaseToInputDir(TSH13)
        labServerProxy.writeCaseToInputDir(TSH14)
        labServerProxy.writeCaseToInputDir(TSH15)
        labServerProxy.writeCaseToInputDir(TSH16)
        labServerProxy.writeCaseToInputDir(TSH17)
    }
}