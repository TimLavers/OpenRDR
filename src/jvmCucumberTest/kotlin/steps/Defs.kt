package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.*
import io.rippledown.integration.restclient.RESTClient
import org.awaitility.Awaitility
import org.openqa.selenium.WebDriver
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit

class Defs : En {
    private val uiTestBase = UITestBase()
    private val serverProxy = uiTestBase.serverProxy
    private val labProxy = uiTestBase.labProxy

    private lateinit var caseListPO: CaseListPO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var cornerstoneViewPO: CornerstoneCaseViewPO
    private lateinit var interpretationViewPO: InterpretationViewPO
    private lateinit var conditionSelectorPO: ConditionSelectorPO
    private lateinit var conclusionsViewPO: ConclusionsViewPO
    private lateinit var kbInfoPO: KBInfoPO

    private lateinit var driver: WebDriver

    init {
        Before { scenario ->
            println("Before scenario '${scenario.name}'")
            serverProxy.start()
        }

        After { scenario ->
            println("After scenario '${scenario.name}'")
            uiTestBase.driverClose()
            serverProxy.shutdown()
        }

        When("I start the client application") {
            driver = uiTestBase.setupWebDriver()
            caseListPO = CaseListPO(driver)
            caseViewPO = CaseViewPO(driver)
            cornerstoneViewPO = CornerstoneCaseViewPO(driver)
            interpretationViewPO = InterpretationViewPO(driver)
            conditionSelectorPO = ConditionSelectorPO(driver)
            conclusionsViewPO = ConclusionsViewPO(driver)
            kbInfoPO = KBInfoPO(driver)
        }

        When("stop the client application") {
            //client application is stopped in the After hook
        }

        Given("a list of cases with the following names is stored on the server:") { dataTable: DataTable ->
            dataTable.asList().forEach { caseName ->
                labProxy.provideCase(caseName)
            }
        }
        Given("a list of {long} cases is stored on the server") { numberOfCases: Long ->
            (1..numberOfCases).forEach { i ->
                val padded = i.toString().padStart(3, '0')
                labProxy.provideCaseWithName("Case_$padded")
            }
        }

        And("I select a case with all three attributes") {
            caseViewPO = caseListPO.select("CaseABC")
        }

        When("a new case with the name {word} is stored on the server") { caseName: String ->
            labProxy.provideCase(caseName)
        }

        Given("the following cases are deleted on the server:") { dataTable: DataTable ->
            dataTable.asList().forEach { caseName ->
                labProxy.restProxy.deleteProcessedCaseWithName(caseName)
            }
        }

        When("the case with the name {word} is deleted on the server") { caseName: String ->
            labProxy.restProxy.deleteProcessedCaseWithName(caseName)
        }

        And("I select case {word}") { caseName: String ->
            caseViewPO = caseListPO.select(caseName)
        }

        And("I move attribute {word} below attribute {word}") { moved: String, target: String ->
            caseViewPO.dragAttribute(moved, target)
            Thread.sleep(1000)
        }

        Given("the initial Attribute order is A, B, C") {
            labProxy.provideCase("Case1", mapOf("A" to "a"))
            labProxy.provideCase("Case2", mapOf("A" to "a", "B" to "b"))
            labProxy.provideCase("Case3", mapOf("A" to "a", "B" to "b", "C" to "c"))
            // The attributes are created when the cases are parsed, so select them in the right order.
            caseListPO.select("Case1")
            caseListPO.select("Case2")
            caseListPO.select("Case3")
        }

        Given("I import the configured zipped Knowledge Base {word}") { toImport: String ->
            kbInfoPO.importKB(toImport)
            kbInfoPO.waitForKBToBeLoaded(toImport)
        }

        And("I export the current Knowledge Base") {
            kbInfoPO.exportKB()
        }

        Then("there is a file called {word} in my downloads directory") { fileName: String ->
            Awaitility.await().atMost(Duration.ofSeconds(5)).until {
                File(uiTestBase.downloadsDir(), fileName).exists()
            }
        }

        Given("I import the exported Knowledge Base {word}") { kbName: String ->
            val exportedZip = File(uiTestBase.downloadsDir(), "$kbName.zip")
            val kbInfoPO = KBInfoPO(driver)
            kbInfoPO.importFromZip(exportedZip)
            kbInfoPO.waitForKBToBeLoaded(kbName)
        }

        Given("case {word} is provided having data:") { caseName: String, dataTable: DataTable ->
            val attributeNameToValue = mutableMapOf<String, String>()
            dataTable.asMap().forEach { (t, u) -> attributeNameToValue[t] = u }
            labProxy.provideCase(caseName, attributeNameToValue)
        }

        Then("^the case (should show|shows) the attributes in order:$") { _: String, dataTable: DataTable ->
            caseViewPO.attributes() shouldBe dataTable.asList()
        }

        Then("the displayed KB name is (now ){word}") { kbName: String ->
            KBInfoPO(driver).headingText() shouldBe kbName
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
            caseListPO.waitForCaseListToHaveSize(expectedCaseNames.size)
            val actualCaseNames = caseListPO.casesListed()
            actualCaseNames shouldBe expectedCaseNames
        }

        Then("I should see no cases in the case list") {
            caseListPO.waitForNoCases()
        }

        Then("I (should )see the case {word} as the current case") { caseName: String ->
            caseViewPO.nameShown() shouldBe caseName
        }
        Then("I should not see any current case") {
            caseViewPO.noNameShowing() shouldBe true
        }

        And("(I )select the case {word}") { caseName: String ->
            caseListPO.select(caseName)
        }

        When("I delete all the text in the interpretation field") {
            interpretationViewPO.deleteAllText()
        }

        And("I enter the text {string} in the interpretation field") { text: String ->
            interpretationViewPO.enterVerifiedText(text)
        }

        When("I replace the text in the interpretation field with {string}") { text: String ->
            interpretationViewPO.deleteAllText()
            interpretationViewPO.enterVerifiedText(text)
        }
        Then("the interpretation field should contain the text {string}") { text: String ->
            interpretationViewPO.selectOriginalTab()
            interpretationViewPO.interpretationText() shouldBe text
        }
        Then("the interpretation field should be empty") {
            interpretationViewPO.interpretationText() shouldBe ""
        }

        And("the interpretation of the case {word} is {string}") { caseName: String, text: String ->
            RESTClient().createRuleToAddText(caseName, text)
        }

        And("the interpretation of the case {word} includes {string} because of condition {string}") { caseName: String, text: String, conditionText: String ->
            RESTClient().createRuleToAddText(caseName, text, conditionText)
        }
        And("the following rules have been defined:") { dataTable: DataTable ->
            dataTable.cells()
                .drop(1) // Drop the header row
                .forEach { row ->
                    RESTClient().createRuleToAddText(row[0], row[1], row[2])
                }
        }

        And("I select the {word} tab") { tabName: String ->
            when (tabName) {
                "interpretation" -> interpretationViewPO.selectOriginalTab()
                "conclusions" -> interpretationViewPO.selectConclusionsTab()
                "changes" -> interpretationViewPO.selectChangesTab()
                else -> throw IllegalArgumentException("Unknown tab name: $tabName")
            }
        }

        Then("I should see that the text {string} has been added") { text: String ->
            interpretationViewPO.requireAddedText(text)
        }

        Then("I should see that the text {string} has been deleted") { text: String ->
            interpretationViewPO.requireDeletedText(text)
        }

        Then("I should see that the text {string} has been replaced by {string}") { replaced: String, replacement: String ->
            interpretationViewPO.requireReplacedText(replaced, replacement)
        }
        And("the changes badge indicates that there is/are {int} change(s)") { numberOfChanges: Int ->
            interpretationViewPO.requireBadgeCount(numberOfChanges)
        }
        And("the changes badge indicates that there is no change") {
            interpretationViewPO.requireNoBadge()
        }
        When("I build a rule for the change on row {int}") { row: Int ->
            interpretationViewPO.buildRule(row)
            conditionSelectorPO.clickDone()
        }
        When("I complete the rule") {
            conditionSelectorPO.clickDone()
        }

        When("I start to build a rule for the change on row {int}") { row: Int ->
            interpretationViewPO.buildRule(row)
        }
        When("I select the condition in position {int}") { index: Int ->
            conditionSelectorPO.clickConditionWithIndex(index)
        }
        When("I select the condition {string}") { text: String ->
            conditionSelectorPO.clickConditionWithText(text)
        }

        When("I select the {word} condition") { position: String ->
            when (position) {
                "first" -> conditionSelectorPO.clickConditionWithIndex(0)
                "second" -> conditionSelectorPO.clickConditionWithIndex(1)
                "third" -> conditionSelectorPO.clickConditionWithIndex(2)
            }
        }
        Then("the conditions showing should be:") { dataTable: DataTable ->
            val expectedConditions = dataTable.asList()
            conditionSelectorPO.requireConditionsShowing(expectedConditions)
        }

        Then("the following conditions (are )(should be )selected:") { dataTable: DataTable ->
            val expectedConditions = dataTable.asList()
            conditionSelectorPO.requireConditionsToBeSelected(expectedConditions)
        }

        And("I build a rule to add the comment {string} with the condition {string}") { comment: String, condition: String ->
            with(interpretationViewPO) {
                enterVerifiedText(comment)
                selectChangesTab()
                buildRule(0)
            }
            with(conditionSelectorPO) {
                clickConditionWithText(condition)
                clickDone()
            }
        }

        And("I build a rule to replace the interpretation by {string} with the condition {string}") { replacement: String, condition: String ->
            with(interpretationViewPO) {
                deleteAllText()
                enterVerifiedText(replacement)
                selectChangesTab()
                buildRule(0)
            }
            with(conditionSelectorPO) {
                clickConditionWithText(condition)
                clickDone()
            }
        }

        And("click the comment {string}") { comment: String ->
            conclusionsViewPO.clickComment(comment)
        }

        Then("the conditions showing are:") { dataTable: DataTable ->
            val expectedConditions = dataTable.asList()
            conclusionsViewPO.requireConditionsToBeShown(*expectedConditions.toTypedArray())
        }

        Then("the message {string} should be shown") { message: String ->
            cornerstoneViewPO.requireMessageForNoCornerstones(message)
        }

        Then("the case {word} is (still )shown as the cornerstone case") { ccName: String ->
            cornerstoneViewPO.requireCornerstoneCase(ccName)
        }

        Then("the number of cornerstone cases should be shown as {int}") { numberOfCornerstoneCases: Int ->
            cornerstoneViewPO.requireNumberOfCornerstones(numberOfCornerstoneCases)
        }
        When("I click the {word} cornerstone case button") { direction: String ->
            when (direction) {
                "previous" -> cornerstoneViewPO.selectPreviousCornerstoneCase()
                "next" -> cornerstoneViewPO.selectNextCornerstoneCase()
            }
        }
    }
}
