package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import io.rippledown.integration.pageobjects.CaseViewPO
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit

class Defs : En {
    val uiTestBase = UITestBase()
    val serverProxy = uiTestBase.serverProxy
    val labProxy = uiTestBase.labProxy

    lateinit var caseListPO: CaseListPO
    lateinit var caseViewPO: CaseViewPO
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
