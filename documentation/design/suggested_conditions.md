# Suggested conditions

Requirement: after each reason the chat offers a ranked list of conditions that hold for the case
([rule_building.md](../requirements/rule_building.md)).

Choosing conditions is the hard part of rule building: they must be true for the session case and false for the cases
that should keep their report. The suggester does the enumeration and puts the likely ones first; the language model is
used only to resolve which suggestion the user picked, never to generate one, so a suggestion is always a real, holding
condition.

## Generation

`ConditionSuggester`, given a `SuggestionContext` (session case, attributes, the rule action, the cornerstones and the
rule tree), enumerates every shape from [conditions.md](conditions.md) over the case's attributes:

- **Episodic**: attribute × predicate × signature, with signatures limited to the case's episode count (a single-episode
  case gets only `Current`).
- **Series**: attribute × series predicate, omitted for single-episode cases.
- **Case structure**: the predicates themselves.

A candidate is kept only if it can apply to the case: a fixed condition must hold; an editable one (`TSH ≥ __`) is kept
unless no edit could make it hold (a non-numeric latest value).

Two further sources and some prunes shape the list:

- **Historical injection.** The literal conditions of existing rules that assign the same attribute and that hold for
  the case are added as candidates. In pathology the cutoffs already in the rules are the clinically defensible ones
  (`eGFR ≥ 70`), whereas the generated editable cutoff pins to the case's reading (`eGFR ≥ 74`); injecting lets the user
  take the clinical one in a click.
- **KB-assigned attributes.** For a derived attribute, presence and absence are offered on top of value conditions,
  because presence means "another rule fired". For a comment attribute only presence is offered: its value restates that
  the comment was given, and stops holding once the comment has a variable; absence is not offered because nearly every
  comment in a KB is absent from any one case and would crowd the list.
- **Prunes.** `Is(<number>)` and `contains` on numeric attributes; `contains` on attributes no rule has ever
  substring-matched (free text is noise); a `no X is <range>` implied by an `all X are <other range>`; and the
  `IsNumeric`, extended-range and `AtLeast`/`AtMost` shapes, judged to add no clinical value.
- **Cycles.** Any condition that would create a dependency cycle is removed before ranking.

## Ranking

`RelevanceRanker` orders by four integer scores, then formal text for determinism:

| Score           | Signal                                                                                                                 |
|-----------------|------------------------------------------------------------------------------------------------------------------------|
| historical      | number of rules assigning the action's target attribute that use this condition — in RDR the strongest predictor       |
| comment overlap | tokens shared between the comment text (or the formula's attributes) and the condition's attribute and direction words |
| discrimination  | number of cornerstones the condition would exclude — how an expert reasons                                             |
| out of range    | tiebreak: the attribute is abnormal in the case                                                                        |

Outside a session the action is null and the cornerstones empty, so the order degrades to alphabetical. After ranking
the list is cut to 20: the chat shows about five, expandable to about ten, and a short list keeps the numbered version
shown to the model unambiguous.

## Editable suggestions

`SuggestedCondition` is either fixed or wraps an `EditableCondition`. `TSH is high` is fixed: changing `high` would make
it false for the case, and every other attribute already has its own suggestion. `TSH ≥ 0.67` almost always wants
editing. An `EditableCondition` is `fixed text` + `__` + `fixed text`, with the value typed as text, integer or real
according to the predicate, so the editor can validate. Some editable shapes are marked as usable once per rule, and
some carry a prerequisite that must hold before they are offered.

## Filtering after the fact

Conditions already in the rule are removed from the list where the response is assembled (`ChatResponseEnricher`),
because within one turn the model may fetch suggestions before it has registered the pick, and the buffered list would
otherwise re-offer the condition just added. The server also fetches suggestions itself when the model forgets to.

## Not built

- A pluggable strategy pipeline and a standalone `suggestions` module (needs `RuleTree` and friends in `common`).
- LLM re-ranking or generation, and embedding-based comment matching, if usage shows the deterministic scorers fall
  short.
- A free-form `ExpressionCondition` holding an expression string with a small interpreter, if compositional conditions
  are ever needed. Expressions would have to be canonicalised before `sameAs`, or the condition library fragments.
