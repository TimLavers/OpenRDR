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
import io.rippledown.integration.utils.Cyborg
import io.rippledown.integration.waitUntilAsserted
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

    @When("A Knowledge Base called {word} has been created")
    fun AKnowledgeBaseCalledWordHasBeenCreated(name: String) {
        restClient().createKB(name)
    }

    @When("I start the client application")
    fun IStartTheClientApplication() {
        startClient()
    }

    @When("stop the client application")
    fun stopTheClientApplication() {
        //client application is stopped in the After hook
    }

    @When("I stop the client application")
    fun IStopTheClientApplication() {
    }

    @Given("a list of cases with the following names is stored on the server:")
    fun aListOfCasesWithTheFollowingNamesIsStoredOnTheServer(dataTable: DataTable) {
        dataTable.asList().forEach { caseName ->
            labProxy().provideCase(caseName)
        }
    }

    @Given("a case with name {word} is stored on the server")
    fun aCaseWithNameWordIsStoredOnTheServer(caseName: String) {
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
    fun IRestartTheServerApplication() {
        stopServer()
        pause(3000)
        reStartWithPostgres()
    }

    @And("I select a case with all three attributes")
    fun ISelectACaseWithAllThreeAttributes() {
        caseListPO().select("CaseABC")
    }

    @When("a new case with the name {word} is stored on the server")
    fun aNewCaseWithTheNameWordIsStoredOnTheServer(caseName: String) {
        labProxy().provideCase(caseName)
    }

    @Given("the configured case {word} is stored on the server")
    fun theConfiguredCaseWordIsStoredOnTheServer(caseName: String) {
        labProxy().provideCase(caseName)
    }

    @Given("the following cases are deleted on the server:")
    fun theFollowingCasesAreDeletedOnTheServer(dataTable: DataTable) {
        dataTable.asList().forEach { caseName ->
            labProxy().restProxy.deleteProcessedCaseWithName(caseName)
        }
    }

    @When("the case with the name {word} is deleted on the server")
    fun theCaseWithTheNameWordIsDeletedOnTheServer(caseName: String) {
        labProxy().restProxy.deleteProcessedCaseWithName(caseName)
    }

    @And("I select case {word}")
    fun ISelectCaseWord(caseName: String) {
        caseListPO().select(caseName)
    }

    @And("I move attribute {word} below attribute {word}")
    fun IMoveAttributeWordBelowAttributeWord(moved: String, target: String) {
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
//        with(caseListPO()) {
//             The attributes are created when the cases are parsed, so select them in the right order.
//            select("Case1")
//            select("Case2")
//            select("Case3")
//        }
    }

    @Given("I import the configured zipped Knowledge Base {word}")
    fun IImportTheConfiguredZippedKnowledgeBaseWord(toImport: String) {
        val zipFile = ConfiguredTestData.kbZipFile(toImport)
        pause(10_000)
        kbControlsPO().importKB(zipFile.absolutePath)
    }

    @And("I export the current Knowledge Base")
    fun IExportTheCurrentKnowledgeBase() {
        exportedZip = File.createTempFile("Exported", ".zip")
        kbControlsPO().exportKB(exportedZip!!.absolutePath)
    }

    @Then("there is a file called {word} in my downloads directory")
    fun thereIsAFileCalledWordInMyDownloadsDirectory(fileName: String) {
        Awaitility.await().atMost(Duration.ofSeconds(5)).until {
            File(StepsInfrastructure.uiTestBase.downloadsDir(), fileName).exists()
        }
    }

    @Given("I import the previously exported Knowledge Base")
    fun IImportThePreviouslyExportedKnowledgeBase() {
        require(exportedZip != null) {
            "Import of previously exported KB attempted but exported KB is null."
        }
        kbControlsPO().importKB(exportedZip!!.absolutePath)
    }

    @Given("case {word} is provided having data:")
    fun caseWordIsProvidedHavingData(caseName: String, dataTable: DataTable) {
        val attributeNameToValue = mutableMapOf<String, String>()
        dataTable.asMap().forEach { (t, u) -> attributeNameToValue[t] = u }
        labProxy().provideCase(caseName, attributeNameToValue)
    }

    @Then("the displayed KB name is (now ){word}")
    fun theDisplayedKBNameIsNowWord(kbName: String) {
        waitUntilAsserted {
            kbControlsPO().currentKB() shouldBe kbName
        }
    }

    @Then("I activate the KB management control")
    fun IActivateTheKBManagementControl() {
        kbControlsPO().expandDropdownMenu()
    }

    @Then("I (should )see this list of available KBs:")
    fun IShouldSeeThisListOfAvailableKBs(dataTable: DataTable) {
        val expectedKBs = dataTable.asList()
        kbControlsPO().availableKBs() shouldBe expectedKBs
    }

    @Then("the displayed product name is 'Open RippleDown'")
    fun theDisplayedProductNameIsOpenRippleDown() {
        applicationBarPO().title() shouldBe "Open RippleDown"
    }

    @Then("I create a Knowledge Base with the name {word}")
    fun ICreateAKnowledgeBaseWithTheNameWord(kbName: String) {
        kbControlsPO().createKB(kbName)
    }

    @Then("I create a Knowledge Base with the name {word} based on the {string} sample")
    fun ICreateAKnowledgeBaseWithTheNameWordBasedOnTheStringSample(kbName: String, sampleTitle: String) {
        kbControlsPO().createKBFromSample(kbName, sampleTitle)
    }

    @Then("I select the Knowledge Base named {word}")
    fun ISelectTheKnowledgeBaseNamedWord(kbName: String) {
        kbControlsPO().selectKB(kbName)
    }

    @And("pause for {long} second(s)")
    fun pauseForLongSeconds(seconds: Long) {
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

    @When("I delete all the text in the interpretation field")
    fun IDeleteAllTheTextInTheInterpretationField() {
        interpretationViewPO().deleteAllText()
    }

    @And("I enter the text {string} in the interpretation field")
    fun IEnterTheTextStringInTheInterpretationField(text: String) {
        interpretationViewPO().setVerifiedText(text)
    }

    @And("I add the text {string} at the end of the current interpretation")
    fun IAddTheTextStringAtTheEndOfTheCurrentInterpretation(text: String) {
        interpretationViewPO().addVerifiedTextAtEndOfCurrentInterpretation(text)
    }

    @And("I slowly type the text {string} in the interpretation field")
    fun ISlowlyTypeTheTextStringInTheInterpretationField(text: String) {
        with(Cyborg()) {
            typeSlowly(text)
        }
    }

    @And("I build a rule to add the conclusion {string} with no conditions")
    fun IBuildARuleToAddTheConclusionStringWithNoConditions(text: String) {
        TODO()
    }

    @When("I replace the text in the interpretation field with {string}")
    fun IReplaceTheTextInTheInterpretationFieldWithString(text: String) {
        interpretationViewPO().selectOriginalTab()
        interpretationViewPO().setVerifiedText(text)
    }

    @Then("the interpretation field should contain the text {string}")
    fun theInterpretationFieldShouldContainTheTextString(text: String) {
        interpretationViewPO().selectOriginalTab()
        interpretationViewPO().waitForInterpretationTextToContain(text)
    }

    @Then("the interpretation should be {string}")
    fun theInterpretationShouldBeString(text: String) {
        interpretationViewPO().selectOriginalTab()
        interpretationViewPO().waitForInterpretationText(text)
    }

    @Then("the interpretation should be this:")
    fun theInterpretationShouldBeThis(text: DocString) {
        interpretationViewPO().selectOriginalTab()
        interpretationViewPO().waitForInterpretationText(text.content)
    }

    @Then("the interpretation field should be empty")
    fun theInterpretationFieldShouldBeEmpty() {
        interpretationViewPO().selectOriginalTab()
        interpretationViewPO().waitForInterpretationText("")
    }

    @And("the interpretation of the case {word} is {string}")
    fun theInterpretationOfTheCaseWordIsString(caseName: String, text: String) {
        restClient().createRuleToAddText(caseName, text)
    }

    @And("the interpretation of the case {word} includes {string} because of condition {string}")
    fun theInterpretationOfTheCaseWordIncludesStringBecauseOfConditionString(
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
                restClient().createRuleToAddText(row[0], row[1], row[2])
            }
    }

    @And("I select the {word} tab")
    fun ISelectTheWordTab(tabName: String) {
        when (tabName) {
            "interpretation" -> interpretationViewPO().selectOriginalTab()
            "comments" -> interpretationViewPO().selectConclusionsTab()
            "changes" -> interpretationViewPO().selectDifferencesTab()
            else -> throw IllegalArgumentException("Unknown tab name: $tabName")
        }
    }

    @And("the changes badge indicates that there is/are {int} change(s)")
    fun theChangesBadgeIndicatesThatThereIsareIntChanges(numberOfChanges: Int) {
        interpretationViewPO().waitForBadgeCount(numberOfChanges)
    }

    @And("the changes badge indicates that there is no change")
    fun theChangesBadgeIndicatesThatThereIsNoChange() {
        interpretationViewPO().waitForNoBadgeCount()
    }

    @When("I select the condition in position {int}")
    fun ISelectTheConditionInPositionInt(index: Int) {
        TODO()
//        conditionSelectorPO.clickConditionWithIndex(index)
    }

    @Then("the following conditions (are )(should be )selected:")
    fun theFollowingConditionsAreShouldBeSelected(dataTable: DataTable) {
        TODO()
        val expectedConditions = dataTable.asList()
//        conditionSelectorPO.requireConditionsToBeSelected(expectedConditions)
    }

    @And("I build a rule to replace the interpretation by {string} with the condition {string}")
    fun IBuildARuleToReplaceTheInterpretationByStringWithTheConditionString(replacement: String, condition: String) {
        with(interpretationViewPO()) {
            deleteAllText()
            setVerifiedText(replacement)
            selectDifferencesTab()
            clickBuildIconOnRow(0)
        }
        with(ruleMakerPO()) {
            clickConditionWithText(condition)
            clickDoneButton()
        }
    }

    @Then("the conditions showing are:")
    fun theConditionsShowingAre(dataTable: DataTable) {
        val expectedConditions = dataTable.asList()
        expectedConditions.forEachIndexed { index, condition ->
            conclusionsViewPO().requireConditionAtIndex(0, index, condition)
        }
    }

    @Then("the KB controls (are )(should be )hidden")
    fun theKBControlsAreShouldBeHidden() {
        kbControlsPO().requireKbControlsToBeHidden()
    }

    @Then("the KB controls (are )(should be )shown")
    fun theKBControlsAreShouldBeShown() {
        kbControlsPO().requireKbControlsToBeShown()
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
}
