package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.When
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement

class BackdoorRuleStepDefs {

    @When("backdoor rules are built as follows:")
    fun backdoorBuildRules(dataTable: DataTable) {
        dataTable.asLists().drop(1).forEach { row ->
            val caseName = row[0].trim()
            val add = row[1]?.trim() ?: ""
            val remove = row[2]?.trim() ?: ""
            val condition = row[3]?.trim() ?: ""
            val diff = when {
                add.isNotEmpty() && remove.isNotEmpty() -> Replacement(remove, add)
                add.isNotEmpty() -> Addition(add)
                remove.isNotEmpty() -> Removal(remove)
                else -> error("Row for case $caseName has neither add nor remove")
            }
            restClient().buildRule(caseName, diff, listOf(condition))
        }
    }

    @And("the backdoor selects the Knowledge Base {string}")
    fun backdoorSelectKB(kbName: String) {
        restClient().selectKBByName(kbName)
    }

    @And("a backdoor rule is built for case {word} to add the comment {string} with conditions:")
    fun backdoorAddComment(caseName: String, comment: String, conditions: DataTable) {
        restClient().buildRule(caseName, Addition(comment), conditions.asList())
    }

    @And("a backdoor rule is built for case {word} to remove the comment {string} with conditions:")
    fun backdoorRemoveComment(caseName: String, comment: String, conditions: DataTable) {
        restClient().buildRule(caseName, Removal(comment), conditions.asList())
    }

    @And("a backdoor rule is built for case {word} to replace the comment {string} with {string} with conditions:")
    fun backdoorReplaceComment(caseName: String, toGo: String, replacement: String, conditions: DataTable) {
        restClient().buildRule(caseName, Replacement(toGo, replacement), conditions.asList())
    }
}
