package io.rippledown.standalone

import io.rippledown.kb.KB
import io.rippledown.kb.sample.defaultDate
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.simpleapi.SimpleInterpreter

class StandAloneInterpreter(val kb: KB): SimpleInterpreter {
    override fun interpretStringMap(caseData: Map<String, String>): String {
        val builder = RDRCaseBuilder()
        caseData.forEach { (attributeName, value) ->
            val attribute = kb.attributeManager[attributeName]
            if (attribute != null) {
                builder.addValue(attribute, defaultDate, value)
            }
        }
        val case = builder.build("irrelevant")
        val interpretation = kb.interpret(case)
        return interpretation.conclusionTexts().toList().sorted().joinToString("\n")
    }
}