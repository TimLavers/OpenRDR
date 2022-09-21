import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.toInstant
import mysticfall.ReactTestSupport
import react.dom.html.ReactHTML
import kotlin.test.Test

class EpisodeDateCellTest : ReactTestSupport {
    val date0 = 1659752689505L

    @Test
    fun creation() {
        val renderer = render {
            EpisodeDateCell {
                attrs.index = 3
                attrs.date = date0
            }
        }
        val cells = renderer.root.findAllByType(EpisodeDateCell)
        cells[0].props.index shouldBe 3
        cells[0].props.date shouldBe date0

        val byId = renderer.root.findAllByType(ReactHTML.th.toString())
            .first {
                it.props.asDynamic()["id"] == "episode_date_cell_3"
            }
        byId shouldNotBe null
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
