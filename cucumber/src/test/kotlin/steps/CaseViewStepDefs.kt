package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.kotest.matchers.shouldBe

class CaseViewStepDefs : En {
    init {
        Then("I (should )see these episode dates:") { dataTable: DataTable ->
            val expectedDates = dataTable.asList()
            StepsInfrastructure.client().rdUiOperator.caseViewPO().datesShown() shouldBe expectedDates
        }

        Then("I (should )see these attributes:") { dataTable: DataTable ->
            val expectedNames = dataTable.asList()
            StepsInfrastructure.client().rdUiOperator.caseViewPO().attributeNames() shouldBe expectedNames
        }

        Then("I (should )see these values for {string}:") { attribute: String, dataTable: DataTable ->
            StepsInfrastructure.client().rdUiOperator.caseViewPO().valuesForAttribute(attribute) shouldBe dataTable.asList()
        }

        Then("I (should )see {string} as reference range for {string}") { range: String, attribute: String ->
            StepsInfrastructure.client().rdUiOperator.caseViewPO().referenceRange(attribute) shouldBe range
        }

        Then("blah") {
            val cd = StepsInfrastructure.client().caseViewPO.valuesShown()
            println("cd: $cd")
        }

        Then("I (should )see these case values:") { dataTable: DataTable ->
            val caseViewPO = StepsInfrastructure.client().rdUiOperator.caseViewPO()
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
                attribute shouldBe expectedAttributes[row]
                val expectedRow = expectedValues.row(row)
                val rowShown = valuesShown[attribute]
                println("row = ${row}, attribute = ${attribute}")
                println("expected row " + expectedRow)
                println("row shown    " + rowShown)
                rowShown shouldBe expectedRow
                val expectedReferenceRange = referenceRangesExpected[row].orEmpty()
                val referenceRangeShown = caseViewPO.referenceRange(attribute)
                expectedReferenceRange shouldBe referenceRangeShown
            }
        }
    }
}
