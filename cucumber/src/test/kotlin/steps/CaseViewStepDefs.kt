package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.cucumber.java8.PendingException
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
    }
}
