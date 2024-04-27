package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.kotest.matchers.shouldBe

class CaseViewStepDefs : En {
    init {
        Then("I (should )see these episode dates:") { dataTable: DataTable ->
            val expectedDates = dataTable.asList()
            caseViewPO().datesShown() shouldBe expectedDates
        }

        Then("I (should )see these attributes:") { dataTable: DataTable ->
            val expectedNames = dataTable.asList()
            caseViewPO().attributeNames() shouldBe expectedNames
        }

        Then("I (should )see these values for {string}:") { attribute: String, dataTable: DataTable ->
            caseViewPO().valuesForAttribute(attribute) shouldBe dataTable.asList()
        }

        Then("I (should )see {string} as reference range for {string}") { range: String, attribute: String ->
            caseViewPO().referenceRange(attribute) shouldBe range
        }

        Then("I (should )see these case values:") { dataTable: DataTable ->
            val caseViewPO = caseViewPO()
            val valuesShown = caseViewPO.valuesShown()
            val rowCount = valuesShown.size
            // Check the number of rows is correct.
            rowCount shouldBe dataTable.height()
            // Extract the expected attributes, reference ranges and test result values.
            val expectedAttributes = dataTable.column(0)
            val referenceRangesExpected = dataTable.column(dataTable.width() - 1)
            val expectedValues = dataTable.subTable(0, 1, rowCount, dataTable.width() - 1)
            // Check each row.
            valuesShown.keys.forEachIndexed { row, attribute ->
                val expectedRow = expectedValues.row(row)
                val rowShown = valuesShown[attribute]
                attribute shouldBe expectedAttributes[row]
                rowShown shouldBe expectedRow.map { it ?: "" }
                val expectedReferenceRange = referenceRangesExpected[row].orEmpty()
                val referenceRangeShown = caseViewPO.referenceRange(attribute)
                expectedReferenceRange shouldBe referenceRangeShown
            }
        }

        Then("the case should show the attributes in order:") { dataTable: DataTable ->
            val caseViewPO = caseViewPO()
            val valuesShown = caseViewPO.attributeNames()
            // Check the number of rows is correct.
            valuesShown.size shouldBe dataTable.height()
            val expectedAttributes = dataTable.column(0)
            valuesShown.forEachIndexed { row, attribute ->
                attribute shouldBe expectedAttributes[row]
            }
        }
    }
}
