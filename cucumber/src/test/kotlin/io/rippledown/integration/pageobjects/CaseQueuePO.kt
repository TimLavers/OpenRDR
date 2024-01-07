package io.rippledown.integration.pageobjects

import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID
import org.awaitility.Awaitility.await
import java.util.concurrent.TimeUnit

class CaseQueuePO() {

    fun waitForNumberOfCasesToBe(count: Int): CaseQueuePO {
        await().atMost(5, TimeUnit.SECONDS).until {
            numberOfCases() == count
        }
        return this
    }

    fun numberOfCases() = 0
//    driver.findElement(By.id(NUMBER_OF_CASES_ID))
//        .text
//        .substringAfter(CASES).trim()
//        .toInt()
}
