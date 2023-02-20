package io.rippledown.kb.export

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File

class RuleImporter(private val source: File) {
    init {
        checkIsDirectory(source)
        require(source.listFiles()!!.isNotEmpty()) {
            "$source is empty."
        }
    }

    fun import(): RuleTree {
        val format = Json { allowStructuredMapKeys = true }
        val idToPrototype = mutableMapOf<String, ExportedRule>()
        val idToRule = mutableMapOf<String, Rule>()
        source.listFiles()?.forEach {
            val data = FileUtils.readFileToString(it, Charsets.UTF_8)
            val prototype = format.decodeFromString<ExportedRule>(data)
            idToPrototype[prototype.id] = prototype
            val rule = Rule(prototype.id, null, prototype.conclusion, prototype.conditions)
            idToRule[prototype.id] = rule
        }
        idToPrototype.forEach {
            if (it.value.parentId != null) {
                val rule = idToRule[it.key]!!
                val parent = idToRule[it.value.parentId]!!
                parent.addChild(rule)
            }
        }
        val rulesWithoutParent = idToRule.values.filter { it.parent == null }
        require(rulesWithoutParent.size == 1) {
            "Rule tree deserialization failed."
        }
        val root = rulesWithoutParent[0]
        return RuleTree(root)
    }
}