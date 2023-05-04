package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.InterpretationViewPO
import io.rippledown.integration.pageobjects.KBInfoPO
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
    private lateinit var interpretationViewPO: InterpretationViewPO
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
            interpretationViewPO = InterpretationViewPO(driver)
        }

        When("stop the client application") {
            //client application is stopped in the After hook
        }

        Given("a list of cases with the following names is stored on the server:") { dataTable: DataTable ->
            dataTable.asList().forEach { caseName ->
                labProxy.copyCase(caseName)
            }
        }
        Given("a list of {long} cases is stored on the server") { numberOfCases: Long ->
            (1..numberOfCases).forEach { i ->
                val padded = i.toString().padStart(3, '0')
                labProxy.writeNewCaseFile("Case_$padded")
            }
        }

        And("I select a case with all three attributes") {
            caseViewPO = caseListPO.select("CaseABC")
        }

        When("a new case with the name {string} is stored on the server") { caseName: String ->
            labProxy.copyCase(caseName)
        }

        Given("the following cases are deleted on the server:") { dataTable: DataTable ->
            dataTable.asList().forEach { caseName ->
                labProxy.deleteCase(caseName)
            }
        }

        When("the case with the name {word} is deleted on the server") { caseName: String ->
            labProxy.deleteCase(caseName)
        }

        And("I select case {word}") { caseName: String ->
            caseViewPO = caseListPO.select(caseName)
        }

        And("I move attribute {word} below attribute {word}") { moved: String, target: String ->
            caseViewPO.dragAttribute(moved, target)
            Thread.sleep(1000)
        }

        Given("the initial Attribute order is A, B, C") {
            labProxy.writeCaseWithDataToInputDir("Case1", mapOf("A" to "a"))
            labProxy.writeCaseWithDataToInputDir("Case2", mapOf("A" to "a", "B" to "b"))
            labProxy.writeCaseWithDataToInputDir("Case3", mapOf("A" to "a", "B" to "b", "C" to "c"))
            // The attributes are created when the cases are parsed, so select them in the right order.
            caseListPO.select("Case1")
            caseListPO.select("Case2")
            caseListPO.select("Case3")
        }

        Given("I import the configured zipped Knowledge Base {word}") { exported: String ->
            val kbInfoPO = KBInfoPO(driver)
            kbInfoPO.importKB(exported)
            kbInfoPO.waitForKBToBeLoaded(exported)
        }

        And("I export the current Knowledge Base") {
            val kbInfoPO = KBInfoPO(driver)
            kbInfoPO.exportKB()
        }

        Then("there is a file called Thyroids.zip in my downloads directory") {
            Awaitility.await().atMost(Duration.ofSeconds(5)).until {
                File(uiTestBase.downloadsDir(), "Thyroids.zip").exists()
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
            labProxy.writeCaseWithDataToInputDir(caseName, attributeNameToValue)
        }

        Then("^the case (should show|shows) the attributes in order:$") { ignoredOption: String, dataTable: DataTable ->
            caseViewPO.attributes() shouldBe dataTable.asList()
        }

        Then("the displayed KB name is (now ){word}") { kbName: String ->
            KBInfoPO(driver).headingText() shouldBe kbName
        }


        And("pause for {long} seconds") { seconds: Long ->
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

        Then("I should see the case {word} as the current case") { caseName: String ->
            caseViewPO.nameShown() shouldBe caseName
        }

        And("select the case {word}") { caseName: String ->
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
            interpretationViewPO.interpretationText() shouldBe text
        }

        And("the interpretation by the project of the case {string} is {string}") { caseName: String, text: String ->
            RESTClient().createRuleToAddText(caseName, text)
        }
        And("I select the changes tab") {
            interpretationViewPO.selectChangesTab()
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
        And("the changes label should indicate that there is {int} change(s)") { numberOfChanges: Int ->
            interpretationViewPO.requireChangesLabel("CHANGES ($numberOfChanges)")
        }
    }
}
