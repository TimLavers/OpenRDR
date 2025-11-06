@file:OptIn(ExperimentalTime::class)

package io.rippledown.caseview

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.mocks.DummyRowScope
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class DateCellTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun show() {
        val date = Instant.parse("2010-06-01T22:19:44.475Z").toEpochMilliseconds()
        val columnWidths = mockk<ColumnWidths>()
        every { columnWidths.valueColumnWeight() }.returns(0.5F)

        val rowScope: RowScope = DummyRowScope()
        composeTestRule.setContent {
            rowScope.DateCell( 1, date, columnWidths)
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText("2010-06-01 22:20"))
        }
    }

    @Test
    fun dateFormatting() {
        val date1 = Instant.parse("2010-06-01T22:19:44.475Z").toEpochMilliseconds()
        formatDate(date1) shouldBe "2010-06-01 22:20"

        val date2 = Instant.parse("2010-06-01T22:19:24.475Z").toEpochMilliseconds()
        formatDate(date2) shouldBe "2010-06-01 22:19"

        val date3 = Instant.parse("2010-06-01T22:19:29.999Z").toEpochMilliseconds()
        formatDate(date3) shouldBe "2010-06-01 22:19"

        val date4 = Instant.parse("2010-06-01T22:19:30.000Z").toEpochMilliseconds()
        formatDate(date4) shouldBe "2010-06-01 22:20"

        val date5 = Instant.parse("2010-06-01T22:19:31.000Z").toEpochMilliseconds()
        formatDate(date5) shouldBe "2010-06-01 22:20"
    }
}