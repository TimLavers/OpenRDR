package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseViewPO
import io.rippledown.integration.pageobjects.KBInfoPO
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit

class Defs : En {
    private val uiTestBase = UITestBase()
    private val serverProxy = uiTestBase.serverProxy
    private val labProxy = uiTestBase.labProxy

    private lateinit var caseListPO: CaseListPO
    private lateinit var caseViewPO: CaseViewPO
    private lateinit var driver: WebDriver

    init {
        Before { scenario ->
            println("Before scenario '${scenario.name}'")
            serverProxy.start()
            driver = uiTestBase.setupWebDriver()
        }

        After { scenario ->
            println("After scenario '${scenario.name}'")
            uiTestBase.driverClose()
            serverProxy.shutdown()
        }

        Given("a list of cases with the following names is stored on the server:") { dataTable: DataTable ->
            dataTable.asList().forEach { caseName ->
                labProxy.copyCase(caseName)
            }
        }
        Given("a list of {long} cases is stored on the server") { numberOfCases: Long ->
            (1..numberOfCases).forEach { i ->
                val padded = i.toString().padStart(3, '0')
                labProxy.writeNewCaseFile("Case $padded")
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

        When("the case with the name {string} is deleted on the server") { caseName: String ->
            labProxy.deleteCase(caseName)
        }

        And("I select case {string}") { caseName: String ->
            caseViewPO = caseListPO.select(caseName)
        }

        And("if I select case {word}") { caseName: String ->
            caseViewPO = caseListPO.select(caseName)
        }

        And("I move attribute {word} below attribute {word}") { moved: String, target: String ->
            caseViewPO.dragAttribute(moved, target)
            Thread.sleep(1000)
        }

        Given("I start the application and the initial Attribute order is A, B, C") {
            caseListPO = CaseListPO(driver)
            caseViewPO = CaseViewPO(driver)
            labProxy.writeCaseWithDataToInputDir("Case1", mapOf("A" to "a"))
            labProxy.writeCaseWithDataToInputDir("Case2", mapOf("A" to "a", "B" to "b"))
            labProxy.writeCaseWithDataToInputDir("Case3", mapOf("A" to "a", "B" to "b", "C" to "c"))
            // The attributes are created when the cases are parsed, so select them in the right order.
            caseListPO.select("Case1")
            caseListPO.select("Case2")
            caseListPO.select("Case3")
        }

        Given("case {word} is provided having data:") { caseName: String, dataTable: DataTable ->
            val attributeNameToValue = mutableMapOf<String, String>()
            dataTable.asMap().forEach { (t, u) -> attributeNameToValue[t] = u }
            labProxy.writeCaseWithDataToInputDir(caseName, attributeNameToValue)
        }

        Then("^the case (should show|shows) the attributes in order:$") { ignoredOption: String, dataTable: DataTable ->
            caseViewPO.attributes() shouldBe dataTable.asList()
        }

        Then("the displayed KB name should be {word}") { kbName: String ->
            KBInfoPO(driver).headingText() shouldBe "Knowledge Base: $kbName"
        }

        When("I start the client application") {
            //client application is started in the Before hook
            caseListPO = CaseListPO(driver)
            caseViewPO = CaseViewPO(driver)
        }

        When("stop the client application") {
            //client application is stopped in the After hook
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

        Then("I should see the case {string} as the current case") { caseName: String ->
            caseViewPO.nameShown() shouldBe caseName
        }
    }
}
