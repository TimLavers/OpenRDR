
fun createCase(name: String): ViewableCase {
    val attribute = Attribute("Glucose", 1000)
    val builder = RDRCaseBuilder()
    builder.addResult(attribute, 99994322, TestResult("5.1"))
    val rdrCase = builder.build(name)
    return ViewableCase(rdrCase, CaseViewProperties(listOf(attribute)))
}