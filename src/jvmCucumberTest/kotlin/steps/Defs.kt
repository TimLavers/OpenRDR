package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.kotest.matchers.shouldBe
import io.rippledown.integration.UITestBase
import io.rippledown.integration.pageobjects.CaseListPO
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit

class Defs : En {
    val uiTestBase = UITestBase()
    val serverProxy = uiTestBase.serverProxy
    val labProxy = uiTestBase.labProxy

    lateinit var caseListPO: CaseListPO
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

        Then("I should see the following cases in the case list:") { dataTable: DataTable ->
            val expectedCaseNames = dataTable.asList()
            caseListPO.waitForCaseListToHaveSize(expectedCaseNames.size)
            val actualCaseNames = caseListPO.casesListed()
            actualCaseNames shouldBe expectedCaseNames
        }

    }
}
