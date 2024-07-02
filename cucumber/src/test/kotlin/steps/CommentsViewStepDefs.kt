package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En

class CommentsViewStepDefs : En {
    init {

        When("I click on the Comments tab") {
            conclusionsViewPO().selectConclusionsTab()
        }
        Then("I should see the condition for each comment as follows:") { dataTable: DataTable ->
            val expected = dataTable.asLists().drop(1)// Remove the header row
            expected.forEachIndexed { index, row ->
                val comment = row[0]
                val condition = row[1]
                conclusionsViewPO().requireCommentAtIndex(index, comment)
                conclusionsViewPO().requireConditionAtIndex(index, 0, condition)
            }
        }

        Then("the following comment is shown:") { dataTable: DataTable ->
            val expected = dataTable.asLists()
            expected.forEachIndexed { index, row ->
                val comment = row[0]
                conclusionsViewPO().requireCommentAtIndex(index, comment)
            }
        }
    }
}
