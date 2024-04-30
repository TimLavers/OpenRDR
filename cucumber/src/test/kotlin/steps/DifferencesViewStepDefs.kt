package steps

import io.cucumber.java8.En
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import java.time.Duration

class DifferencesViewStepDefs : En {
    init {
        Then("the differences table should show {int} row(s)") { expected: Int ->
            await.atMost(Duration.ofSeconds(5)).untilAsserted {
                interpretationViewPO().numberOfRows() shouldBe expected
            }
        }

        Then("I should see in row {int} that the text {string} has been added") { row: Int, text: String ->
            interpretationViewPO().requireAddedTextRow(row - 1, text)
        }

        Then("I should see in row {int} that the text {string} has been deleted") { row: Int, text: String ->
            interpretationViewPO().requireDeletedTextRow(row - 1, text)
        }

        Then("I should see in row {int} that the text {string} has been replaced by {string}") { row: Int, replaced: String, replacement: String ->
            interpretationViewPO().requireReplacedTextRow(row - 1, replaced, replacement)
        }

    }
}
