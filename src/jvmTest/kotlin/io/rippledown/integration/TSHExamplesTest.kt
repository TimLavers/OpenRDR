package io.rippledown.integration

import io.rippledown.examples.vltsh.*
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import io.rippledown.integration.pageobjects.CaseViewPO
import kotlin.test.*

// ORD4
internal class TSHExamplesTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var dataShown: Map<String, List<String>>

    @BeforeTest
    fun setup() {
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

    @Test
    fun tshCases() {
        caseViewPO = caseListPO.select("1.4.1")
        assertEquals(caseViewPO.nameShown(), "1.4.1")
        var dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "28")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "0.67 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "16 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Lethargy.")

        caseViewPO = caseListPO.select("1.4.2")
        assertEquals(caseViewPO.nameShown(), "1.4.2")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 6)
        assertEquals(dataShown["Age"]!![0], "28")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "0.67 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Lethargy.")

        caseViewPO = caseListPO.select("1.4.3")
        assertEquals(caseViewPO.nameShown(), "1.4.3")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "36")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "0.74 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "8 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Weight loss.")

        caseViewPO = caseListPO.select("1.4.4")
        assertEquals(caseViewPO.nameShown(), "1.4.4")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 6)
        assertEquals(dataShown["Age"]!![0], "57")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "7.3 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Weight gain.")

        caseViewPO = caseListPO.select("1.4.5")
        assertEquals(caseViewPO.nameShown(), "1.4.5")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "57")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "7.3 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Weight gain.")

        caseViewPO = caseListPO.select("1.4.6")
        assertEquals(caseViewPO.nameShown(), "1.4.6")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "76")
        assertEquals(dataShown["Sex"]!![0], "M")
        assertEquals(dataShown["TSH"]!![0], "4.5 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "15 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Routine check.")

        caseViewPO = caseListPO.select("1.4.7")
        assertEquals(caseViewPO.nameShown(), "1.4.7")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "62")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "14.0 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Constipation.")

        caseViewPO = caseListPO.select("1.4.8")
        assertEquals(caseViewPO.nameShown(), "1.4.8")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "27")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "0.05 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "Obstetric clinic.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Period of amenorrhea 12/40 weeks.")

        caseViewPO = caseListPO.select("1.4.9")
        assertEquals(caseViewPO.nameShown(), "1.4.9")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 8)
        assertEquals(dataShown["Age"]!![0], "32")
        assertEquals(dataShown["Sex"]!![0], "F")
        assertEquals(dataShown["TSH"]!![0], "4.6 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Trying for a baby.")
        assertEquals(dataShown["TPO Antibodies"]!![0], "33 kU/L")
        assertEquals(caseViewPO.referenceRange("TPO Antibodies"), "(<6)")

        caseViewPO = caseListPO.select("1.4.10")
        assertEquals(caseViewPO.nameShown(), "1.4.10")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "55")
        assertEquals(dataShown["Sex"]!![0], "M")
        assertEquals(dataShown["TSH"]!![0], "0.02 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "18 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Feeling very tired.")

        caseViewPO = caseListPO.select("1.4.11")
        assertEquals(caseViewPO.nameShown(), "1.4.11")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 8)
        assertEquals(dataShown["Age"]!![0], "55")
        assertEquals(dataShown["Sex"]!![0], "M")
        assertEquals(dataShown["TSH"]!![0], "0.02 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "18 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Free T3"]!![0], "6.1 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T3"), "(3.0 - 5.5)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Hyperthyroid?")

        caseViewPO = caseListPO.select("1.4.12")
        assertEquals(caseViewPO.nameShown(), "1.4.12")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"]!![0], "74")
        assertEquals(dataShown["Sex"]!![0], "M")
        assertEquals(dataShown["TSH"]!![0], "59 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"]!![0], "<5 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"]!![0], "General Practice.")
        assertEquals(dataShown["Tests"]!![0], "TFTs")
        assertEquals(dataShown["Clinical Notes"]!![0], "Hypothyroid?")

        caseViewPO = caseListPO.select("1.4.13")
        assertEquals(caseViewPO.nameShown(), "1.4.13")
        val datesShown = caseViewPO.datesShown()
        assertEquals(2, datesShown.size)
        assertEquals("2022-08-18 13:07", datesShown[0])
        assertEquals("2022-08-25 14:22", datesShown[1])
        dataShown = caseViewPO.valuesShown()
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