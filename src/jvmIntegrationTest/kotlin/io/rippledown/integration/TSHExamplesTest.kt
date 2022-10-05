package io.rippledown.integration

import io.rippledown.examples.vltsh.*
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD4
internal class TSHExamplesTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var dataShown: Map<String, List<String>>

    @BeforeTest
    fun setup() {
        serverProxy.start()
        setupCases()
        setupWebDriver()
        caseQueuePO = CaseQueuePO(driver)
        caseQueuePO.refresh().waitForNumberWaitingToBe(17)
        caseListPO = caseQueuePO.review()
    }

    @AfterTest
    fun cleanup() {
        driverClose()
        serverProxy.shutdown()
    }

    @Test
    fun tshCases() {
        selectCaseAndCheckName("1.4.1")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(28, "F")
        checkTSH("0.67")
        checkFreeT4("16")
        checkNotes( "Lethargy.")

        selectCaseAndCheckName("1.4.2")
        assertEquals(dataShown.size, 6)
        checkAgeSexTestsLocation(28, "F")
        checkTSH("0.67")
        checkNotes( "Lethargy.")

        selectCaseAndCheckName("1.4.3")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(36, "F")
        checkTSH("0.74")
        checkFreeT4("8")
        checkNotes( "Weight loss.")

        selectCaseAndCheckName("1.4.4")
        assertEquals(dataShown.size, 6)
        checkAgeSexTestsLocation(57, "F")
        checkTSH("7.3")
        checkNotes( "Weight gain.")

        selectCaseAndCheckName("1.4.5")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(57, "F")
        checkTSH("7.3")
        checkFreeT4("13")
        checkNotes( "Weight gain.")

        selectCaseAndCheckName("1.4.6")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(76, "M")
        checkTSH("4.5")
        checkFreeT4("15")
        checkNotes( "Routine check.")

        selectCaseAndCheckName("1.4.7")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(62, "F")
        checkTSH("14.0")
        checkFreeT4("13")
        checkNotes( "Constipation.")

        selectCaseAndCheckName("1.4.8")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(27, "F", location="Obstetric clinic.")
        checkTSH("0.05")
        checkFreeT4("13")
        checkNotes( "Period of amenorrhea 12/40 weeks.")

        selectCaseAndCheckName("1.4.9")
        assertEquals(dataShown.size, 8)
        checkAgeSexTestsLocation(32, "F")
        checkTSH("4.6")
        checkFreeT4("13")
        checkNotes( "Trying for a baby.")
        assertEquals(dataShown["TPO Antibodies"]!![0], "33 kU/L")
        assertEquals(caseViewPO.referenceRange("TPO Antibodies"), "(< 6)")

        selectCaseAndCheckName("1.4.10")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(55, "M")
        checkTSH("0.02")
        checkFreeT4("18")
        checkNotes( "Feeling very tired.")

        selectCaseAndCheckName("1.4.11")
        assertEquals(dataShown.size, 8)
        checkAgeSexTestsLocation(55, "M")
        checkTSH("0.02")
        checkFreeT4("18")
        assertEquals(dataShown["Free T3"]!![0], "6.1 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T3"), "(3.0 - 5.5)")
        checkNotes( "Hyperthyroid?")

        selectCaseAndCheckName("1.4.12")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(74, "M")
        checkTSH("59")
        checkFreeT4("<5")
        checkNotes( "Hypothyroid?")

        selectCaseAndCheckName("1.4.13")
        val datesShown = caseViewPO.datesShown()
        assertEquals(2, datesShown.size)
        assertEquals("2022-08-18 13:08", datesShown[0])
        assertEquals("2022-08-25 14:23", datesShown[1])
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "74")
        assertEquals(dataShown["Age"]!![1], "74")
        assertEquals(dataShown["Sex"]!![0], "M")
        assertEquals(dataShown["Sex"]!![1], "M")
        assertEquals(dataShown["TSH"]!![0], "59 mU/L")
        assertEquals(dataShown["TSH"]!![1], "40 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "<5 pmol/L")
        assertEquals(dataShown["Free T4"]!![1], "8 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Patient Location"]!![1], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Tests"]!![1], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Hypothyroid?")
        assertEquals(dataShown["Clinical Notes"]!![1], "Hypothyroid, started T4 replacement 1 week ago.")

        selectCaseAndCheckName("1.4.14")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(43, "F")
        checkTSH("0.72")
        checkFreeT4("16")
        checkNotes( "On T4 replacement.")

        selectCaseAndCheckName("1.4.15")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(54, "F")
        checkTSH("5.6")
        checkFreeT4("12")
        checkNotes( "On T4 replacement.")

        selectCaseAndCheckName("1.4.16")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(61, "F")
        checkTSH("0.02")
        checkFreeT4("19")
        checkNotes( "On T4 replacement.")

        selectCaseAndCheckName("1.4.17")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(51, "F")
        checkTSH("0.12")
        checkFreeT4("19")
        checkNotes( "Previous total thyroidectomy for thyroid cancer. On thyroxine.")

    }

    private fun selectCaseAndCheckName(name: String) {
        caseViewPO = caseListPO.select(name)
        dataShown = caseViewPO.valuesShown()
        assertEquals(name, caseViewPO.nameShown())
    }

    private fun checkAgeSexTestsLocation(age: Int, sex: String, tests: String = "TFTs", location: String = "General Practice.") {
        assertEquals(dataShown["Age"]!![0], "$age")
        assertEquals(dataShown["Sex"]!![0], sex)
        assertEquals(dataShown["Tests"]!![0], tests)
        assertEquals(dataShown["Patient Location"]!![0], location)
    }

    private fun checkNotes(value: String) {
        assertEquals(value, dataShown["Clinical Notes"]!![0])
    }

    private fun checkTSH(value: String) {
        assertEquals(dataShown["TSH"]!![0], "$value mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
    }

    private fun checkFreeT4(value: String) {
        val dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown["Free T4"]!![0], "$value pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")

    }

    private fun setupCases() {
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