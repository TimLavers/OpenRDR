# Implement case search by conditions
Implements `documentation/requirements/search_cases_by_condition.md` against the cukes in `cucumber/src/test/resources/requirements/cases/Search cases by conditions.feature` (15 active scenarios, 1 `@ignore`d). TDD throughout; gates at the end of each stage; live cucumber runs are Tim's.
## Not yet covered by cukes
Things from the requirements with no scenario yet, to be cuked during the work (Stage 6) or explicitly left to unit tests:
* **Cancellation mid-search** — the `@ignore`d scenario; becomes deterministically cukable using your delayed-predicate trick (see Design).
* **Search lifetime**: a search continuing after the user closes the KB and opens another; the results being in the destination list when they return.
* **The report being dropped** when no one is on the searched KB when it completes (including the user being in another KB's chat).
* **No concurrent searches within a single KB** (second search refused) — and the implied converse, that searches in different KBs may overlap.
* **Rule building refused while a search is in progress** (the reverse direction is cuked).
* **KB deleted mid-search**: search terminated, found cases lost with the KB. Cuking adds little over unit tests (everything observable disappears with the KB); unit tests cover it, and a simple scenario can be added if cheap.
* **Conditions on derived attributes** — raw-data and comment conditions are cuked, derived-attribute conditions are not (same code path; unit-tested).
* **The formal form of conditions in the report** — deliberately not pinned in the existing cukes (wording unknown until the parser runs); asserted loosely via unit tests.
* **"No history kept" after abandoning the setup** — not directly assertable; the abandon-then-chat-works scenario is as close as cukes get.
## Design
### Chat flow: one action, model-driven prompting
A new prompt section (`26_searching_for_cases.md`) instructs the model to collect the five parameters — conditions, and/or connector, how many (a number or "all"), which lists, destination — by prompts or from one message; to **pre-check before summarising** that every condition names a known attribute (the attribute list is already in the prompt) and the destination is not a built-in list name, refusing with "Cannot …" if not; then to show a summary and ask for confirmation; and, on confirmation, to emit one action with all parameters. Condition texts and the destination are passed **verbatim**; no model-side parsing.
On the server, `SearchCases` re-validates deterministically (defence in depth): it parses each condition text and refuses unknown attributes — so a model slip still cannot run a bad search, though the refusal would then arrive after confirmation (the cukes expect the pre-check path).
`StopCaseSearch` is emitted when the user asks to stop; it cancels the KB's running search and responds "The search has been terminated."
### Search execution
* **`CaseSearchSpec`** (server, `kb/search/`): parsed conditions (user text + `Condition`), connector (AND/OR), limit (`Int?`, null = no limit), resolved source lists, destination name.
* **Parsing** reuses the existing pipeline: the same `ConditionGenerator`-backed parser used by `RuleSessionManager.conditionForExpression` (RuleSessionManager.kt:143), but with an existing-attributes-only resolver (`attributeManager.byName`, not `getOrCreate`), no case-level validation (conditions need not hold any case, and no case need be selected), and no rule session. Unparseable/unknown-attribute conditions are refused, echoing the offending text.
* **`CaseSearcher`**: snapshot the case ids of the selected lists; for each case, materialise it (`kb.ruleTree.materialise`) **only if** some condition touches a KB-assigned (comment or derived) attribute, otherwise evaluate on the raw case; evaluate conditions and combine per the connector; on a match, copy the case to the destination **as found** via the existing `kb.copyCaseToList` (which already gives case-insensitive destination resolution, merging into an existing list, built-in-name refusal, and no empty list when nothing is found). Copying as found is what makes cancellation retain the cases found so far, and is why result order is unspecified. Stop at the limit, when the lists are exhausted, or when cancelled; check cancellation between cases.
* Source list names resolve case-insensitively to `Processed`, `Cornerstone`, or `CaseListType(name)`; an unknown list name is refused.
### Registry, lifetime, exclusions, reporting
* **`SearchCoordinator`** (server, application-level, like `ChatCoordinator`): running searches keyed by KB id. Starting refuses if a search is already running for that KB (per-KB concurrency) or a rule session is active in it ("Please finish or cancel the current rule before searching for cases."). It holds the KB directly, not the session, so a search survives the KB being closed or switched away from.
* `RuleSessionManager.startRuleSession` gains the mirror check: refuse while a search is running for the KB ("Please finish or cancel the current search before building a rule.").
* KB deletion (`KnowledgeBaseService.delete` path, which already notifies `ChatCoordinator.knowledgeBaseDeleted`) also cancels that KB's searches; the found cases vanish with the KB.
* **Reporting**: on completion (or cancellation), build the report — number found, lists searched, conditions in both user and formal forms (`condition.asText()`), destination, terminated state — and push it to the users currently on the searched KB (`ChatCoordinator.usersOn(kbId)`) over the websocket with a new `SEARCH_REPORT_PREFIX`, in the style of `sendChatContext`. Nobody on the KB ⇒ dropped, which satisfies "will not see the report". The `StopCaseSearch` action's own response states termination immediately.
* Each result copy pushes `CasesInfo` as `copyCaseToList` does today, so the UI's lists update live.
### Chat plumbing
* `ActionComment` gains fields `searchConditions: List<String>?`, `connector: String?`, `limit: Int?`, `searchLists: List<String>?`, `destination: String?`, wired into `invokeConstructor` (the `suggestions: List<String>?` field is the precedent for a list).
* Constants `SEARCH_CASES = "SearchCases"`, `STOP_CASE_SEARCH = "StopCaseSearch"` (values = class simple names for the reflective lookup), prompt variables, and a "Searching" capability entry in `ListCapabilities`.
* `RuleService` gains `searchCases(conditions, connector, limit, lists, destination)` and `stopCaseSearch()`, delegating to the coordinator; `SearchCases` does not require a current case.
### UI
* `WebSocketApi.startSession` handles `SEARCH_REPORT_PREFIX` with a new callback; `OpenRDRUI` appends the report to the chat as a bot message, through the same state that REST chat responses feed.
### Test-only delayed predicate (your trick)
A `GreaterThanButWithDelay(value, delayMillis = 500)` predicate in `common` (in `episodic/predicate/`, delegating to the ordinary greater-than comparison with a sleep), recognised by a **deterministic pre-parse** in the search condition parser for the phrase "`<attribute> is greater than but with delay <value>`". The dumb name means no user or suggestion will ever produce it; only the search path recognises it. This makes cancellation, concurrency and lifetime scenarios slow enough to interrupt deterministically.
## Stages
### Stage 1: Common — delayed predicate
TDD: failing tests for `GreaterThanButWithDelay` (same result as greater-than; sleeps) in `common`; then the predicate. Gate: `gradle :common:test`.
### Stage 2: Server core — parsing and evaluation
TDD in `KBTest`/new `CaseSearcherTest` (InMemoryKB): parse each condition text (existing-attribute resolver, refusal echoing unknown attributes, delayed-predicate pre-parse); resolve list names; and/or evaluation; limit stops early; exhaustion; comment conditions evaluate against materialised cases; copies land in the destination via `copyCaseToList`; cancellation between cases retains found cases (using a short-delay predicate). Gate: `gradle :server:test`.
### Stage 3: Server — coordinator, exclusions, deletion hook
TDD (new `SearchCoordinatorTest` + `RuleSessionManagerTest` additions): per-KB single search; second search refused; search refused during a rule session; rule session refused during a search; cancellation; completion report content; report routing via `usersOn`; deletion cancels. Gate: `gradle :server:test`.
### Stage 4: Server — chat layer
TDD: `SearchCases`/`StopCaseSearch` actions (happy path incl. no current case; refusals surface server messages verbatim); `ActionComment` fields; constants; `RuleService` methods; prompt section `26_searching_for_cases.md` + `KBChatService` wiring (sections list, variables, caseless section handling); `ListCapabilities` addition. Gate: `gradle :server:test`.
### Stage 5: Report push — server and UI
TDD: `WebSocketManager.sendSearchReport` (server); `WebSocketApi` prefix handling and `OpenRDRUI` appending the report (ui unit tests — at most one run without asking). Gate: `gradle :server:test`, `gradle :ui:compileTestKotlin` (+ one UI test).
### Stage 6: Cucumber — the not-yet-cuked list
Un-`@ignore` and rewrite the cancellation scenario with the delayed predicate (assert termination response; destination list shown with fewer than all cases — add a `the user-defined {word} case count should be less than {int}` step if needed). Add scenarios: second search refused; rule building refused during a search; search continues across closing and re-opening the KB (results present on return, report never seen); searches in different KBs may overlap. Update the feature header comment (no longer "not implemented"). Gates: `gradle :cucumber:compileTestKotlin`, `gradle :cucumber:cucumberDryRun`; live runs (all 20+ scenarios) on Tim's signal.
### Stage 7: Verification and docs
Run the existing 15 scenarios live (Tim's go-ahead); fix what they surface (the model pre-check wording for refusals is the likely tender spot — the cukes expect "Cannot … Processed" without a confirmation step). Update `documentation/requirements/search_cases_by_condition.md` to note the cukes file; final gates in order: `:common:test`, `:server:test`, `:ui:test` (scheduled with Tim), `:cucumber:compileTestKotlin`, `:cucumber:cucumberDryRun`.
