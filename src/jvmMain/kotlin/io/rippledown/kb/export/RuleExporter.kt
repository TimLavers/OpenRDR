package io.rippledown.kb.export

import io.rippledown.model.rule.RuleTree
import java.io.File

class RuleExporter(private val destination: File, private val ruleTree: RuleTree) {
    init {
        checkDirectoryIsSuitableForExport(destination, "Rule")
    }

    fun export() {
        val rules = ruleTree.rules()
        val ruleIds = rules.map { it.id.toString() }.toSet()
        val ruleIdToFilename = FilenameMaker(ruleIds).makeUniqueNames()

        rules.forEach{
            val filename = ruleIdToFilename[it.id.toString()]!!
            val file = File(destination, filename)
            ExportedRule(it).export(file)
        }
    }
}