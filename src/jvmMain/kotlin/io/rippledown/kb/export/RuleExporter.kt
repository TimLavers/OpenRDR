package io.rippledown.kb.export

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RuleExporter: Exporter<Rule>, Importer<PersistentRule> {
    private val json = Json { allowStructuredMapKeys = true }

    override fun exportToString(t: Rule) = json.encodeToString(PersistentRule(t))

    override fun importFromString(data: String) = json.decodeFromString<PersistentRule>(data)
}
class RuleSource(val ruleTree: RuleTree): IdentifiedObjectSource<Rule> {
    override fun all() = ruleTree.rules()

    override fun idFor(t: Rule) = t.id

    override fun exporter() = RuleExporter()

    override fun exportType() = "Rule"
}