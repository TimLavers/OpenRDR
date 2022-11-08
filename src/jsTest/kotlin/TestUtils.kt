import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase

fun createCase(name: String): ViewableCase {
    val attribute = Attribute("Glucose")
    val builder = RDRCaseBuilder()
    builder.addResult(attribute, 99994322, TestResult("5.1"))
    val rdrCase = builder.build(name)
    return ViewableCase(rdrCase, CaseViewProperties(listOf(attribute)))
}