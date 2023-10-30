package io.rippledown.caseview

import io.kotest.matchers.shouldBe
import kotlinx.datetime.toInstant
import proxy.findById
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class EpisodeDateCellTest {
    private val date0 = 1659752689505L

        @Test
        fun creation() {
            val vfc = FC {
                EpisodeDateCell {
                    index = 3
                    date = date0
                }
            }
            runReactTest(vfc) { container ->
                with(container) {
                    val cell = findById("episode_date_cell_3")
                    cell.textContent shouldBe "2022-08-06 02:25"
                    cell.textContent shouldBe formatDate(date0)
                }
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
