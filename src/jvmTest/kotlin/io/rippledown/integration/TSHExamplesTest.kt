package io.rippledown.integration

import io.rippledown.examples.vltsh.*
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseQueuePO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD4
internal class TSHExamplesTest: UITestBase() {

    private lateinit var caseQueuePO: CaseQueuePO
    private lateinit var caseListPO: CaseListPO

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
        var caseViewPO = caseListPO.select("1.4.1")
        assertEquals(caseViewPO.nameShown(), "1.4.1")
        var dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "28")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "0.67 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "16 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Lethargy.")

        caseViewPO = caseListPO.select("1.4.2")
        assertEquals(caseViewPO.nameShown(), "1.4.2")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 6)
        assertEquals(dataShown["Age"], "28")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "0.67 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Lethargy.")

        caseViewPO = caseListPO.select("1.4.3")
        assertEquals(caseViewPO.nameShown(), "1.4.3")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "36")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "0.74 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "8 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Weight loss.")

        caseViewPO = caseListPO.select("1.4.4")
        assertEquals(caseViewPO.nameShown(), "1.4.4")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 6)
        assertEquals(dataShown["Age"], "57")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "7.3 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Weight gain.")

        caseViewPO = caseListPO.select("1.4.5")
        assertEquals(caseViewPO.nameShown(), "1.4.5")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "57")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "7.3 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Weight gain.")

        caseViewPO = caseListPO.select("1.4.6")
        assertEquals(caseViewPO.nameShown(), "1.4.6")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "76")
        assertEquals(dataShown["Sex"], "M")
        assertEquals(dataShown["TSH"], "4.5 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "15 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Routine check.")

        caseViewPO = caseListPO.select("1.4.7")
        assertEquals(caseViewPO.nameShown(), "1.4.7")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "62")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "14.0 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Constipation.")

        caseViewPO = caseListPO.select("1.4.8")
        assertEquals(caseViewPO.nameShown(), "1.4.8")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "27")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "0.05 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "Obstetric clinic.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Period of amenorrhea 12/40 weeks.")

        caseViewPO = caseListPO.select("1.4.9")
        assertEquals(caseViewPO.nameShown(), "1.4.9")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 8)
        assertEquals(dataShown["Age"], "32")
        assertEquals(dataShown["Sex"], "F")
        assertEquals(dataShown["TSH"], "4.6 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "13 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Trying for a baby.")
        assertEquals(dataShown["TPO Antibodies"], "33 kU/L")
        assertEquals(caseViewPO.referenceRange("TPO Antibodies"), "(< 6)")

        caseViewPO = caseListPO.select("1.4.10")
        assertEquals(caseViewPO.nameShown(), "1.4.10")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "55")
        assertEquals(dataShown["Sex"], "M")
        assertEquals(dataShown["TSH"], "0.02 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "18 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Feeling very tired.")

        caseViewPO = caseListPO.select("1.4.11")
        assertEquals(caseViewPO.nameShown(), "1.4.11")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 8)
        assertEquals(dataShown["Age"], "55")
        assertEquals(dataShown["Sex"], "M")
        assertEquals(dataShown["TSH"], "0.02 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "18 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Free T3"], "6.1 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T3"), "(3.0 - 5.5)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Hyperthyroid?")

        caseViewPO = caseListPO.select("1.4.12")
        assertEquals(caseViewPO.nameShown(), "1.4.12")
        dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown.size, 7)
        assertEquals(dataShown["Age"], "74")
        assertEquals(dataShown["Sex"], "M")
        assertEquals(dataShown["TSH"], "59 mU/L")
        assertEquals(caseViewPO.referenceRange("TSH"), "(0.50 - 4.0)")
        assertEquals(dataShown["Free T4"], "<5 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "(10 - 20)")
        assertEquals(dataShown["Patient Location"], "General Practice.")
        assertEquals(dataShown["Tests"], "TFTs")
        assertEquals(dataShown["Clinical Notes"], "Hypothyroid?")

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
    }
}