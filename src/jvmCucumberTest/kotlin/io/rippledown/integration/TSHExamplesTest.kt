package io.rippledown.integration

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD4
internal class TSHExamplesTest : TSHTest() {

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
        assertEquals(caseViewPO.referenceRange("TPO Antibodies"), "< 6")

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
        assertEquals(caseViewPO.referenceRange("Free T3"), "3.0 - 5.5")
        checkNotes("Hyperthyroid?")

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
        assertEquals(caseViewPO.referenceRange("TSH"), "0.50 - 4.0")
        assertEquals(dataShown["Free T4"]!![0], "<5 pmol/L")
        assertEquals(dataShown["Free T4"]!![1], "8 pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "10 - 20")
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

        selectCaseAndCheckName("1.4.18")
        with (caseViewPO.datesShown()) {
            size shouldBe 2
            this[0] shouldBe "2022-02-25 13:08"
            this[1] shouldBe "2022-08-18 14:23"
        }
        dataShown.size shouldBe 6
        dataShown["Age"]!![0] shouldBe "56"
        dataShown["Age"]!![1] shouldBe "56"
        dataShown["Sex"]!![0] shouldBe "F"
        dataShown["Sex"]!![1] shouldBe "F"
        dataShown["TSH"]!![0] shouldBe "4.3 mU/L"
        dataShown["TSH"]!![1] shouldBe "3.6 mU/L"
        caseViewPO.referenceRange("TSH") shouldBe "0.50 - 4.0"
        dataShown["Free T4"]!![0] shouldBe "13 pmol/L"
        dataShown["Free T4"]!![1] shouldBe "12 pmol/L"
        caseViewPO.referenceRange("Free T4") shouldBe "10 - 20"
        dataShown["Tests"]!![0] shouldBe "TFTs"
        dataShown["Tests"]!![1] shouldBe "TFTs"
        dataShown["Clinical Notes"]!![0] shouldBe  ""
        dataShown["Clinical Notes"]!![1] shouldBe  "Subclinical hypothyroidism, follow-up."

        selectCaseAndCheckName("1.4.19")
        assertEquals(dataShown.size, 7)
        checkAgeSexTestsLocation(37, "F")
        checkTSH("0.03")
        checkFreeT4("20")
        checkNotes( "Amenorrhea.")

        selectCaseAndCheckName("1.4.20")
        dataShown.size shouldBe 8
        checkAgeSexTestsLocation(53, "F")
        checkTSH("<0.01")
        checkFreeT4("16")
        checkFreeT3("5.5")
        checkNotes( "Annual check.")

        selectCaseAndCheckName("1.4.21")
        with (caseViewPO.datesShown()) {
            size shouldBe 2
            this[0] shouldBe "2023-02-14 11:54"
            this[1] shouldBe "2023-08-04 09:40"
        }
        dataShown.size shouldBe 7
        dataShown["Age"]!![0] shouldBe "53"
        dataShown["Age"]!![1] shouldBe "53"
        dataShown["Sex"]!![0] shouldBe "F"
        dataShown["Sex"]!![1] shouldBe "F"
        dataShown["TSH"]!![0] shouldBe "<0.01 mU/L"
        dataShown["TSH"]!![1] shouldBe "<0.01 mU/L"
        caseViewPO.referenceRange("TSH") shouldBe "0.50 - 4.0"
        dataShown["Free T4"]!![0] shouldBe "16 pmol/L"
        dataShown["Free T4"]!![1] shouldBe "17 pmol/L"
        caseViewPO.referenceRange("Free T4") shouldBe "10 - 20"
        dataShown["Free T3"]!![0] shouldBe "5.5 pmol/L"
        dataShown["Free T3"]!![1] shouldBe "6.1 pmol/L"
        caseViewPO.referenceRange("Free T3") shouldBe "3.0 - 5.5"
        dataShown["Tests"]!![0] shouldBe "TFTs"
        dataShown["Tests"]!![1] shouldBe "TFTs"
        dataShown["Clinical Notes"]!![0] shouldBe "Annual check."
        dataShown["Clinical Notes"]!![1] shouldBe "Previous suppressed TSH."

        selectCaseAndCheckName("1.4.22")
        dataShown.size shouldBe 8
        checkAgeSexTestsLocation(84, "F", location = "Emergency Department.")
        checkTSH("<0.01")
        checkFreeT4("45")
        checkFreeT3("18")
        checkNotes( "Severe hypertension, sweating and palpitation.")

        selectCaseAndCheckName("1.4.23")
        dataShown.size shouldBe 7
        checkAgeSexTestsLocation(46, "F")
        checkTSH("<0.01")
        checkFreeT4("7")
        checkNotes( "Started carbimazole therapy recently for Graves’ disease.")

        selectCaseAndCheckName("1.4.24")
        dataShown.size shouldBe 6
        checkAgeSexTestsLocation(59, "F", location = "Nuclear Medicine.")
        checkTSH("120")
        checkNotes( "Thyroid cancer. Pre I-131 Thyrogen therapy.")

        selectCaseAndCheckName("1.4.25")
        dataShown.size shouldBe 8
        checkAgeSexTestsLocation(64, "F", tests = "Tg/TgAb", location = "Oncology Clinic.")
        dataShown["Thyroglobulin"]!![0] shouldBe "31 μg/L"
        dataShown["Anti-Thyroglobulin"]!![0] shouldBe "<1 kU/L"
        caseViewPO.referenceRange("Anti-Thyroglobulin") shouldBe "< 4"
        checkNotes("Thyroid cancer. Post thyroidectomy, monitoring.")

        selectCaseAndCheckName("1.4.26")
        dataShown.size shouldBe 8
        checkAgeSexTestsLocation(64, "F", tests = "Tg/TgAb", location = "Oncology Clinic.")
        dataShown["Thyroglobulin"]!![0] shouldBe "<0.1 μg/L"
        dataShown["Anti-Thyroglobulin"]!![0] shouldBe "14 kU/L"
        caseViewPO.referenceRange("Anti-Thyroglobulin") shouldBe "< 4"
        checkNotes("Thyroid cancer. Post thyroidectomy, monitoring.")

        selectCaseAndCheckName("1.4.27")
        dataShown.size shouldBe 9
        checkAgeSexTestsLocation(50, "M", tests = "TFTs")
        checkTSH("4.2")
        checkFreeT4("11")
        checkFreeT3("5.6")
        dataShown["TPO Antibodies"]!![0] shouldBe "876 kU/L"
        caseViewPO.referenceRange("TPO Antibodies") shouldBe "< 6"
        checkNotes("Family history of thyroid disease.")

        selectCaseAndCheckName("1.4.28")
        dataShown.size shouldBe 8
        checkAgeSexTestsLocation(63, "M", tests = "TFTs")
        checkTSH("<0.01")
        checkFreeT4("23")
        checkFreeT3("5.0")
        checkNotes("On amiodarone.")

        selectCaseAndCheckName("1.4.29")
        dataShown.size shouldBe 7
        checkAgeSexTestsLocation(53, "M", tests = "TFTs", location = "General Practice.")
        checkTSH("1.3")
        checkFreeT4("26")
        checkNotes("Diabetes.")

        selectCaseAndCheckName("1.4.30")
        dataShown.size shouldBe 8
        checkAgeSexTestsLocation(53, "M", tests = "TFTs", location = "General Practice.")
        checkTSH("1.3")
        checkFreeT4("26")
        checkFreeT3("6.1")
        checkNotes("Diabetes.")

        selectCaseAndCheckName("1.4.31")
        dataShown.size shouldBe 14
        checkAgeSexTestsLocation(39, "M", tests = "TFTs", location = "Emergency Dept.")
        checkTSH("<0.01")
        checkFreeT4("43")
        checkFreeT3("22")
        checkNotes("General weakness.")
        dataShown["Sodium"]!![0] shouldBe "143 mmol/L"
        caseViewPO.referenceRange("Sodium") shouldBe "134 - 146"
        dataShown["Potassium"]!![0] shouldBe "2.4 mmol/L"
        caseViewPO.referenceRange("Potassium") shouldBe "3.4 - 5.0"
        dataShown["Bicarbonate"]!![0] shouldBe "18 mmol/L"
        caseViewPO.referenceRange("Bicarbonate") shouldBe "22 - 32"
        dataShown["Urea"]!![0] shouldBe "6.0 mmol/L"
        caseViewPO.referenceRange("Urea") shouldBe "3.0 - 8.0"
        dataShown["Creatinine"]!![0] shouldBe "62 μmol/L"
        caseViewPO.referenceRange("Creatinine") shouldBe "60 - 110"
        dataShown["eGFR"]!![0] shouldBe ">90 mL/min/1.73 m∧2"

        selectCaseAndCheckName("1.4.32")
        dataShown.size shouldBe 7
        checkAgeSexTestsLocation(60, "M", tests = "TFTs", location = "General Practice.")
        checkTSH("4.5")
        checkFreeT4("8")
        checkNotes("Previous raised TSH.")

        selectCaseAndCheckName("1.4.33")
        dataShown.size shouldBe 7
        checkAgeSexTestsLocation(67, "M", tests = "TFTs", location = "General Practice.")
        checkTSH("0.02")
        checkFreeT4("8")
        checkNotes("Pituitary failure. On T4.")

        // We haven't done case 35 as it is a chemistry panel.

        selectCaseAndCheckName("1.4.35")
        dataShown.size shouldBe 7
        checkAgeSexTestsLocation(66, "F", tests = "TFTs", location = "Emergency Department.")
        checkTSH("100") //">100" in the document, but 1.4.24 has "120".
        checkFreeT4("8")
        checkNotes("Semi-coma.")
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
        assertEquals(caseViewPO.referenceRange("TSH"), "0.50 - 4.0")
    }

    private fun checkFreeT4(value: String) {
        val dataShown = caseViewPO.valuesShown()
        assertEquals(dataShown["Free T4"]!![0], "$value pmol/L")
        assertEquals(caseViewPO.referenceRange("Free T4"), "10 - 20")
    }

    private fun checkFreeT3(value: String) {
        val dataShown = caseViewPO.valuesShown()
        dataShown["Free T3"]!![0] shouldBe "$value pmol/L"
        caseViewPO.referenceRange("Free T3") shouldBe  "3.0 - 5.5"
    }
}