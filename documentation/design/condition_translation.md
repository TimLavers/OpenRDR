# Condition translation

Requirement: a reason is stated in the user's own words and language and becomes a `Condition`
([rule_building.md](../requirements/rule_building.md)).

## Why a language model, not a GUI or a grammar

| Approach                   | Why not                                                                                                                                               |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| Condition-building GUI     | Friendly for `TSH > 4`, but multi-episode conditions (`at least 3 Glucose high in the past year`) need a large custom widget per shape.               |
| Formal syntax and a parser | Flexible, but the user must learn and remember it, the parser is non-trivial, and every language needs its own grammar.                               |
| **LLM translation**        | Accepts `elevated glucose`, `raised glucose`, `glucosa elevada` alike; no parser, no per-shape UI, internationalisation for free. Costs a model call. |

The cost is acceptable because the output is never trusted: a translated condition must parse, name attributes the
case has, hold for the case and create no cycle before it is added (see below). The model chooses words; the server
decides truth.

## How

The `hints` module owns the translation. `ConditionChatService` holds one long-lived Gemini chat per
`RuleSessionManager`, whose system prompt lists the predicate and signature classes with worked examples. Each request
sends the case's attribute names and the user's expression; the reply is a `ConditionSpecification` JSON: attribute
name, predicate name and parameters, signature name and parameters, plus the user's expression. `ConditionGenerator`
instantiates the named predicate and signature reflectively from the `common` condition packages and assembles the
`Condition`, keeping the user's phrase on it.

The translator is a **separate conversation** from the operator chat ([chat_architecture.md](chat_architecture.md)).
The operator model calls it as a function (`transformReasonToFormalCondition`) and receives either the formal text or a
corrective message to relay. Keeping the two apart keeps the translator's prompt small and its examples focused; the
operator's conversation history is not needed to translate a single phrase, and a combined prompt would have to police
two jobs at once.

## Validation before anything is added

`RuleSessionManager.conditionForExpression` refuses a translated condition unless all of:

1. it parsed to a `Condition`;
2. every attribute it names exists (names are resolved exactly, then punctuation-normalised, then by a single edit,
   because model output is not trusted verbatim);
3. it holds for the case the rule is being built on;
4. it creates no dependency cycle ([rule_tree_and_inference.md](rule_tree_and_inference.md)).

Failures are returned to the operator model as the function result, phrased as instructions it can relay ("does not
hold for this case", "cannot be understood"), so the user learns why and nothing is added silently.
