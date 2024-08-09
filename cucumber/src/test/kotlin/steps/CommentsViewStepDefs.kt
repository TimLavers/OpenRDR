package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When

class CommentsViewStepDefs {

    @When("(I )click on the Comments tab")
    fun clickOnTheCommentsTab() {
        conclusionsViewPO().selectConclusionsTab()
    }

    @Then("I should see the condition for each comment as follows:")
    fun conditionForEachCommentShouldBe(dataTable: DataTable) {
        clickOnTheCommentsTab()
        val expected = dataTable.asLists().drop(1)// Remove the header row
        expected.forEachIndexed { index, row ->
            val comment = row[0]
            val condition = row[1]
            conclusionsViewPO().requireCommentAtIndex(index, comment)
            conclusionsViewPO().requireConditionAtIndex(index, 0, condition)
        }
    }

    @Then("the conditions showing are:")
    fun requireConditionsShowing(dataTable: DataTable) {
        val expectedConditions = dataTable.asList()
        expectedConditions.forEachIndexed { index, condition ->
            conclusionsViewPO().requireConditionAtIndex(0, index, condition)
        }
    }

    @Then("the following comment is shown:")
    fun commentIsShown(dataTable: DataTable) {
        clickOnTheCommentsTab()
        val expected = dataTable.asLists()
        expected.forEachIndexed { index, row ->
            val comment = row[0]
            conclusionsViewPO().requireCommentAtIndex(index, comment)
        }
    }

    @And("(I )click the comment {string}")
    fun clickComment(comment: String) {
        clickOnTheCommentsTab()

        //TODO: Handle scenario where there is more than one comment
        conclusionsViewPO().clickCommentAtIndex(comment, 0)
    }

    @Then("no comments are shown")
    fun noCommentsAreShown() {
        clickOnTheCommentsTab()
        conclusionsViewPO().requireNoComments()
    }
}
