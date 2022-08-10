import io.kotest.matchers.shouldBe
import io.rippledown.model.CaseId
import mysticfall.ReactTestSupport
import kotlin.test.Test

class CaseListTest : ReactTestSupport {

    @Test
    fun shouldListCaseIds() {
        val caseIds = listOf(
            CaseId(id = "1", name = "a"),
            CaseId(id = "2", name = "b")
        )
        val renderer = render {
            CaseList {
                attrs.caseIds = caseIds
            }

        }
        val ids = renderer.root.findAllByType(CaseList).map { c -> c.props.caseIds }
        ids shouldBe listOf(caseIds)

    }
}
