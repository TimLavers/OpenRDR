package steps

import com.google.common.base.Stopwatch
import io.cucumber.datatable.DataTable
import io.cucumber.docstring.DocString
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe
import io.rippledown.integration.pause
import io.rippledown.integration.proxy.ConfiguredTestData
import io.rippledown.integration.proxy.TestResultDetail
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
import java.util.concurrent.TimeUnit.SECONDS

class Defs {
    private var exportedZip: File? = null
    private lateinit var stopwatch: Stopwatch

    @Before("not @database")
    fun before(scenario: Scenario) {
        println("\nBefore scenario '${scenario.name}'")
        stopwatch = Stopwatch.createStarted()
        startServerWithInMemoryDatabase()
    }

    @Before("@database")
    fun beforeWithDatabase(scenario: Scenario) {
        println("\nDB Before. Scenario: '${scenario.name}'")
        startServerWithPostgresDatabase()
    }

    @After
    fun after(scenario: Scenario) {
        stopwatch.stop()
        cleanup()
        println("After scenario  '${scenario.name}', duration: ${stopwatch.elapsed(SECONDS)} seconds")
    }

    @After("@chat")
    fun afterChatScenario(scenario: Scenario) {
        println("Delaying for 30 secs after chat scenario")
        Thread.sleep(30_000)
    }

    @When("A Knowledge Base called {word} has been created")
    fun createKnowledgeBase(name: String) {
        restClient().createKB(name)
    }

    @When("I start the client application")
    fun startClientApplication() = startClient()

    @When("(I )stop the client application")
    fun stopTheClientApplication() {
        //client application is stopped in the After hook
    }

    @Given("a list of cases with the following names is stored on the server:")
    fun aListOfCasesWithTheFollowingNamesIsStoredOnTheServer(dataTable: DataTable) {
        dataTable.asList().forEach { caseName ->
            labProxy().provideCase(caseName)
        }
    }

    @Given("a case with name {word} is stored on the server")
    fun aCaseWithNameIsStoredOnTheServer(caseName: String) {
        labProxy().provideCase(caseName)
    }

    @Given("a list of {long} cases is stored on the server")
    fun aListOfLongCasesIsStoredOnTheServer(numberOfCases: Long) {
        (1..numberOfCases).forEach { i ->
            val padded = i.toString().padStart(3, '0')
            labProxy().provideCaseWithName("Case_$padded")
        }
    }

    @And("I re-start the server application")
    fun restartTheServerApplication() {
        stopServer()
        pause(3000)
        reStartWithPostgres()
    }

    @And("I select a case with all three attributes")
    fun selectACaseWithAllThreeAttributes() {
        caseListPO().select("CaseABC")
    }

    @When("a new case with the name {word} is stored on the server")
    fun aNewCaseIsStoredOnTheServer(caseName: String) {
        labProxy().provideCase(caseName)
    }

    @When("a new case is stored on the server")
    fun caseWithDefaultNameIsStoredOnTheServer() {
        labProxy().provideCase("Case1")
    }

    @Given("the configured case {word} is stored on the server")
    fun theConfiguredCaseIsStoredOnTheServer(caseName: String) {
        labProxy().provideCase(caseName)
    }

    @Given("the following cases are deleted on the server:")
    fun theFollowingCasesAreDeletedOnTheServer(dataTable: DataTable) {
        dataTable.asList().forEach { caseName ->
            labProxy().restProxy.deleteProcessedCaseWithName(caseName)
        }
    }

    @When("the case with the name {word} is deleted on the server")
    fun theCaseWithTheNameIsDeletedOnTheServer(caseName: String) {
        labProxy().restProxy.deleteProcessedCaseWithName(caseName)
    }

    @And("I select case {word}")
    fun selectCase(caseName: String) {
        caseListPO().select(caseName)
    }

    @And("I move attribute {word} below attribute {word}")
    fun moveAttributeBelowAttribute(moved: String, target: String) {
        caseViewPO().dragAttribute(moved, target)
        Thread.sleep(1000)
    }

    @Given("the initial Attribute order is A, B, C")
    fun theInitialAttributeOrderIsABC() {
        labProxy().provideCase("Case1", mapOf("A" to "a"))
        labProxy().provideCase("Case2", mapOf("A" to "a", "B" to "b"))
        labProxy().provideCase("Case3", mapOf("A" to "a", "B" to "b", "C" to "c"))

        // The attributes are created when the cases are parsed, so select them in the right order.
        caseCountPO().waitForCountOfNumberOfCasesToBe(3)
    }

    @Given("I import the configured zipped Knowledge Base {word}")
    fun importConfiguredZippedKnowledgeBase(toImport: String) {
        val zipFile = ConfiguredTestData.kbZipFile(toImport)
        kbControlsPO().importKB(zipFile.absolutePath)
    }

    @And("I export the current Knowledge Base")
    fun exportTheCurrentKnowledgeBase() {
        exportedZip = File.createTempFile("Exported", ".zip")
        kbControlsPO().exportKB(exportedZip!!.absolutePath)
    }

    @Then("there is a file called {word} in my downloads directory")
    fun requireFileInMyDownloadsDirectory(fileName: String) {
        Awaitility.await().atMost(Duration.ofSeconds(5)).until {
            File(StepsInfrastructure.uiTestBase.downloadsDir(), fileName).exists()
        }
    }

    @Given("I import the previously exported Knowledge Base")
    fun importThePreviouslyExportedKnowledgeBase() {
        require(exportedZip != null) {
            "Import of previously exported KB attempted but exported KB is null."
        }
        kbControlsPO().importKB(exportedZip!!.absolutePath)
    }

    @Given("case {word} is provided having data:")
    fun provideCaseWithData(caseName: String, dataTable: DataTable) {
        val attributeNameToValue = mutableMapOf<String, String>()
        dataTable.asMap().forEach { (t, u) -> attributeNameToValue[t] = u }
        labProxy().provideCase(caseName, attributeNameToValue)
    }

    @Given("case {word} for KB {word} is provided having data:")
    fun provideCaseWithDataForKB(caseName: String, kbName: String, dataTable: DataTable) {
        val attributeNameToValue = mutableMapOf<String, String>()
        dataTable.asMap().forEach { (t, u) -> attributeNameToValue[t] = u }
        labProxy().provideCaseForKb(kbName, caseName, attributeNameToValue)
    }

    @Given("case {word} for KB {word} gets the interpretation {string} when it is provided having data:")
    fun provideCaseWithDataForKbAndCheckInterpretation(caseName: String, kbName: String, comment: String,  dataTable: DataTable) {
        val attributeNameToValue = mutableMapOf<String, String>()
        dataTable.asMap().forEach { (t, u) -> attributeNameToValue[t] = u }
        val received = labProxy().provideCaseForKb(kbName, caseName, attributeNameToValue)
        received.interpretation.conclusionTexts() shouldBe setOf(comment)
    }

    @Given("case {word} is provided with the following values, reference ranges and units:")
    fun provideCaseWithDataIncludingReferenceRangesAndUnits(caseName: String, dataTable: DataTable) {
        //| attribute | value | lower reference range| upper reference range | units |
        val details = dataTable.cells()
            .drop(1) // Drop the header row
            .map {
                TestResultDetail(it[0], it[1], it[2], it[3], it[4])
            }
        labProxy().provideCase(caseName, details)
    }

    @Then("the displayed product name is 'Open RippleDown'")
    fun requireProductNameIsOpenRippleDown() {
        applicationBarPO().title() shouldBe "Open RippleDown"
    }

    @And("pause for {long} second(s)")
    fun pauseSeconds(seconds: Long) {
        Thread.sleep(SECONDS.toMillis(seconds))
    }

    @And("pause")
    fun pause() {
        Thread.sleep(TimeUnit.DAYS.toMillis(1L))
    }

    @And("pause briefly")
    fun pauseBriefly() {
        Thread.sleep(SECONDS.toMillis(20L))
    }

    @Then("I (should )see the following cases in the case list:")
    fun IShouldSeeTheFollowingCasesInTheCaseList(dataTable: DataTable) {
        val expectedCaseNames = dataTable.asList()
        caseCountPO().waitForCountOfNumberOfCasesToBe(expectedCaseNames.size)
        caseListPO().requireCaseNamesToBe(expectedCaseNames)
    }

    @Then("I should see no cases in the case list")
    fun IShouldSeeNoCasesInTheCaseList() {
        caseCountPO().requireCaseCountToBeHidden()
    }

    @Then("I (should )see the case {word} as the current case")
    fun IShouldSeeTheCaseWordAsTheCurrentCase(caseName: String) {
        caseViewPO().waitForNameToShow(caseName)
    }

    @Then("I should not see any current case")
    fun IShouldNotSeeAnyCurrentCase() {
        caseViewPO().requireNoNameShowing()
    }

    @Then("Eventually I should not see any cases")
    fun EventuallyIShouldNotSeeAnyCases() {
        caseViewPO().waitForNoNameShowing()
    }

    @And("(I )select the case {word}")
    fun ISelectTheCaseWord(caseName: String) {
        caseListPO().select(caseName)
    }

    @And("(I )select the case {word} followed by {word}")
    fun ISelectTheCaseWordFollowedByWord(caseName1: String, caseName2: String) {
        caseListPO().select(caseName1)
        caseListPO().select(caseName2)
    }

    @Then("the interpretation should contain the text {string}")
    fun theInterpretationFieldShouldContainTheText(text: String) {
        interpretationViewPO().waitForInterpretationTextToContain(text)
    }

    @Then("the (interpretation )(report )(should be )(is ){string}")
    fun theInterpretationShouldBeString(text: String) {
        interpretationViewPO().waitForInterpretationText(text)
    }

    @Then("the interpretation should be this:")
    fun theInterpretationShouldBeThis(text: DocString) {
        interpretationViewPO().waitForInterpretationText(text.content)
    }

    @Then("the (interpretation )(report )(should be )(is )(empty)(blank)")
    fun theInterpretationFieldShouldBeEmpty() {
        interpretationViewPO().waitForInterpretationText("")
    }

    @And("a rule exists to add the comment {string} to case {word} for the following conditions:")
    fun `add rule to give comment for conditions`(commentText: String, caseName: String, dataTable: DataTable) {
        val conditions = dataTable.asList()
        restClient().createRuleToAddText(caseName, commentText, *conditions.toTypedArray())
    }

    @And("a rule exists to add the comment {string} to case {word} with no conditions")
    fun `add rule to give comment with no conditions`(commentText: String, caseName: String) {
        restClient().createRuleToAddText(caseName, commentText)
    }

    @And("the interpretation of the case {word} is {string}")
    fun theInterpretationOfTheCaseWordIsString(caseName: String, text: String) {
        restClient().createRuleToAddText(caseName, text)
    }

    @Then("the cases should have interpretations as follows:")
    fun requireInterpretations(dataTable: DataTable) {
        dataTable.cells().forEach { row ->
            val case = row[0]
            val expectedInterpretation = row[1] ?: ""
            caseListPO().select(case)
            caseViewPO().waitForNameToShow(case)
            interpretationViewPO().waitForInterpretationText(expectedInterpretation)
        }
    }

    @Given("cases are set up as follows:")
    fun casesAreSetUpAsFollows(dataTable: DataTable) {
        dataTable.cells()
            .drop(1) // Drop the header row
            .forEach { row ->
                val caseName = row[0]
                val attributeName = row[1]
                val attributeValue = row[2]
                val comment = row[3]
                val conditionText = row[4]
                labProxy().provideCase(caseName, mapOf(attributeName to attributeValue))
                if (comment != null) createRuleToAddComment(caseName, comment, conditionText)
            }
    }

    @And("the interpretation of the case {word} includes {string} because of condition {string}")
    fun createRuleToAddComment(
        caseName: String,
        text: String,
        conditionText: String
    ) {
        restClient().createRuleToAddText(caseName, text, conditionText)
    }

    @And("the following rules have been defined:")
    fun theFollowingRulesHaveBeenDefined(dataTable: DataTable) {
        dataTable.cells()
            .drop(1) // Drop the header row
            .forEach { row ->
                restClient().createRuleToAddText(
                    caseName = row[0],
                    text = row[1],
                    conditions = arrayOf(row[2])
                )
            }
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

    @And("the following comments have been defined in the project:")
    fun createComments(dataTable: DataTable) {
        dataTable.asList().forEach { comment ->
            restClient().getOrCreateConclusion(comment)
        }
    }
}
