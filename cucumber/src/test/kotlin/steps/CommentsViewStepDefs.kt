package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.rippledown.integration.pause

class CommentsViewStepDefs {

    @When("I click on the Comments tab")
    fun IClickOnTheCommentsTab() {
        conclusionsViewPO().selectConclusionsTab()
    }

    @Then("I should see the condition for each comment as follows:")
    fun IShouldSeeTheConditionForEachCommentAsFollows(dataTable: DataTable) {
        val expected = dataTable.asLists().drop(1)// Remove the header row
        expected.forEachIndexed { index, row ->
            val comment = row[0]
            val condition = row[1]
            conclusionsViewPO().requireCommentAtIndex(index, comment)
            conclusionsViewPO().requireConditionAtIndex(index, 0, condition)
        }
    }

    @Then("the following comment is shown:")
    fun theFollowingCommentIsShown(dataTable: DataTable) {
        val expected = dataTable.asLists()
        expected.forEachIndexed { index, row ->
            val comment = row[0]
            conclusionsViewPO().requireCommentAtIndex(index, comment)
        }
    }

    @Then("no comments are shown")
    fun noCommentsAreShown() {
        pause(2000)
        conclusionsViewPO().requireNoComments()
    }
}
