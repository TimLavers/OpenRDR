# AI-derived attributes

Requirements: [derived attributes](../requirements/derived_attributes.md).
Run instructions and synthetic examples: [demonstration and model evaluation](../ai_derived_attribute_demo.md).

## Definition and authoring

`AiBoolean` is a stored `ValueExpression` describing a Boolean classification of one existing `EXTERNAL` text
attribute. It contains the source, approved instructions, labelled examples, a revision token and an
`AiEvaluationProfile` containing provider, model, generation settings and prompt/schema versions.
The source is referenced by ID. Renaming a source or target does not change evaluation identity.

`AiDefinitionAuthoring` validates source, instructions, examples, profile and target name. Conflicting example
labels, non-primary sources and unsupported profiles are rejected. A near source-name match requires confirmation.
A new definition gets a server-owned draft and revision; acceptance accepts that exact draft, not a model
reconstruction. Its card shows criteria, examples and a preview when a case is available. A caseless draft
can be retained until selection; its first preview binds the case.

Acceptance starts an ordinary assignment session. The server asks for reasons and uses normal cornerstone review
before committing an `AssignValue(attribute, ByDefinition)` rule and its definition. New attributes have provisional
IDs until commit, so cancellation does not reserve names. Preview records can remain without activating a definition.
See [pending changes](previewing_pending_changes.md).

Editing the default creates a new revision and applies everywhere that uses `ByDefinition`, without editing rules
or automatically reviewing cornerstones. Conditioned exceptions use replacement or removal rules; literal Boolean
overrides remain available. See [derived definitions](derived_attribute_definitions.md).

The server chooses a default profile when drafting: `AI_EVALUATION_MODEL`, if set, otherwise the chat model constant.
Existing definitions keep their saved profiles. Credentials are runtime configuration, never definition data.

## Evaluation contract

Read only the source value in the latest episode. Missing, empty or whitespace-only input causes no request and
no assignment; an older nonblank episode is not substituted. Nonblank input is used exactly as supplied.

`DefaultValueExpressionEvaluator` resolves local expressions and reads prepared AI results. Direct local
evaluation of `AiBoolean` fails; shared model code never performs network I/O.

| Outcome | Meaning |
|---|---|
| `Value` | A value, with saved provenance for AI; Booleans materialise as `"true"` or `"false"`. |
| `NoInput` | No source text; ordinary inference continues without assigning the attribute. |
| `NeedsEvaluation` | A selected assignment needs an unprepared result; speculative inference stops. |
| `Failed` | Interpretation is incomplete and carries a diagnostic. |

A demand or failure is not absence and cannot activate absence-based downstream rules. Incomplete interpretations
cannot publish partial comments, generate reports or approve a rule review. False is a value and supports ordinary
downstream conditions. Only assignments selected by inference, or explicitly requested previews, are evaluated.

## Preparation and concurrency

`AiInferenceCoordinator` prepares under `KBSession.locked`, using isolated interpretations for candidate cases
and cornerstones. On demand it releases the lock, obtains a saved result or classification through
`AiEvaluationService`, adds the durable result to the KB's `PreparedEvaluations` map and retries against live
knowledge. The synchronous engine reads that map without store or provider I/O.

Mutation follows successful preparation under the lock. Session operations carry guards across unlocked work:
closing a KB, cancellation or a changed session invalidates a waiting operation. Failed condition changes and commit
requests restore the previous review; one-call rule creation uses a temporary session. Commit rechecks the source
case, definitions, applicability, conditions, cycles and conflicting cornerstones.

Operations are bounded to 100 demanded evaluations and 120 seconds by default. Identical in-flight requests share
work within a KB; cancelling one waiter does not cancel another's evaluation. `LockedRuleService` bridges the
synchronous chat service with `runBlocking` outside the KB lock. Endpoint operations that prepare AI work suspend.
Saved-result explanation uses store-only mode and never starts a new classification.

## Provider response

`GeminiBooleanClassifier` makes a fresh structured request with the approved definition and examples. Source text
is a separate encoded data field, without case identifiers, tools or chat history. Output must contain exactly
`value` (a JSON Boolean) and `evidence` (a verbatim source excerpt or null). Invalid fields, types or evidence fail;
strings such as `"false"` are never coerced. Oversized input is rejected rather than truncated.

Default limits are two attempts, 30 seconds per attempt and 65 seconds overall including backoff. Only transient
provider/transport failures are retried within that budget. Schema-valid output can still be wrong; model-quality
evaluation is separate from deterministic response-validation tests.

## Persistence and export

Each KB owns an `AiEvaluationStore`, in-memory or Postgres. Its key combines the definition revision, a canonical
fingerprint of source ID/criteria/examples/profile, and the SHA-256 hash of exact input text. Display names are excluded.
Identical input and definition can share a result across cases in one KB, but not across KBs.

A record retains the definition/profile snapshot, Boolean, optional evidence, timestamp, requested and reported
model, hashes and provenance ID. It does not duplicate the full source note. Insert-if-absent selects one durable
winner for racing requests; persistence failure leaves the operation incomplete. Failures are not stored as
classifications; in-flight sharing and a retry cooldown limit repeated failures. Explicit retry is available in chat.
Successful results are reused on case selection. New revisions require new results.

Export freezes the KB files under its lock, then reads the append-only evaluation store outside the lock. This
includes every saved result used by the snapshot. Records arriving for source attributes absent from that snapshot
are excluded so the archive remains importable. A failed store read fails the export.

`AiEvaluations` in a KB archive contains versioned records. Import checks definitions, source IDs, fingerprints,
policy versions, model consistency and conflicting duplicates before creating persistent state. Older archives
without records are accepted. Import makes no provider calls; missing results are obtained when needed.
Orphan preview records do not activate definitions, and KB deletion removes its store.

## Presentation and validation

The Derived attributes panel distinguishes values, no input and evaluation failure. Successful-value tooltips show
source, approved criteria, saved evidence and rule conditions; timestamps remain in provenance records.
Draft and explanation cards are attached by the server. Rendering does not classify text.

Failed interpretation responses use HTTP 503 with typed diagnostics and the retained case ID. A failed case view
also carries primary data with an empty interpretation. The client clears stale reports and permits chat retry.
Ingestion retains primary data even if interpretation fails.

Deterministic coverage includes `AiBooleanTest`, `GeminiBooleanClassifierTest`, `AiEvaluationServiceTest`,
`AiInferenceCoordinatorTest`, `AiRuleSessionIntegrationTest`, `AiDefinitionConversationTest`,
`KBEndpointAiInterpretationTest` and `AiEvaluationExportImportTest`. Live authoring and model-quality checks are
separate opt-in tests; see the [demo guide](../ai_derived_attribute_demo.md). Outstanding review and product work
is in the [backlog](../tickets/TODO.md).

Multiple sources, derived-value inputs, non-Boolean outputs, historical-episode aggregation, bulk reclassification
and automatic record eviction are not implemented. Formulas and comments can consume the resulting Boolean.
