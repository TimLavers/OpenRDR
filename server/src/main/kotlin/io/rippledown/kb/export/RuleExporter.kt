package io.rippledown.kb.export

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import io.rippledown.persistence.PersistentRule
import kotlinx.serialization.json.Json

class RuleExporter: Exporter<Rule>, Importer<PersistentRule> {
    // An export made before conclusions were retired has a conclusion id in
    // each rule, which is ignored rather than refused.
    private val json = Json {
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
    }

    override fun exportToString(t: Rule) = json.encodeToString(PersistentRule(t))

    override fun importFromString(data: String) = json.decodeFromString<PersistentRule>(data)
}
class RuleSource(val ruleTree: RuleTree): IdentifiedObjectSource<Rule> {
    override fun all() = ruleTree.rules()

    override fun idFor(t: Rule) = t.id

    override fun exporter() = RuleExporter()

    override fun exportType() = "Rule"
}