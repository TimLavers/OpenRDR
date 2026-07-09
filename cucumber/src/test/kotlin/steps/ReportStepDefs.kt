package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class ReportStepDefs {

    @When("I click to show the report panel")
    fun clickToShowReportPanel() {
        reportPO().clickReportToggle()
    }

    @Then("the report (should contain )(contains )the phrase(s):")
    fun reportShouldContainPhrases(phrases: DataTable) {
        reportPO().waitForReportPanelToBeVisible()
        reportPO().waitForReportTextToContain(phrases.asList())
    }
}
