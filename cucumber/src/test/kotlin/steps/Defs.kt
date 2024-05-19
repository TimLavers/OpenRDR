package steps

import io.cucumber.datatable.DataTable
import io.cucumber.docstring.DocString
import io.cucumber.java8.En
import io.kotest.matchers.shouldBe
import io.rippledown.integration.pause
import io.rippledown.integration.proxy.ConfiguredTestData
import io.rippledown.integration.utils.Cyborg
import io.rippledown.integration.waitUntilAssertedOnEventThread
import org.awaitility.Awaitility
import steps.StepsInfrastructure.cleanup
import steps.StepsInfrastructure.reStartWithPostgres
import steps.StepsInfrastructure.startClient
import steps.StepsInfrastructure.startServerWithInMemoryDatabase
import steps.StepsInfrastructure.startServerWithPostgresDatabase
import steps.StepsInfrastructure.stopServer
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

class Defs : En {
    private var exportedZip: File? = null
    init {
        println("--------------- Defs init!!!!!!!!!!!!!!!!!!!!!!!----------------------")
        Before("not @database") { scenario ->
            println("Before scenario '${scenario.name}'")
            startServerWithInMemoryDatabase()
        }

        Before("@database") { scenario ->
            println("DB Before. Scenario: '${scenario.name}'")
            startServerWithPostgresDatabase()
        }

        After { scenario ->
            println("After scenario '${scenario.name}'")
            cleanup()
        }

        When("A Knowledge Base called {word} has been created") { name: String ->
            restClient().createKB(name)
        }

        When("I start the client application") {
            startClient()
        }

        When("stop the client application") {
            //client application is stopped in the After hook
        }

        When("I stop the client application") {
        }

        Given("a list of cases with the following names is stored on the server:") { dataTable: DataTable ->
            dataTable.asList().forEach { caseName ->
                labProxy().provideCase(caseName)
            }
        }
        Given("a list of {long} cases is stored on the server") { numberOfCases: Long ->
            (1..numberOfCases).forEach { i ->
                val padded = i.toString().padStart(3, '0')
                labProxy().provideCaseWithName("Case_$padded")
            }
        }

        And("I re-start the server application") {
            stopServer()
            pause(3000)
            reStartWithPostgres()
        }

        And("I select a case with all three attributes") {
            caseListPO().select("CaseABC")
        }

        When("a new case with the name {word} is stored on the server") { caseName: String ->
            labProxy().provideCase(caseName)
        }

        Given("the configured case {word} is stored on the server") { caseName: String ->
            labProxy().provideCase(caseName)
        }

        Given("the following cases are deleted on the server:") { dataTable: DataTable ->
            dataTable.asList().forEach { caseName ->
                labProxy().restProxy.deleteProcessedCaseWithName(caseName)
            }
        }

        When("the case with the name {word} is deleted on the server") { caseName: String ->
            labProxy().restProxy.deleteProcessedCaseWithName(caseName)
        }

        And("I select case {word}") { caseName: String ->
            caseListPO().waitForCaseListToContain(caseName)
            caseListPO().select(caseName)
        }

        And("I move attribute {word} below attribute {word}") { moved: String, target: String ->
            caseViewPO().dragAttribute(moved, target)
            Thread.sleep(1000)
        }

        Given("the initial Attribute order is A, B, C") {
            labProxy().provideCase("Case1", mapOf("A" to "a"))
            labProxy().provideCase("Case2", mapOf("A" to "a", "B" to "b"))
            labProxy().provideCase("Case3", mapOf("A" to "a", "B" to "b", "C" to "c"))
            // The attributes are created when the cases are parsed, so select them in the right order.
            with(caseListPO()) {
                waitForCountOfNumberOfCasesToBe(3)

                // The attributes are created when the cases are parsed, so select them in the right order.
                select("Case1")
                select("Case2")
                select("Case3")
            }
        }

        Given("I import the configured zipped Knowledge Base {word}") { toImport: String ->
            val zipFile = ConfiguredTestData.kbZipFile(toImport)
            val kbControlOperator = applicationBarPO().kbControlOperator()
            pause(10_000)
            kbControlOperator.importKB(zipFile.absolutePath)
        }

        And("I export the current Knowledge Base") {
            exportedZip = File.createTempFile("Exported", ".zip")
            val kbControlOperator = applicationBarPO().kbControlOperator()
            kbControlOperator.exportKB(exportedZip!!.absolutePath)
        }

        Then("there is a file called {word} in my downloads directory") { fileName: String ->
            Awaitility.await().atMost(Duration.ofSeconds(5)).until {
                File(StepsInfrastructure.uiTestBase.downloadsDir(), fileName).exists()
            }
        }

        Given("I import the previously exported Knowledge Base") {
            require(exportedZip != null) {
                "Import of previously exported KB attempted but exported KB is null."
            }
            val kbControlOperator = applicationBarPO().kbControlOperator()
            kbControlOperator.importKB(exportedZip!!.absolutePath)
        }

        Given("case {word} is provided having data:") { caseName: String, dataTable: DataTable ->
            val attributeNameToValue = mutableMapOf<String, String>()
            dataTable.asMap().forEach { (t, u) -> attributeNameToValue[t] = u }
            labProxy().provideCase(caseName, attributeNameToValue)
        }

        Then("the displayed KB name is (now ){word}") { kbName: String ->
            waitUntilAssertedOnEventThread {
                applicationBarPO().kbControlOperator().currentKB() shouldBe kbName
            }
        }

        Then("I activate the KB management control") {
            applicationBarPO().kbControlOperator().expandDropdownMenu()
        }

        Then("I (should )see this list of available KBs:") { dataTable: DataTable ->
            val expectedKBs = dataTable.asList()
            applicationBarPO()
                .kbControlOperator()
                .availableKBs() shouldBe expectedKBs
        }

        Then("the displayed product name is 'Open RippleDown'") {
            applicationBarPO().title() shouldBe "Open RippleDown"
        }

        Then("I create a Knowledge Base with the name {word}") { kbName: String ->
            val kbControlOperator = applicationBarPO().kbControlOperator()
            kbControlOperator.createKB(kbName)
        }

        Then("I select the Knowledge Base named {word}") { kbName: String ->
            val kbControlOperator = applicationBarPO().kbControlOperator()
            kbControlOperator.selectKB(kbName)
        }

        And("pause for {long} second(s)") { seconds: Long ->
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds))
        }

        And("pause") {
            Thread.sleep(TimeUnit.DAYS.toMillis(1L))
        }

        And("pause briefly") {
            Thread.sleep(TimeUnit.SECONDS.toMillis(20L))
        }

        Then("I (should )see the following cases in the case list:") { dataTable: DataTable ->
            val expectedCaseNames = dataTable.asList()
            caseListPO().waitForCountOfNumberOfCasesToBe(expectedCaseNames.size)
            caseListPO().requireCaseNamesToBe(expectedCaseNames)
        }

        Then("I should see no cases in the case list") {
            caseListPO().waitForNoCases()
        }

        Then("I (should )see the case {word} as the current case") { caseName: String ->
            caseViewPO().waitForNameToShow(caseName)
        }
        Then("I should not see any current case") {
            caseViewPO().requireNoNameShowing()
        }

        Then("Eventually I should not see any cases") {
            caseViewPO().waitForNoNameShowing()
        }

        And("(I )select the case {word}") { caseName: String ->
            caseListPO().select(caseName)
        }
        And("(I )select the case {word} followed by {word}") { caseName1: String, caseName2: String ->
            caseListPO().select(caseName1)
            caseListPO().select(caseName2)
        }

        When("I delete all the text in the interpretation field") {
            interpretationViewPO().deleteAllText()
        }

        And("I enter the text {string} in the interpretation field") { text: String ->
            interpretationViewPO().setVerifiedText(text)
        }

        And("I add the text {string} at the end of the current interpretation") { text: String ->
            interpretationViewPO().addVerifiedTextAtEndOfCurrentInterpretation(text)
        }

        And("I slowly type the text {string} in the interpretation field") { text: String ->
            with(Cyborg()) {
                typeSlowly(text)
            }
        }

        And("I build a rule to add the conclusion {string} with no conditions") { text: String ->
//            interpretationViewPO.enterVerifiedText(text)
        }

        When("I replace the text in the interpretation field with {string}") { text: String ->
//            interpretationViewPO.deleteAllText()
//            interpretationViewPO.enterVerifiedText(text)
        }
        Then("the interpretation field should contain the text {string}") { text: String ->
            interpretationViewPO().waitForInterpretationTextToContain(text)
        }
        Then("the interpretation should be {string}") { text: String ->
            interpretationViewPO().waitForInterpretationText(text)
        }
        Then("the interpretation should be this:") { text: DocString ->
            interpretationViewPO().waitForInterpretationText(text.content)
        }

        Then("the interpretation field should be empty") {
            interpretationViewPO().interpretationText() shouldBe ""
        }

        And("the interpretation of the case {word} is {string}") { caseName: String, text: String ->
            restClient().createRuleToAddText(caseName, text)
        }

        And("the interpretation of the case {word} includes {string} because of condition {string}") { caseName: String, text: String, conditionText: String ->
            restClient().createRuleToAddText(caseName, text, conditionText)
        }
        And("the following rules have been defined:") { dataTable: DataTable ->
            dataTable.cells()
                .drop(1) // Drop the header row
                .forEach { row ->
                    restClient().createRuleToAddText(row[0], row[1], row[2])
                }
        }

        And("I select the {word} tab") { tabName: String ->
            when (tabName) {
                "interpretation" -> interpretationViewPO().selectOriginalTab()
                "conclusions" -> interpretationViewPO().selectConclusionsTab()
                "changes" -> interpretationViewPO().selectDifferencesTab()
                else -> throw IllegalArgumentException("Unknown tab name: $tabName")
            }
        }

        And("the changes badge indicates that there is/are {int} change(s)") { numberOfChanges: Int ->
            interpretationViewPO().requireBadgeCount(numberOfChanges)
        }
        And("the changes badge indicates that there is no change") {
//            interpretationViewPO.requireNoBadge()
        }
        When("I build a rule for the change on row {int}") { row: Int ->
//            interpretationViewPO.buildRule(row)
//            conditionSelectorPO.clickDone()
        }
        When("I complete the rule") {
//            conditionSelectorPO.clickDone()
        }
        When("(I )cancel the rule") {
//            conditionSelectorPO.clickCancel()
        }

        When("I start to build a rule for the change on row {int}") { row: Int ->
//            interpretationViewPO.buildRule(row)
        }
        When("I select the condition in position {int}") { index: Int ->
//            conditionSelectorPO.clickConditionWithIndex(index)
        }
        When("I select the condition {string}") { text: String ->
//            conditionSelectorPO.clickConditionWithText(text)
        }

        When("I select the {word} condition") { position: String ->
            when (position) {
//                "first" -> conditionSelectorPO.clickConditionWithIndex(0)
//                "second" -> conditionSelectorPO.clickConditionWithIndex(1)
//                "third" -> conditionSelectorPO.clickConditionWithIndex(2)
            }
        }
        Then("the conditions showing should be:") { dataTable: DataTable ->
            val expectedConditions = dataTable.asList()
//            conditionSelectorPO.requireConditionsShowing(expectedConditions)
        }

        Then("the following conditions (are )(should be )selected:") { dataTable: DataTable ->
            val expectedConditions = dataTable.asList()
//            conditionSelectorPO.requireConditionsToBeSelected(expectedConditions)
        }

        And("I build a rule to add the comment {string} with the condition {string}") { comment: String, condition: String ->
//            with(interpretationViewPO) {
//                enterVerifiedText(comment)
//                selectChangesTab()
//                buildRule(0)
//            }
//            with(conditionSelectorPO) {
//                clickConditionWithText(condition)
//                clickDone()
//            }
        }
        And("I build a rule to add the comment {string}") { comment: String ->
//            with(interpretationViewPO) {
//                enterVerifiedText(comment)
//                selectChangesTab()
//                buildRule(0)
//            }
//            with(conditionSelectorPO) {
//                clickDone()
//            }
        }

        And("I build another rule to append the comment {string}") { comment: String ->
//            with(interpretationViewPO) {
//                appendVerifiedText(" $comment")
//                selectChangesTab()
//                buildRule(row = 1)//The first row has the unchanged comment
//            }
//            with(conditionSelectorPO) {
//                clickDone()
//            }
        }

        And("I build a rule to replace the interpretation by {string} with the condition {string}") { replacement: String, condition: String ->
//            with(interpretationViewPO) {
//                deleteAllText()
//                enterVerifiedText(replacement)
//                selectChangesTab()
//                buildRule(0)
//            }
//            with(conditionSelectorPO) {
//                clickConditionWithText(condition)
//                clickDone()
//            }
        }

        And("click the comment {string}") { comment: String ->
//            conclusionsViewPO.clickComment(comment)
        }

        Then("the conditions showing are:") { dataTable: DataTable ->
            val expectedConditions = dataTable.asList()
//            conclusionsViewPO.requireConditionsToBeShown(*expectedConditions.toTypedArray())
        }

        Then("the message {string} should be shown") { message: String ->
//            cornerstoneViewPO.requireMessageForNoCornerstones(message)
        }

        Then("the case {word} is (still )shown as the cornerstone case") { ccName: String ->
//            cornerstoneViewPO.requireCornerstoneCase(ccName)
        }

        Then("the number of cornerstone cases should be shown as {int}") { numberOfCornerstoneCases: Int ->
//            cornerstoneViewPO.requireNumberOfCornerstones(numberOfCornerstoneCases)
        }
        When("I click the {word} cornerstone case button") { direction: String ->
            when (direction) {
//                "previous" -> cornerstoneViewPO.selectPreviousCornerstoneCase()
//                "next" -> cornerstoneViewPO.selectNextCornerstoneCase()
            }
        }

        Then("the KB controls (are )(should be )disabled") {
//            kbControlsPO.requireKbControlsToBeDisabled()
        }

        Then("the KB controls (are )(should be )enabled") {
//            kbControlsPO.requireKbControlsToBeEnabled()
        }

        And("the count of the number of cases should be hidden") {
            caseListPO().requireCaseCountToBeHidden()
        }

        And("the count of the number of cases is {int}") { numberOfCases: Int ->
            caseListPO().waitForCountOfNumberOfCasesToBe(numberOfCases)
        }
    }
}
