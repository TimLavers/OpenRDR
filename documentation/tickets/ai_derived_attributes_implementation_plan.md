# Implementation plan: AI-evaluated derived attributes

**Status: steps 1–4 implemented; step 5 next.**

Design and acceptance semantics: [AI-evaluated derived attributes](ai_derived_attributes_design.md).
Implement in the order below. Names marked new are proposed names, not claims about existing classes.

## Progress

Where the implementation settled on different names or placement than proposed below, the implemented form is
authoritative for later steps.

- **Step 1 — done.** `AiBoolean`, `AiEvaluationProfile`, `AiBooleanExample` in `common/.../model/rule/AiBoolean.kt`;
  `AiProvenance` in `common/.../model/caseview`. Server: `model/rule/evaluation/ValueExpressionEvaluator.kt`
  (`AiEvaluationRequest`, `AiEvaluationResult`, `EvaluationContext`, `EvaluationOutcome`,
  `DefaultValueExpressionEvaluator`, `AiBoolean.inputFrom`), `kb/ai/AiBooleanValidation.kt`, `kb/ai/AiEvaluationPolicy.kt`.
  The evaluator tests live in `DefaultValueExpressionEvaluatorTest`, not a separate `AiExpressionEvaluatorTest`.
- **Step 2 — done.** `AiEvaluationKey`/`AiEvaluationRecord` in `model/rule/evaluation`; `persistence/AiEvaluationStore`
  (`InMemoryAiEvaluationStore`, `PostgresAiEvaluationStore`); `AiBooleanClassifier`/`AiClassification` and
  `AiEvaluationService` in `kb/ai`; export/import of records.
- **Step 3 — done.** The adapter is in `llm`, not in `Gemini.kt`: `GeminiBooleanClassifier` (prompt contract v1,
  strict parsing, `ClassificationLimits`, typed `ClassificationFailure` kinds) over a `StructuredGenerationTransport`
  whose production implementation is `GeminiStructuredTransport`. The server bridge is `kb/ai/GeminiAiBooleanClassifier`;
  `AiEvaluationPolicy` takes its versions from the `llm` constants. `GeminiStructuredTransportTest` is the live check.
- **Step 4 — done.** `RuleTree.infer(case, resolver, evaluator, context)` returns `InferenceOutcome`:
  `Complete(case, provenance)` or `Stopped` (`Demand(attribute, request)` / `Incomplete(attribute, reason)`); a stopped
  run resets the case's interpretation. `materialise`/`apply` keep local semantics and throw
  `IncompleteInferenceException` on a stopped outcome, so until step 5 routes the entry points through the coordinator a
  KB holding an AI definition fails loudly rather than answering wrongly.
- **Steps 5–10 — not started.**

## Working rules and verification

- Follow [AGENTS.md](../../AGENTS.md): Kotlin production classes and tests, no production `!!`, JUnit 5, Mockk without
  per-mock `relaxed = true`, Kotest matchers, and Given/When/Then comments.
- For each production slice, write and run a meaningful failing unit test, implement the minimum behaviour, then
  extend happy/unhappy/boundary coverage and refactor. Keep existing formula/comment behaviour pinned throughout.
- Do not commit changes; the owner reviews the code first.
- Target individual non-UI test classes while iterating. Compiling and individual test files are already authorised.
  Ask before running more than one UI unit test or more than one Cucumber test, as required by AGENTS.md. Schedule
  desktop-taking suites with the owner; do not launch broad UI/Cucumber tasks as a default regression check.
- Use [running-tests.md](../../.windsurf/rules/running-tests.md) for Gradle commands and database constraints, with
  current AGENTS.md taking precedence where instructions differ. Check Postgres availability before running its
  tests; do not launch a suite that hangs waiting for a database. Keep live Gemini evaluation opt-in.

## Repository integration map

Paths below are relative to the repository root. Main production packages are rooted at `io/rippledown`.

| Area | Existing integration points | Required change |
|---|---|---|
| Expressions | `common/src/main/kotlin/io/rippledown/model/rule/ValueExpression.kt` | Add serializable AI definition and source references |
| Definition persistence | `server/.../kb/DerivedDefinitionManager.kt`, `persistence/DerivedDefinitionStore.kt` | Accept new subtype; retain old JSON compatibility |
| Local inference | `server/.../model/rule/RuleTree.kt`, `DerivedAttributeDependencyGraph.kt` | Explicit evaluation context; missing-input and demand outcomes |
| Rule preparation | `server/.../model/rule/RuleBuildingSession.kt`, `kb/RuleSessionManager.kt` | Prepare AI-dependent previews/reviews without live mutations |
| Lock/orchestration | `server/.../kb/KBSession.kt`, `server/KBEndpoint.kt`, `kb/chat/LockedRuleService.kt` | Snapshot, evaluate unlocked, revalidate, apply once |
| Chat protocol | `server/.../kb/chat/ActionComment.kt`, `RuleConversation.kt`, `KBChatService.kt`, `ChatResponseEnricher.kt` | Structured draft/create/edit/retry actions and state |
| Gemini | `llm/src/main/kotlin/io/rippledown/llm/Gemini.kt` | Dedicated structured request adapter with bounded attempts |
| Case presentation | `common/.../model/caseview/ViewableCase.kt`, `DerivedValueInfo.kt`; `ui/.../interpretation/DerivedValueRows.kt`, `DerivedValueTooltip.kt` | Status/provenance DTOs and rendering |
| Export/import | `server/.../kb/export/DefinitionExporter.kt`, `KBExporter.kt`, `KBImporter.kt` | Definitions plus durable evaluation records |

The lock boundary is the largest change. Complete its tests before wiring real remote evaluation into any production
entry point. An evaluator injected into `RuleTree` must never be a hidden network caller.

## 1. Define the data and outcome contracts

**First failing tests:** new `AiBooleanTest` in `common`, plus expression serialization cases in existing
`ValueExpressionTest` and definition-store tests.

1. Add `AiBoolean`, `AiBooleanExample`, and `AiEvaluationProfile` as serializable Kotlin data types. Support exactly
   one source and Boolean output. Store the immutable revision token and explicit evaluator-policy versions.
2. Implement ID-based reference extraction/alignment and a readable `asText()`. Require the server evaluator for
   case evaluation; do not add a Gemini dependency to `common`.
3. Define server-side `ValueExpressionEvaluator`, immutable `EvaluationContext`, request key, and sealed outcomes
   `Value`, `NoInput`, `NeedsEvaluation`, and `Failed`. Preserve a local evaluator for existing expressions.
4. Define the shared status/provenance DTOs independently of SDK response types. Keep a valid false distinct from
   all no-value outcomes; canonical materialisation is `Boolean.toString()`.
5. Define definition validation separately from deserialization. Resolve primary sources exactly, check names and
   profile support, and reject blank criteria/conflicting duplicate labels before mutating a KB.

**Coverage:** new subtype round trip; old expression fixtures; renamed source/target; unsupported or missing source;
derived/comment source rejection; empty instructions; duplicate conflicting versus identical examples; empty
episode list; stable revision across draft/preview/commit; no shared-code transport dependency.

**Exit:** the representation and validation are reviewable without a network client or live model call.

## 2. Persist and retrieve stable evaluation records

**First failing tests:** new in-memory store contract tests for round trip and insert-if-absent, then Postgres tests
using the same behavioural contract.

1. Add `AiEvaluationRecord`, canonical fingerprint construction, and `AiEvaluationStore`. Use SHA-256 over a specified,
   versioned canonical representation; preserve exact nonblank input bytes. Exclude display names from identity.
2. Add the store to `PersistentKB`, `InMemoryKB`, and `PostgresKB`. Introduce a KB-local evaluation table with a unique
   request key, Boolean, nullable evidence, profile/definition snapshot, input hash, timestamp, and model metadata.
3. Implement atomic insert-if-absent returning the stored winner. A result becomes authoritative only after durable
   persistence succeeds. Load records lazily by key; do not copy full clinical text into the record.
4. Add a service-local map of in-flight requests keyed by request identity. Make completion, exception, timeout, and
   cancellation release the entry correctly; one cancelled waiter must not cancel another user's needed result.
5. Define per-operation failure memoisation and a short retry cooldown. Keep successful records independent of case
   lifetime and never expire them automatically. KB deletion owns cleanup of all records.

**Coverage:** reload after store recreation; distinct definitions/inputs/KBs; semantic/profile/prompt-version edits
change identity; rename does not; whitespace-only input bypasses key/evaluation; nonblank whitespace is preserved;
concurrent duplicates use one winner; conflicting duplicate writes cannot change a result; store failure is explicit;
an error is never a false or successful record; in-flight entries do not leak after failure.

**Exit:** stable results survive process restart, and concurrent consumers agree on one record.

## 3. Add a strict Gemini Boolean classifier adapter

**First failing tests:** new `GeminiBooleanClassifierTest` in `llm`, using an injectable fake transport and no API key.

1. Introduce a dedicated structured-generation adapter alongside `generateText`; keep chat, transcription, and report
   generation behaviour unchanged. Accept the stored profile explicitly rather than using a changing global model.
2. Build the versioned system contract, approved criteria/examples, and separately encoded input message. Supply no
   tools or whole-case payload. Keep credentials in server configuration and raw notes out of logs.
3. Request the JSON schema with required Boolean `value` and nullable string `evidence`. Decode strictly into a typed
   DTO. Validate a nonnull excerpt against the original note. Distinguish refusal, invalid response, unavailable model,
   transport failure, rate limit, and input-limit errors.
4. Implement the design's per-attempt and total deadlines with a testable clock/backoff policy. Do not stack generic
   long retries around this adapter. Apply two attempts at most and retry only eligible transient failures.
5. Capture requested and returned model metadata where the installed SDK supports it. Confirm the exact SDK schema
   API against the installed dependency/official documentation while implementing; do not assume method names.

**Coverage:** true/false/null evidence; invalid Boolean types; missing/extra fields; invalid JSON; empty response;
fabricated excerpt; refusal; oversized input without truncation; transient retry success/exhaustion; total deadline
including backoff; cancellation; selected model/configuration propagation; no tools or case identifiers in requests.

**Exit:** a transport response can produce only a validated result or a typed failure; deterministic tests do not
contact Gemini. These tests do not claim to establish classification accuracy.

## 4. Make the inference engine consume prepared results

**First failing tests:** extend `RuleTreeFixpointTest` with a counted fake evaluator; add `AiExpressionEvaluatorTest`.

1. Thread an evaluation context/evaluator through `RuleTree.apply` and `materialise`. Resolve `ByDefinition` first;
   evaluate only selected assignments. Keep backward-compatible defaults for local-expression tests/callers.
2. Handle missing/blank source and no-episode cases before any request lookup. Use the latest episode only.
3. Return a typed demand when a result is unavailable. End the speculative attempt instead of treating demand as
   absent or permitting a partial fixpoint to be published. Return incomplete status on a failed evaluation.
4. Reuse the immutable prepared result through all passes. The engine itself performs no persistence/network I/O.
5. Keep source references in the dependency graph and preserve existing cycle checks. Permit downstream conditions,
   comments, and formulas on canonical true/false values as existing expression capabilities allow.
6. Audit display paths that call expression evaluation directly; views consume evaluated values/provenance and
   `asText()`, not the classifier. Make incomplete interpretation unrepresentable as a normal successful result.

**Coverage:** no requests for no-input, inactive, retracted, or overridden assignments on a pass; later-pass activation;
multiple passes sharing a result; true and false driving different downstream rules; absent-source conditions;
failed/needed evaluation never firing absence-based conclusions; literal overrides and removals; no stale derived
value after source removal; formula/comment regression tests; cycle rejection and rename alignment.

**Exit:** the complete deterministic engine can be tested using prepared data without external dependencies.

## 5. Introduce snapshot preparation and unlocked evaluation

**First failing tests:** new `AiInferenceCoordinatorTest` with latches/fake futures; extend
`KBEndpointConcurrencyTest` to prove another same-KB operation completes while classification is blocked.

1. Add interpretation-state revisions to `KBSession` and draft/session revisions to user-owned state. Inventory
   mutations affecting cases, rule trees, definitions, cornerstones, and session conditions; cover every invalidation
   point. Presentation changes should not unnecessarily invalidate evaluation identity.
2. Implement immutable/deeply isolated snapshots. Audit `RDRCase`, `Interpretation`, attributes, `RuleTree.copy()`, and
   pending-definition overlays. A speculative run must not modify stored cases or another user's session.
3. Implement `AiInferenceCoordinator`: snapshot under lock, pure calculation outside lock, resolve demands through
   the evaluation service outside lock, then validate revisions and publish/apply under lock.
4. Ensure persisted record loading/writes, provider calls, backoff, and coroutine waits happen outside the KB lock.
   Share identical in-flight evaluations across callers and keep the rule engine synchronous.
5. On stale state, discard the prepared application and restart from a new snapshot within a bounded restart budget.
   Reuse cache entries only when their keys still match. On cancellation/deletion, suppress late publication.
6. Separate prepare/apply APIs for mutating operations. Do not wrap an existing stateful method in a retry loop.
   Short application phases may verify prepared calculations against the live state, but may not call Gemini.

**Coverage:** same/different KB concurrent calls; notes/definitions/rules changed during a request; session cancelled
or changed; case/KB deleted; stale late response; monotonic revision checks; isolated preview mutations; bounded
restart exhaustion; one cancelled waiter among several; failures leave no partially applied operation or push.

**Exit:** no production path to the provider exists under `KBSession.locked` or a manager's KB lock.

## 6. Cover every inference entry point and failure response

**First failing tests:** extend `KBDefinitionResolutionTest`, `InterpreterTest`, `KBEndpointTest`, and new incomplete
interpretation route tests with deterministic classifiers.

1. Split `KB.processCase` ingestion/storage from interpretation preparation so accepted primary data is retained
   even if evaluation fails. Preserve external attribute collision handling and existing successful payloads.
2. Route `KBEndpoint.case`, `viewableCase`, ingestion, and report preparation through the coordinator. Update Ktor
   routes and service signatures where suspension is needed; do not introduce `runBlocking` under a KB lock.
3. Add typed incomplete responses/status, including case ID and diagnostic category. Specify and test the wire
   contract in the API constants/DTOs; an interpretation-request failure uses a non-success response with the retained
   case ID, while view retrieval can display data plus an explicit incomplete status.
4. Keep the old success response shape for successful interpretation. Expose no new derived values in the laboratory
   data feed. No-input cases remain successful and retain normal absence semantics.
5. Block report generation from incomplete interpretations and prevent the existing report cache returning an older
   report as current after a failed new interpretation. Update relevant client error handling and refresh behaviour.

**Coverage:** failure after primary data saved; retry the retained case without duplicate ingestion; valid false;
missing input; no stale true after note edits; successful no-AI compatibility; stale report suppression; view DTO
status serialization; external data ownership and collisions.

**Exit:** the feature works through real server boundaries with a fake classifier, not just directly in `RuleTree`.

## 7. Integrate rule sessions, previews, and cornerstones

**First failing tests:** extend `AssignValueDefinitionFlowTest`, `RuleBuildingSessionDerivedValuesTest`,
`RuleSessionManagerPendingChangeTest`, and `StaleRuleSessionTest`.

1. Add an overload/internal entry accepting a validated `ValueExpression` instead of an expression string, keeping
   the existing arithmetic/literal parser unchanged. Preserve pending definitions until commit.
2. Make session preparation use the coordinator for the session case and each relevant cornerstone, evaluating
   original and candidate trees with their correct definition resolvers and common result records.
3. Prepare changes before installing a session or changing its conditions. Preview results belong to the draft;
   they do not materialise on the live case. No-input preview is allowed, failed preview is shown explicitly.
4. Add a bounded, cancellable cornerstone operation budget and bounded concurrency. A timeout/failure leaves review
   incomplete and cannot automatically exempt a cornerstone or complete the session. Report progress through the
   existing workflow rather than holding the KB lock.
5. Apply the same preparation to condition suggestions and commit revalidation. Preserve checks for applicability,
   newly conflicting cornerstones, unchanged pending definitions, and dependency cycles immediately before commit.
6. Ensure atomic visibility of the definition/rule to other users; a failure during persistence cannot leave a live
   active assignment referring to an uncommitted definition. Record rules and send completion pushes exactly once.
7. Implement safe cancellation/name reuse for AI drafts. Account for the known cancelled-derived-name bug in TODO:
   avoid creating the live attribute until needed, or release only an unused draft reservation owned by this session.
   Test two users competing for a name; never delete another user's attribute or definition.
8. Add global AI-definition edits with new revisions and existing cycle/session validation; do not run a hidden global
   cornerstone review. Retain normal literal overrides, removals, undo behaviour, and conditional exceptions.

**Coverage:** true/false/missing/error previews; draft versus committed resolver; cancelled preview cannot activate a
definition; old/new trees share matching evaluations; an edited definition gets a new result; failed cornerstone
evaluation blocks commit; literal overrides remain intact; concurrent new cornerstone and definition change;
mutation/push exactly once after preparation retries; successful cancellation and name reuse.

**Exit:** rule building and correction are as reproducible as ordinary interpretation, with no network under lock.

## 8. Add chat authoring, editing, and retry

**First failing tests:** extend `ActionCommentTest`, add action tests, and extend the conversation-state tests with
scripted responses. These are server unit tests, not live chat calls.

1. Add structured fields/DTOs for source, instructions, and labelled examples to `ActionComment`; update its explicit
   constructor argument mapping. Add constants and actions such as `ProposeAiDerivedAttribute`,
   `EditAiDerivedAttributeDefinition`, and `RetryAiEvaluation`. Exact names can follow the established action naming.
2. Keep profile selection and revisions server-owned. A proposed action does not directly save an arbitrary model's
   output as a definition or bypass validation and the rule workflow.
3. Add per-user draft state with exact acceptance/correction/cancellation semantics. Source-name correction uses
   a server-owned offer. A bare acceptance is meaningful only for the current displayed draft.
4. Extend `RuleService`, its locking/orchestration wrapper, and action handlers. Register new session-starting actions
   in `ChatResponseEnricher` and any `RuleConversation` logic that assumes only `ASSIGN_DERIVED_VALUE` starts one.
5. Update `KBChatService` instruction substitutions and the assignment/edit instruction files. Teach the chat to
   recognise an AI-evaluation request, clarify missing semantics, preserve examples, and avoid inventing a formula
   or recording the current classification as a permanent literal.
6. Present definition, example labels, and the explicit current-case preview. Honour already-supplied choices; the
   complete diabetes request must not elicit redundant questions. Link a reply to the draft shown, even if the
   conversation or case changes while evaluation is in flight.
7. Support retrying failed evaluation and explaining an existing result from provenance. Explanation reads the saved
   evidence and definition; it does not reclassify the case. Successful records remain stable until a new revision.

**Coverage:** full one-turn specification; missing/ambiguous source; partial clarification; conflicting examples;
already-specified exclusions; no selected case; no notes on selected case; accepting/revising/cancelling the exact
draft; global edit versus conditional override; retry; name clash; two users; stale acceptance after context change.

**Exit:** the example feature can be created and revised entirely through chat with deterministic server tests.

## 9. Present status and provenance in existing panels

**First failing tests:** shared DTO/serialization tests and pure view-mapping tests before UI work. Add targeted UI
tests for rows/tooltips, running them only within the owner's desktop-test limits.

1. Extend view data with typed evaluation status and optional provenance. Update `ViewableCaseSerializer` and any
   interpretation serializer explicitly, preserving defaults for older payloads. Never encode status text as a value.
2. Keep `DerivedValueInfo` and server view construction able to represent no-input and failure even without a
   materialised attribute value. Make the case-level incomplete state visible alongside any diagnostic row.
3. Render canonical true/false normally; show no-input and failure text distinctly. Extend tooltips with source,
   approved criteria, excerpt, and evaluation time. Retain existing rule-condition information and pending-definition
   styling. No new permanent GUI controls are required.
4. Prevent views/tooltips/recomposition from initiating requests. Retry and definition changes use chat actions.

**Coverage:** all statuses, null evidence, renamed attributes, old payloads, incomplete case without an assignment,
pending versus committed display, no stale values, and repeated rendering causing zero additional evaluations.

**Exit:** users can distinguish a valid negative classification from missing input and a service failure.

## 10. Complete export/import and compatibility

**First failing tests:** extend `KBExporterTest`/`KBImporterTest` with AI definitions, records, and a fake classifier
that fails if called during import or on a cached post-import interpretation.

1. Extend definition serialization/import alignment for `AiBoolean`.
2. Add an evaluation-record export directory and import handling. Validate revisions, canonical fingerprints,
   source IDs, profile-policy versions, and duplicate-key consistency before creating/loading persistent state.
3. Preserve record timestamps/provenance and attach records to the destination KB store; never share a runtime cache
   across KBs just because imported identifiers match. Support exports with no evaluations directory.
4. Preserve records needed for historical provenance and staged-preview records without activating orphaned drafts.
   Unsupported expression/policy versions produce a clear error, not a literal fallback or silent model change.
5. Confirm KB deletion removes evaluation data and that imported definitions without a matching record evaluate
   only when subsequently needed. Check no-AI existing exports and restart behaviour as regressions.

**Coverage:** export/import with identical downstream interpretation and zero new provider requests; old exports;
missing records; corrupt hash; conflicting duplicates; unsupported policy; renamed source alignment; isolated KBs;
database recreation; definition revision and literal override persistence.

**Exit:** restart and export/import preserve the meaning and already-observed classifications of cornerstone cases.

## 11. Acceptance scenarios, model evaluation, and documentation

1. Add `cucumber/src/test/resources/requirements/inferencing/AI derived attribute.feature`, with steps in the existing
   derived-value step definitions or a focused new file. Use a deterministic classifier fixture for the evaluation
   part, clearly separate from any real authoring-model calls in the existing harness.
2. Cover creation via chat, true and false values, missing notes with zero evaluation calls, downstream comments,
   revision after editing, cancellation/name reuse, failure/retry, and a conditioned literal override. Add server-level
   acceptance for restart/export and concurrency rather than multiplying desktop scenarios.
3. Compile the Cucumber sources and run the dry-run binding check. Run one selected desktop scenario only when
   appropriate; ask before scheduling more than one UI unit test or Cucumber test. Record exactly what was run.
4. Add an opt-in real-Gemini evaluation harness/corpus using synthetic or explicitly approved notes. Include all six
   requested examples and the design's longer/negative/adversarial cases. Keep labelled examples used in the prompt
   separate from held-out examples that measure generalisation.
5. Record expected/actual labels, disagreements, elapsed time, profile/revision, and returned model version. Repeat a
   representative subset to expose variability, bypassing the production cache only within the harness. Never write
   evaluation results back into a production KB. Do not describe mocked fixture success as model accuracy.
6. Require all six specified examples to pass and have the owner review extended-corpus disagreements before adopting
   a profile. Agree a quantitative target if needed; do not invent an acceptable clinical error rate.
7. Update `requirements/derived_attributes.md`, `design/derived_attribute_definitions.md`, `rule_tree_and_inference.md`,
   `chat_architecture.md`, `concurrent_users.md`, `previewing_pending_changes.md`, and persistence/report documentation
   for the implemented behaviour. The README's description of the LLM's role must include runtime classification.
   Move the completed design from tickets into design, update links, and remove its backlog entry when it lands.

## Suggested focused commands

These are examples for the later implementation, not commands executed while writing this plan. New class names
must match the classes actually introduced. Run test tasks separately so a module's filter cannot affect another.

```powershell
.\gradlew.bat :common:test --tests "io.rippledown.model.rule.AiBooleanTest" --console=plain
.\gradlew.bat :llm:test --tests "io.rippledown.llm.GeminiBooleanClassifierTest" --console=plain
.\gradlew.bat :server:cleanTest :server:test --tests "io.rippledown.model.rule.RuleTreeFixpointTest" --console=plain
.\gradlew.bat :server:cleanTest :server:test --tests "io.rippledown.kb.ai.AiInferenceCoordinatorTest" --console=plain
.\gradlew.bat :cucumber:compileTestKotlin --console=plain
.\gradlew.bat :cucumber:cucumberDryRun --console=plain
```

After focused tests pass, run the affected non-UI regression classes, then the applicable broader non-UI package
checks from the repository's running-tests guidance. Filter out unavailable database/live-provider tests explicitly
by selecting known offline packages/classes. Inspect relevant Kover coverage where configured; add tests for missing
behaviour, not simply to colour lines. Report unavailable checks and pending scheduled UI/Cucumber runs honestly.

## Completion checklist

- [ ] The user's six examples have the required labels in the saved definition and real-model evaluation corpus.
- [ ] Chat creates/edits the definition without requiring a supplied algorithm or a new editor.
- [ ] Missing/blank latest-episode input produces no assignment and no Gemini request.
- [ ] A valid false, no input, and evaluation failure remain distinct through persistence, inference, and UI/API.
- [ ] Identical input/revision is stable across passes, users, restarts, and export/import.
- [ ] Interpretation, previews, cornerstones, suggestions, and commits use the same evaluator/result contract.
- [ ] Provider work, retry waits, and evaluation-record I/O never hold the KB lock.
- [ ] Stale results, failed reviews, and cancelled drafts cannot publish values or commit rules.
- [ ] Normal formulas, comment templates, literal overrides, external data ownership, and cycle checks still work.
- [ ] Unit and model-quality results are reported separately; desktop tests respect the owner's scheduling limits.
- [ ] Documentation describes the delivered behaviour; no commits have been made without owner review.
