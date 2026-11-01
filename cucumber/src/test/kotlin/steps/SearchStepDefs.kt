package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

/**
 * Steps for the "search by conditions" facility
 * (documentation/requirements/search_cases_by_condition.md).
 *
 * Search parameters are given to the chatbot in natural language, as the
 * copy-to-list requests are: the model extracts the parameters and the server
 * acts on them. The requirements specify a prompted flow - conditions,
 * connector, how many cases, which lists, destination, then a summary asking
 * for confirmation - so most scenarios give all the parameters in one request,
 * and one scenario answers the prompts one at a time.
 */
class SearchStepDefs {

    @When("I ask to search for cases by conditions")
    fun askToSearchForCasesByConditions() {
        sendSearchRequest("I would like to search for cases by conditions")
    }

    @And("I give the search condition(s):")
    fun giveSearchConditions(conditions: DataTable) {
        val quoted = conditions.asLists().map { "\"${it[0].trim()}\"" }
        sendSearchRequest("the search conditions are ${quoted.joinToString(" and ")}")
    }

    @And("I say that the conditions are to be joined by {word}")
    fun sayConditionsAreToBeJoinedBy(connector: String) {
        sendSearchRequest("the conditions are to be joined with \"$connector\"")
    }

    @And("I say that at most {int} cases are to be found")
    fun sayAtMostCasesAreToBeFound(count: Int) {
        sendSearchRequest("at most $count cases are to be found")
    }

    @And("I say that the following case lists are to be searched:")
    fun sayTheFollowingCaseListsAreToBeSearched(lists: DataTable) {
        val names = lists.asLists().map { it[0].trim() }
        sendSearchRequest("the ${names.joinToString(" and ")} case lists are to be searched")
    }

    @And("I say that the cases found are to go in the {word} case list")
    fun sayTheCasesFoundAreToGoInTheCaseList(listName: String) {
        sendSearchRequest("the cases found are to go in the $listName case list")
    }

    @And("I confirm the search")
    fun confirmTheSearch() {
        // The chatbot shows a summary of the search parameters and asks for
        // confirmation; wait for that before agreeing.
        ChatDefs().waitForBotRequestForConfirmationAndConfirm()
    }

    @And("I abandon the search setup")
    fun abandonTheSearchSetup() {
        sendSearchRequest("let's abandon the search, I don't want to search after all")
    }

    @And("I ask to stop the search")
    fun askToStopTheSearch() {
        sendSearchRequest("please stop the search")
    }

    @When("I search the {string} case list(s) for cases where {string} and put at most {int} results in the {word} case list")
    fun searchForCasesWithLimit(lists: String, conditions: String, limit: Int, destination: String) {
        sendSearchRequest(
            "please search the $lists case lists for cases where \"$conditions\", " +
                "find at most $limit of them, and put the results in the $destination case list"
        )
    }

    @When("I search the {string} case list(s) for all cases where {string} and put the results in the {word} case list")
    fun searchForAllCases(lists: String, conditions: String, destination: String) {
        sendSearchRequest(
            "please search the $lists case lists for all cases where \"$conditions\" " +
                "and put the results in the $destination case list"
        )
    }

    @Then("the user-defined {word} case list should contain the following cases in any order:")
    fun theUserDefinedCaseListShouldContainTheFollowingCasesInAnyOrder(listName: String, dataTable: DataTable) {
        userDefinedCaseListPO(listName).requireCaseNamesInAnyOrder(dataTable.asList())
    }

    @Then("the user-defined {word} case count should be {int}")
    fun theUserDefinedCaseCountShouldBe(listName: String, count: Int) {
        userDefinedCaseListCaseCountPO(listName).waitForCountOfNumberOfCasesToBe(count)
    }

    @Then("the {word} case list is not shown")
    fun theCaseListIsNotShown(listName: String) {
        userDefinedCaseListCaseCountPO(listName).requireCaseCountToBeHidden()
        userDefinedCaseListCaseCountPO(listName).requireCasesLabelToBeHidden()
    }

    // The search request is handled by a real LLM call, which completes
    // asynchronously. enterChatTextAndSend only submits the message, so we
    // must wait for the chatbot's reply - the next prompt, the summary asking
    // for confirmation, or a refusal - before the scenario continues.
    private fun sendSearchRequest(text: String) {
        val countBefore = chatPO().numberOfChatMessages()
        ChatDefs().enterChatTextAndSend(text)
        await().atMost(ofSeconds(90)).until {
            chatPO().numberOfChatMessages() > countBefore
        }
    }
}
