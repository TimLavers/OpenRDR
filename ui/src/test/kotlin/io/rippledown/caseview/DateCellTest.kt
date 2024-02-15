package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.mocks.DummyRowScope
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DateCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun show() {
        val date = "2010-06-01T22:19:44.475Z".toInstant().toEpochMilliseconds()

        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.DateCell( 1, date, 0.1F)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("2010-06-01 22:20"))
        }
    }

    @Test
    fun dateFormatting() {
        val date1 = "2010-06-01T22:19:44.475Z".toInstant().toEpochMilliseconds()
        formatDate(date1) shouldBe "2010-06-01 22:20"

        val date2 = "2010-06-01T22:19:24.475Z".toInstant().toEpochMilliseconds()
        formatDate(date2) shouldBe "2010-06-01 22:19"

        val date3 = "2010-06-01T22:19:29.999Z".toInstant().toEpochMilliseconds()
        formatDate(date3) shouldBe "2010-06-01 22:19"

        val date4 = "2010-06-01T22:19:30.000Z".toInstant().toEpochMilliseconds()
        formatDate(date4) shouldBe "2010-06-01 22:20"

        val date5 = "2010-06-01T22:19:31.000Z".toInstant().toEpochMilliseconds()
        formatDate(date5) shouldBe "2010-06-01 22:20"
    }
}