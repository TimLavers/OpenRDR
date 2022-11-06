import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import kotlinx.html.currentTimeMillis

fun createCase(name: String): ViewableCase {
    val attribute = Attribute("Glucose")
    val builder = RDRCaseBuilder()
    builder.addResult(attribute, currentTimeMillis(), TestResult("5.1"))
    val rdrCase = builder.build(name)
    return ViewableCase(rdrCase, CaseViewProperties(listOf(attribute)))
}