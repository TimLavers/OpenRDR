package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class CaseListStepDefs {
    // Restores keyboard focus to the last-selected case before an arrow-key press.
    // After a case is selected, ChatPanel's LaunchedEffect(id) steals focus to the
    // chat text field, which would otherwise absorb subsequent arrow-key presses.
    private var refocusLastSelectedCase: (() -> Unit)? = null

    @And("I select case {word}( on the processed case list)")
    fun selectCase(caseName: String) {
        caseListPO().select(caseName)
        caseViewPO().waitForNameToShow(caseName)
        refocusLastSelectedCase = { caseListPO().mouseClick(caseName) }
    }

    @And("I select case {word} on the favourites case list")
    fun selectFavouritesCase(caseName: String) {
        favouriteCaseListPO().select(caseName)
        caseViewPO().waitForNameToShow(caseName)
        refocusLastSelectedCase = { caseListPO().mouseClick(caseName) }
    }

    @Then("I (should )see the following cases in the case list:")
    fun IShouldSeeTheFollowingCasesInTheCaseList(dataTable: DataTable) {
        val expectedCaseNames = dataTable.asList()
        caseListPO().requireCaseNamesToBe(expectedCaseNames)
    }

    @Then("I should see no cases in the case list")
    fun IShouldSeeNoCasesInTheCaseList() {
        caseCountPO().requireCaseCountToBeHidden()
    }

    @Then("I should see no cases in the favourites case list")
    fun IShouldSeeNoCasesInTheFavouritesCaseList() {
        caseCountPO().requireCaseCountToBeHidden()
    }

    @Then("the cornerstone case list should contain:")
    fun theCornerstoneCaseListShouldContain(dataTable: DataTable) {
        cornerstoneCaseListPO().requireCaseNamesToBe(dataTable.asList())
    }

    @Then("the processed case list should contain:")
    fun theProcessedCaseListShouldContain(dataTable: DataTable) {
        processedCaseListPO().requireCaseNamesToBe(dataTable.asList())
    }

    @Then("the favourites case list should contain:")
    fun theFavouritesCaseListShouldContain(dataTable: DataTable) {
        favouriteCaseListPO().requireCaseNamesToBe(dataTable.asList())
    }

    @And("the case list (is )(should be )hidden")
    fun theCaseListIsShouldBeHidden() {
        caseCountPO().requireCasesLabelToBeHidden()
        caseCountPO().requireCaseCountToBeHidden()
        caseListPO().requireCaseListToBeHidden()
    }

    @And("the case list (is )(should be )shown")
    fun theCaseListIsShouldBeShown() {
        caseCountPO().requireCasesLabelToBeShown()
        caseCountPO().requireCaseCountToBeShown()
        caseListPO().requireCaseListToBeShown()
    }

    @And("the count of the number of cases is {int}")
    fun theCountOfTheNumberOfCasesIsInt(numberOfCases: Int) {
        caseCountPO().waitForCountOfNumberOfCasesToBe(numberOfCases)
    }

    @And("the cornerstone case count should be {int}")
    fun theNumberOfCornerstoneCasesIsInt(numberOfCases: Int) {
        cornerstoneCaseCountPO().waitForCountOfNumberOfCasesToBe(numberOfCases)
    }

    @Then("Eventually I should not see any cases")
    fun EventuallyIShouldNotSeeAnyCases() {
        caseViewPO().waitForNoNameShowing()
    }

    @And("(I )select the case {word}")
    fun ISelectTheCaseWord(caseName: String) {
        caseListPO().select(caseName)
        caseViewPO().waitForNameToShow(caseName)
        refocusLastSelectedCase = { caseListPO().mouseClick(caseName) }
    }

    @And("I select the case {word} on the cornerstone case list")
    fun ISelectTheCornerstoneCase(caseName: String) {
        cornerstoneCaseListPO().select(caseName)
        caseViewPO().waitForNameToShow(caseName)
        refocusLastSelectedCase = { cornerstoneCaseListPO().mouseClick(caseName) }
    }

    @Then("the selected case should (still )be {word}")
    fun theSelectedCaseShouldStillBe(caseName: String) {
        caseListPO().requireCaseToBeSelected(caseName)
    }

    @When("I press the down arrow key")
    fun pressDownArrowKey() {
        // Best-effort: give any pending ChatPanel.LaunchedEffect(id) focus-steal
        // a chance to finish before we refocus the case list. If no focus-steal
        // is pending (e.g. the prior `select case` resolved to the already-current
        // case, so currentCaseId did not change and chatId did not increment),
        // we simply proceed rather than hanging for the full timeout.
        chatPO().waitForChatToBeFocusedQuietly()
        StepsInfrastructure.client().withWindowOnTop {
            refocusLastSelectedCase?.invoke()
            caseListPO().pressDownArrow()
            Thread.sleep(100)
        }
    }

    @When("I press the up arrow key")
    fun pressUpArrowKey() {
        chatPO().waitForChatToBeFocusedQuietly()
        StepsInfrastructure.client().withWindowOnTop {
            refocusLastSelectedCase?.invoke()
            caseListPO().pressUpArrow()
            Thread.sleep(100)
        }
    }
}