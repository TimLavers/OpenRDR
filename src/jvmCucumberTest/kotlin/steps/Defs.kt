package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.cucumber.java8.PendingException
import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseViewPO
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

class Defs : En {
    val uiTestBase = UITestBase()
    val serverProxy = uiTestBase.serverProxy
    val labProxy = uiTestBase.labProxy

    lateinit var caseListPO: CaseListPO
    lateinit var caseView: CaseViewPO
    lateinit var driver: WebDriver

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

        And("I select a case with all three attributes") {
            caseView = caseListPO.select("CaseABC")
        }

        And("if I select case {word}") { caseName: String ->
            caseView = caseListPO.select(caseName)
        }

        And("I move attribute {word} below attribute {word}") {moved: String, target: String ->
            caseView.dragAttribute(moved, target)
            Thread.sleep(1000)
        }

        Given(
            "case {word} is provided having data:") { caseName: String, dataTable: DataTable ->
            val attributeNameToValue = mutableMapOf<String, String>()
            dataTable.asMap().forEach { (t, u) ->  attributeNameToValue[t] = u}
            labProxy.writeCaseWithDataToInputDir(caseName, attributeNameToValue)
        }

        Then("the case should show the attributes in order:") { dataTable: DataTable ->
            caseView.attributes() shouldBe  dataTable.asList()
        }

        When("I start the client application") {
            //client application is started in the Before hook
            caseListPO = CaseListPO(driver)
        }

        When("stop the client application") {
            //client application is stopped in the After hook
        }

        And("pause") {
            Thread.sleep(TimeUnit.DAYS.toMillis(1L))
        }

        And("pause briefly") {
            Thread.sleep(TimeUnit.SECONDS.toMillis(20L))
        }

        Then("I should see the following cases in the case list:") { dataTable: DataTable ->
            val expectedCaseNames = dataTable.asList()
            caseListPO.waitForCaseListToHaveSize(expectedCaseNames.size)
            val actualCaseNames = caseListPO.casesListed()
            actualCaseNames shouldBe expectedCaseNames
        }
    }
}
