# Demonstration knowledge bases: implementation plan

This is the step-by-step plan for `demonstration_knowledge_bases.md`. Read that document first; it says *why*. This
one says *what to type*. Do the steps in order. Each step ends green (`.\gradlew.bat :common:test :server:test` with
the filters in `.windsurf/rules/running-tests.md`), and each step is a sensible commit.

Review status: Steps 4–8 have been reviewed. Corrections cover greeting order and formatting, horizontal chip scrolling,
the explicit default-KB helper, old list expectations and UTF-8 damage from the Step 8 bulk feature edit. Steps 9 and 10
are implemented. Common and filtered server unit tests and Kover checks pass, as do the focused scrolling regression,
eight mock-based cucumber helper tests, cucumber compilation and dry run (206 scenarios, 2,635 bound steps).
The real `:cucumber:kb` run remains for the user to schedule; broader UI tests require approval under `AGENTS.md`.

## Ground rules for whoever implements this

Subsequent usability change: the grouped vertical list in `demonstration_knowledge_bases.md` supersedes Step 7's
separate horizontal chips. The response now carries `kbListing: KnowledgeBaseListing?`, containing stored names,
demonstration names and the open name, instead of flat `kbChoices`. The list itself is clickable and renders once.

- Follow `.windsurf/rules/*.md`. In particular: test first; no `!!` in production code; do not add comments; do not
  commit — the user commits.
- Do not invent new mechanisms. Every piece of this reuses something that exists: `createKBFromSample`,
  `PendingKbCreation`, `KbManagementOutcome.Ask`, `SuggestionListRow`, `WebSocketManager.sendKbInfo`.
- When a step says "constant" it means a `const val` or a small `fun` in
  `common/src/main/kotlin/io/rippledown/constants/chat/Constants.kt`, next to the existing knowledge base messages
  (lines ~99–160). Cukes match on these strings, so the exact wording matters.
- When a step names a test file that exists, add tests to it; when it names one that does not, create it in the same
  package as the class under test.
- Test base classes to use: `io.rippledown.kb.chat.action.KbActionTestBase` (mockk `kbService`, `thyroids` /
  `glucose` / `scratch` fixtures, `text()`, `asAsk()`, `accept()` helpers) for actions;
  `io.rippledown.kb.chat.FirstKbCreationTest` shows how a `ChatManager` naming-flow test is written (mock
  `ConversationService`, canned interpreter JSON).
- After each step run the Kover report for the touched classes (see running-tests) and read the uncovered lines.

---

## Step 1 — Rename the DEMO sample to PATHOLOGY

**Files**

- `common/src/main/kotlin/io/rippledown/sample/SampleKB.kt`
- `common/src/test/kotlin/io/rippledown/sample/SampleKBTest.kt`
- `server/src/main/kotlin/io/rippledown/kb/sample/SampleKBLoader.kt` (the `DEMO ->` branch)
- `server/src/main/kotlin/io/rippledown/kb/sample/demo/DemoSampleBuilder.kt` (KDoc reference only)

**Test first** (`SampleKBTest`)

- `entries` still has 7 members; the last is `PATHOLOGY`; `PATHOLOGY.title() shouldBe "Pathology"`.
- New test `demonstrations`: `SampleKB.demonstrations() shouldBe listOf(TSH, CONTACT_LENSES, ZOO, PATHOLOGY)`.

**Implement**

- Rename the enum constant `DEMO` → `PATHOLOGY`, title `"Pathology"`.
- Add to the enum a companion:

```kotlin
companion object {
    fun demonstrations(): List<SampleKB> = listOf(TSH, CONTACT_LENSES, ZOO, PATHOLOGY)
}
```

- Fix every compile error (`SampleKBLoader`, any test using `SampleKB.DEMO`). Leave `DemoSampleBuilder`'s class name
  alone.

**Check**: `.\gradlew.bat :common:test` and the server filtered tests.

---

## Step 2 — Name resolution knows about demonstrations

Status: complete. Common and filtered server tests pass; Kover coverage reviewed for the touched code.

**Files**

- `server/src/main/kotlin/io/rippledown/kb/KbNameResolution.kt`
- `server/src/test/kotlin/io/rippledown/kb/KbNameResolutionTest.kt`

**Test first** (`KbNameResolutionTest`). Use `demos = SampleKB.demonstrations()` and stored `listOf(thyroids, glucose)`.

| Input                                        | Stored                              | Expected                                                                                                                                         |
|----------------------------------------------|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `"Zoo Animals"`                              | thyroids, glucose                   | `Demonstration(ZOO)`                                                                                                                             |
| `"zoo animals"`                              | thyroids, glucose                   | `Demonstration(ZOO)` (case-insensitive)                                                                                                          |
| `"Zoo"`                                      | thyroids, glucose                   | `Demonstration(ZOO)` (unique partial)                                                                                                            |
| `"Thyroid"`                                  | thyroids, glucose                   | `Partial(thyroids)` — stored partial beats demo                                                                                                  |
| `"Thyroid Stimulating Hormone"`              | thyroids, glucose                   | `Demonstration(TSH)` — exact demo beats stored partial                                                                                           |
| `"Pathology"`                                | a stored KB "Pathology"             | `Exact(stored)` — stored exact beats demo exact (cannot normally happen because titles are reserved, but resolution must still be deterministic) |
| `"o"`                                        | thyroids, glucose                   | `Ambiguous` with candidates from stored **and** demo titles, sorted                                                                              |
| `"Nothing"`                                  | thyroids, glucose                   | `NotFound("Nothing", ["Glucose","Thyroids"], demoTitlesSorted)`                                                                                  |
| `"Zoo Animals"`                              | any, `demonstrations = emptyList()` | `NotFound` (old behaviour preserved)                                                                                                             |
| `isDemonstrationTitle("zoo animals", demos)` | —                                   | `true`; `"Zoo"` → `false`; `" Pathology "` → `true`                                                                                              |

**Implement**

- Add `data class Demonstration(val sample: SampleKB) : KbResolution()`.
- Add `val demonstrations: List<String> = emptyList()` as a third property of `NotFound`.
- Change the signature to
  `fun resolveKbName(name: String, kbInfos: Collection<KBInfo>, demonstrations: Collection<SampleKB> = emptyList()): KbResolution`.
  Order of checks:
    1. blank → `NotFound`.
    2. exact stored (existing code) → `Exact` / `Ambiguous`.
    3. exact demo title, ignore case → `Demonstration`.
    4. partial stored: 1 → `Partial`; >1 → `Ambiguous(wanted, storedMatches + demoMatches sorted)`.
    5. partial demo: 1 → `Demonstration`; >1 → `Ambiguous(wanted, demoMatches sorted)`.
    6. `NotFound(wanted, storedNamesSorted, demoTitlesSorted)`.
- Add `fun isDemonstrationTitle(name: String, demonstrations: Collection<SampleKB>): Boolean` =
  any title equals `name.trim()` ignoring case.
- Fix the `when` expressions over `KbResolution` that no longer compile: `OpenKnowledgeBase`, `DeleteKnowledgeBase`.
  For this step only, make them return `done(kbNotFoundMessage(...))` for `Demonstration`; Step 4 gives them real
  behaviour. Update `kbNotFoundMessage` to take `demonstrations: List<String>` and append
  `" The demonstration knowledge bases are: A, B."` when non-empty (constant
  `THE_DEMONSTRATION_KNOWLEDGE_BASES_ARE = "The demonstration knowledge bases are:"`).

---

## Step 3 — `KnowledgeBaseService` gains demonstrations

Status: complete. Common and filtered server tests pass; Kover coverage reviewed for the touched code.

**Files**

- `server/src/main/kotlin/io/rippledown/kb/chat/KnowledgeBaseService.kt`
- `server/src/main/kotlin/io/rippledown/server/ApplicationKbService.kt`
- `server/src/test/kotlin/io/rippledown/server/ApplicationKbServiceTest.kt`
- `server/src/main/kotlin/io/rippledown/server/ServerApplication.kt` (no change expected; `createKBFromSample` exists)

**Test first** (`ApplicationKbServiceTest`)

- `demonstrations()` returns `SampleKB.demonstrations()`.
- `resolve("Zoo Animals")` returns `Demonstration(ZOO)` when no stored KB has that name.
- `isDemonstrationTitle("pathology")` is true, `"Thyroids"` false.
- `createFromSample("Zoo2", ZOO)`: returns a `KBInfo` named `Zoo2`; `app.kbList()` contains it; the endpoint for it
  has 101 processed cases; `webSocketManager.sendKbInfo(created)` was called (mockk `coVerify`).

**Implement**

- Interface additions:

```kotlin
fun demonstrations(): List<SampleKB>
fun isDemonstrationTitle(name: String): Boolean
suspend fun createFromSample(name: String, sample: SampleKB): KBInfo
```

- `ApplicationKbService`:
    - `demonstrations() = SampleKB.demonstrations()`
    - `resolve(name) = resolveKbName(name, knowledgeBases(), demonstrations())`
    - `isDemonstrationTitle(name) = isDemonstrationTitle(name, demonstrations())` (the top-level function from Step 2)
    - `createFromSample`: `application.createKBFromSample(name, sample)` then `webSocketManager.sendKbInfo(created)`;
      return `created`. Mirror `create`.
- Every other `KnowledgeBaseService` implementation or mockk in tests: mockk is strict, so tests that now hit
  `isDemonstrationTitle` / `demonstrations` will fail with "no answer found" — stub them where needed
  (`every { kbService.isDemonstrationTitle(any()) } returns false`). Do not use `relaxed = true`.

---

## Step 4 — Actions

Status: complete. Common and filtered server tests pass; Kover coverage reviewed for the touched code.
The temporary question-only `AskForName` handling used for Step 4 has been replaced by the shared naming workflow in
Step 5.

### 4a. Shared new-name validation

**File**: new `server/src/main/kotlin/io/rippledown/kb/chat/action/NewKbName.kt`.

Move the body of `CreateKnowledgeBase.doIt` into a top-level function so the copy action can share it:

```kotlin
suspend fun outcomeForNewKbName(
    kbService: KnowledgeBaseService,
    rawName: String,
    create: suspend (KnowledgeBaseService, String) -> ChatResponse
): KbManagementOutcome {
    val name = rawName.trim()
    if (name.isEmpty()) return done(BLANK_NAME_MESSAGE)
    if (kbService.isDemonstrationTitle(name)) return done(kbNameReservedMessage(name))
    val existing = kbService.resolve(name)
    if (existing is KbResolution.Exact) return done(kbAlreadyExistsMessage(existing.kbInfo.name))
    val nearDuplicate = kbService.nearDuplicateOf(name)
    if (nearDuplicate != null) {
        return KbManagementOutcome.Ask(confirmKbCreateMessage(name, nearDuplicate.name)) { create(it, name) }
    }
    return KbManagementOutcome.Done(create(kbService, name))
}
```

Move `BLANK_NAME_MESSAGE` to `Constants.kt` (`KB_NAME_CANNOT_BE_BLANK` already exists with slightly different
wording; keep `BLANK_NAME_MESSAGE`'s text because `CreateKnowledgeBaseTest` asserts on it).

New constant:
`fun kbNameReservedMessage(name: String) = "\"$name\" is the name of a demonstration knowledge base; please choose another."`
and `const val KB_NAME_RESERVED = "is the name of a demonstration knowledge base"` for cukes.

### 4b. `CreateKnowledgeBase`

**Test first** (`CreateKnowledgeBaseTest`): `CreateKnowledgeBase("Zoo Animals")` with
`every { kbService.isDemonstrationTitle("Zoo Animals") } returns true` → `text()` is
`kbNameReservedMessage("Zoo Animals")`;
`kbService.create` is never called. Existing tests: add `every { kbService.isDemonstrationTitle(any()) } returns false`.

**Implement**: `doIt` becomes `outcomeForNewKbName(kbService, kbName) { service, name -> create(service, name) }`.

### 4c. `CopyDemonstrationKnowledgeBase` (new)

**File**: `server/src/main/kotlin/io/rippledown/kb/chat/action/CopyDemonstrationKnowledgeBase.kt`.

```kotlin
data class CopyDemonstrationKnowledgeBase(val sample: SampleKB, val kbName: String) : KbManagementAction {
    override suspend fun doIt(kbService: KnowledgeBaseService): KbManagementOutcome =
        outcomeForNewKbName(kbService, kbName) { service, name ->
            val created = service.createFromSample(name, sample)
            ChatResponse(kbCopiedFromDemonstrationMessage(created.name, sample.title()))
        }
}
```

Constant:
`fun kbCopiedFromDemonstrationMessage(name: String, title: String) = "Created \"$name\" from the $title demonstration and opened it."`
and `const val KB_COPIED_FROM_DEMONSTRATION = "demonstration and opened it"`.

`changesContext` stays `true` (default). This action is never emitted by the model, so nothing is added to
`ActionComment` or the prompt.

**Test first** (`CopyDemonstrationKnowledgeBaseTest`, extends `KbActionTestBase`): blank name; reserved name; exact
clash (`resolve` returns `Exact(thyroids)`); near duplicate → `asAsk()`, `accept()` calls `createFromSample`; happy
path calls `createFromSample("Zoo2", ZOO)` and returns the copied message.

### 4d. `OpenKnowledgeBase`

Add a new outcome type in `Action.kt`:

```kotlin
class AskForName(val question: String, val actionForName: (String) -> KbManagementAction) : KbManagementOutcome()
```

`OpenKnowledgeBase.doIt`, `Demonstration` branch:

```kotlin
is KbResolution.Demonstration -> KbManagementOutcome.AskForName(nameForDemonstrationCopyMessage(resolution.sample.title())) {
    CopyDemonstrationKnowledgeBase(resolution.sample, it)
}
```

Constant:
`fun nameForDemonstrationCopyMessage(title: String) = "You will get your own copy of the $title demonstration. $NAME_THE_NEW_KB"`
(`NAME_THE_NEW_KB` = "What would you like to call it?" exists). Cuke term: `"your own copy"`.

**Test first** (`OpenKnowledgeBaseTest`): `resolve("Zoo Animals")` returns `Demonstration(ZOO)` → outcome is
`AskForName`; its `question` is the message; `actionForName("Zoo2")` is `CopyDemonstrationKnowledgeBase(ZOO, "Zoo2")`;
`kbService.open` never called.

### 4e. `DeleteKnowledgeBase`

`Demonstration` branch → `done(cannotDeleteDemonstrationMessage(resolution.sample.title()))`.
Constant:
`fun cannotDeleteDemonstrationMessage(title: String) = "$title is a demonstration knowledge base and cannot be deleted. Your own copies can be."`
and `const val CANNOT_BE_DELETED = "cannot be deleted"`.

**Test first** (`DeleteKnowledgeBaseTest`): returns `Done` (not `Ask`) with that text; `delete` never called.

### 4f. `RenameKnowledgeBase`

After the blank check: `if (kbService.isDemonstrationTitle(newName)) return done(kbNameReservedMessage(newName))`.

**Test first** (`RenameKnowledgeBaseTest`): reserved name refused, `rename` never called. Stub
`isDemonstrationTitle` false in the existing tests.

### 4g. `ListKnowledgeBases`

Output text, exactly (blank line between sections; the cuke step `consists of the following lines` ignores blank
lines):

```
Your knowledge bases:
Thyroids (open)
Glucose

Demonstration knowledge bases (open one to get your own copy):
Contact Lens Prescription
Pathology
Thyroid Stimulating Hormone
Zoo Animals
```

With no stored KBs the first section is the single line `You have no knowledge bases of your own.`

Constants: `YOUR_KNOWLEDGE_BASES = "Your knowledge bases:"`,
`NO_KNOWLEDGE_BASES_OF_YOUR_OWN = "You have no knowledge bases of your own."`,
`DEMONSTRATION_KNOWLEDGE_BASES_HEADING = "Demonstration knowledge bases (open one to get your own copy):"`.
`NO_KNOWLEDGE_BASES` ("There are no knowledge bases.") stays for `kbNotFoundMessage`.

Stored names in the order `kbService.knowledgeBases()` gives (already sorted); demonstration titles sorted
alphabetically.

`ChatResponse` (common, `io.rippledown.model.chat.ChatResponse`) gains `val kbChoices: List<String> = emptyList()`.
`ListKnowledgeBases` returns `KbManagementOutcome.Done(ChatResponse(text, kbChoices = choices))` where `choices` =
stored names **excluding the open one**, then demonstration titles, same order as the text.

**Test first** (`ListKnowledgeBasesTest`): the exact text above for (thyroids open, glucose) and for no stored KBs;
`kbChoices` = `["Glucose", "Contact Lens Prescription", "Pathology", "Thyroid Stimulating Hormone", "Zoo Animals"]`;
with nothing open, `Thyroids` is included in `kbChoices` too. Also `ChatResponseTest` (common): `kbChoices` round-trips
through JSON and defaults to empty.

**Check** `ChatManager.processActionComment`: the `when` at lines ~195–204 rebuilds `ChatResponse` in two branches
(`ChatResponse(chatResponse.text, bufferedSuggestions, tip)`) and would drop `kbChoices`. Change those constructions to
`chatResponse.copy(suggestions = ..., tip = tip)` so `kbChoices` survives. Add a `ChatManagerTest` that a
`ListKnowledgeBases` action's `kbChoices` reach the returned response.

---

## Step 5 — `ChatManager`: one naming workflow for both create and copy

Status: complete. `PendingKbCreation` has `actionForName`; `manageKnowledgeBases` stores the naming
action from `AskForName`; `answerToKbCreation` uses `pending.actionForName` for `CONFIRM_WITH_NAME`
and re-asks the question on a bare yes during `AWAITING_NAME`. Existing first-KB tests unchanged;
`DemonstrationCopyNamingTest` covers the copy flow. Common and filtered server tests pass.

**Files**: `ChatManager.kt`, `FirstKbCreationTest.kt` (add tests), maybe new `DemonstrationCopyNamingTest.kt`.

**Implement**

- `PendingKbCreation` gains a third field:
  `val actionForName: (String) -> KbManagementAction = { CreateKnowledgeBase(it) }`.
- `manageKnowledgeBases`: add

```kotlin
is KbManagementOutcome.AskForName -> {
    pendingKbCreation = PendingKbCreation(KbCreationStage.AWAITING_NAME, outcome.question, outcome.actionForName)
    ChatResponse(outcome.question)
}
```

- `answerToKbCreation`:
    - `isAcceptance(message)` branch: if `pending.stage == OFFER_CREATION` behave as now (move to `AWAITING_NAME` with
      `NAME_THE_NEW_KB`, default action); if already `AWAITING_NAME`, keep `pending` and return
      `ChatResponse(pending.question)` (a "yes" is not a name, so ask again with the same question).
    - `CONFIRM` intent: same rule as above.
    - `CONFIRM_WITH_NAME`: `manageKnowledgeBases(pending.actionForName(checkNotNull(reply.kbName)))`.
    - `DENY`, `UNCLEAR`, `OTHER_REQUEST`: unchanged.
- `startConversation` already clears `pendingKbCreation`; nothing to do.

**Test first**

- Open a demonstration → `AskForName` → `pendingKbCreation` set: the next message `"Zoo2"` with the interpreter
  returning `CONFIRM_WITH_NAME` / `"Zoo2"` runs `CopyDemonstrationKnowledgeBase(ZOO, "Zoo2")` (verify
  `kbService.createFromSample("Zoo2", ZOO)` called) and clears the pending state.
- Same, but reply `"no"` → interpreter `DENY` → `KB_CREATION_DECLINED`, nothing created.
- Same, but reply `"yes"` → asks the same question again (`pending.question`), nothing created.
- Same, but reply `"open Thyroids"` → interpreter `OTHER_REQUEST` → falls through to the model (stub the
  conversation to return an `OpenKnowledgeBase` JSON) → `open(thyroids)` called.
- A rule session active + `OpenKnowledgeBase("Zoo Animals")` → `KB_ACTION_DURING_RULE_MESSAGE`, no pending state
  (already true through `changesContext`; assert it).
- The existing first-KB flow tests still pass unchanged.

---

## Step 6 — Greeting and prompt

**Files**

- `Constants.kt`: `noKbGreeting(available: List<String>, demonstrations: List<String>)`.
- `ChatCoordinator.greetingFor`: pass `kbService.demonstrations().map { it.title() }`.
- `KBChatService.systemPromptVariables` / `createKBChatService`: add parameter
  `demonstrationNames: List<String> = emptyList()`,
  variable `"DEMONSTRATION_KB_NAMES"` joined with `", "`.
- `ChatManagerFactory.create`: compute `demonstrationNames` and pass it through `caseLess` and `forCase`.
- `server/src/main/resources/chat/instructions/20_knowledge_base_management.md`.

**Greeting text**

- No stored KBs:
  `"There are no knowledge bases yet. Do you want to create one, or open a demonstration knowledge base? The demonstration knowledge bases are: Contact Lens Prescription, Pathology, Thyroid Stimulating Hormone, Zoo Animals."`
  Keep `NO_KBS_YET` as the first sentence so the existing cuke term still matches. A plain "yes" still means
  "create" (server-owned `OFFER_CREATION` flow, unchanged); "open Zoo" goes through the interpreter as
  `OTHER_REQUEST` to the model.
- Some stored KBs: existing text, then a new line
  `"The demonstration knowledge bases are: ..."` before `"Do you want to open one or create a new one?"`.

**Prompt** (`20_knowledge_base_management.md`): under the bullet list at the top add
`- The demonstration knowledge bases are: {{DEMONSTRATION_KB_NAMES}}`. Add a short section after "Opening a knowledge
base":

> ## Demonstration knowledge bases
> These are built in and cannot be changed or deleted. Opening one gives the user their own copy; the system asks for
> the copy's name. Use `{{OPEN_KNOWLEDGE_BASE}}` with the name exactly as the user gave it, as for any other knowledge
> base. If the user asks to delete one, still output `{{DELETE_KNOWLEDGE_BASE}}`; the system explains. Their names
> cannot be used for a new or renamed knowledge base; the system refuses, you do not.

**Tests**: `ChatCoordinatorTest` greeting strings; `KBChatServiceTest` that the placeholder is replaced (grep the
generated prompt for `Zoo Animals`); a `ConstantsTest` or existing greeting test for both `noKbGreeting` forms.

---

## Step 7 — UI: clickable knowledge base chips

**Files**

- `ui/src/main/kotlin/io/rippledown/chat/ChatPanel.kt`: new message type and row.
- `ui/src/main/kotlin/io/rippledown/chat/ChatController.kt`: map `response.kbChoices`.
- `ui/src/main/kotlin/io/rippledown/chat/ChatTestHook.kt`: expose the most recent choices (mirror suggestions).
- Tests: `ui/src/test/kotlin/io/rippledown/chat/KbChoiceRowTest.kt`, `ChatControllerTest` (if present; else
  `ChatPanelTest`).

**Implement**

- `data class KbChoiceListMessage(val names: List<String>) : ChatMessage { text = ""; isUser = false }`.
- `const val KB_CHOICE_ITEM = "KB_CHOICE_ITEM_"`.
- `@Composable fun KbChoiceRow(names: List<String>, index: Int, onChosen: (String) -> Unit)`: copy the layout of
  `SuggestionListRow` (horizontal scroll, chip per item, `pointerHoverIcon(Hand)`), **without** the numbering and the
  editable marker; each chip's `contentDescription = "$KB_CHOICE_ITEM$name"`.
- `ChatPanel`, in the `when (message)`:
  `is KbChoiceListMessage -> KbChoiceRow(message.names, index) { name -> if (sendIsEnabled) onMessageSent(UserMessage("Open $name")) }`.
  Sending goes through `onMessageSent`, the same path as typed text; nothing else.
- `ChatController` (line ~40): after the suggestions line,
  `if (response.kbChoices.isNotEmpty()) add(KbChoiceListMessage(response.kbChoices))`.
- `ChatTestHook.Snapshot`: add `mostRecentKbChoices: List<String>?`, filled from the last `KbChoiceListMessage`.

**Tests (one UI test at a time — ask before running several)**

- `KbChoiceRow` renders one chip per name with the right content description; clicking calls `onChosen(name)`.
- `ChatController` turns a response with `kbChoices` into `BotMessage` followed by `KbChoiceListMessage`.

---

## Step 8 — Cukes: explicit default knowledge base and no lazy default

### 8a. Server: delete the lazy default

- Delete `ServerApplication.getDefaultProject()` and its test in `ServerApplicationTest` (`get default project` — the
  behaviour is intentionally gone; say so in the summary).
- Delete the `get(DEFAULT_KB)` route in `server/.../routes/KBManagement.kt` and the `DEFAULT_KB` constant in
  `common/.../constants/api/Constants.kt`.
- `ui/.../main/Api.kt` `kbInfo()`: replace the fetch with
  `return checkNotNull(currentKB) { "No knowledge base is open." }`; drop `kbInfoMutex` if nothing else uses it.
  Remove the `DEFAULT_KB` branch from `ui/src/test/.../mocks/MockEngineDSL.kt` and the `defaultKB` /
  `defaultKbFetches` config it served; fix `ApiTest` accordingly.
- `DEFAULT_PROJECT_NAME` ("Thyroids") stays: the cukes use it.

### 8b. Cucumber step

- `cucumber/src/test/kotlin/steps/StepsInfrastructure.kt`: remove `uiTestBase.restClient.createKBWithDefaultName()`
  from `startServerWithInMemoryDatabase`.
- `cucumber/src/test/kotlin/steps/Defs.kt`: replace `there is a knowledge base called {word}` with

```kotlin
@Given("a default KB is opened")
fun aDefaultKbIsOpened() {
    restClient().createKBWithDefaultName()
}
```

(The client opens the first stored KB when it starts, so "opened" is what the scenario observes.)

### 8c. Feature files

For every `.feature` under `cucumber/src/test/resources/requirements/` **except** those listed below, add at the top
of the feature (after any tags and the `Feature:` line and its description):

```gherkin
  Background:
Given a default KB is opened
```

Exceptions:

- `samples/*.feature` — they create their own KB via the sample dialog and select it with the backdoor.
- Scenarios that begin `Given The Knowledge Base called Thyroids has been deleted` — remove that step; they now start
  with no KB. Both files that already have a `Background` (`kb/Knowledge Base Management.feature`,
  `kb/Knowledge Base management by chat.feature`) replace `there is a knowledge base called Thyroids` with the new
  step; in the `by chat` feature the "no knowledge bases" scenarios must instead opt out, so move that `Background`
  step into each scenario that needs it (Gherkin has no per-scenario Background exclusion).

Do this mechanically with a script, then `.\gradlew.bat :cucumber:cucumberDryRun` must report every step bound and no
undefined steps.

### 8d. Packaging

- `OpenRDRServer.kt`: delete `seedDemoOnStart`, the `DEMO_ARG` check and the `ensureSampleKB` block.
- `ServerApplication.ensureSampleKB`: delete, and its test if any.
- `common/.../constants.server/Constants.kt`: delete `DEMO_ARG`, `DEMO_KB_NAME`.
- `packaging/start-demo.bat`, `packaging/start-demo.sh`: remove the `Demo` argument and the "with Demo KB" wording.
- `packaging/README-demo.txt`: the first step of the script becomes: in the chat, type `open Pathology`, then give the
  copy a name (suggest `Clinic`); the Taylor/Lindsay/Jane cases are then present.
- `packaging/src/test/kotlin/io/rippledown/packaging/DemoZipSmokeTest.kt`: replace
  `shouldContain "\"Demo\""` with an assertion that `/api/kbList` returns `[]`, and drop
  `shouldContain "Demo KB seeded."`.
- `cucumber/.../demo/Pathology demo script.feature`: gets the `Background` like the rest (it provides its own Taylor
  case); fix the feature description that mentions the seeded Demo KB.

---

## Step 9 — New cuke scenarios (`kb/Knowledge Base management by chat.feature`)

Add, using existing steps (`I enter the following text into the chat panel`, `the chatbot response contains the
following terms`, `the chatbot response consists of the following lines`, `the displayed KB name is now`, `the count
of the number of cases is`), plus one new step:

```kotlin
@When("I click the knowledge base chip {string}")
fun clickKbChip(name: String) = chatPO().clickKbChoice(name)   // ChatPO: like clickSuggestion but KB_CHOICE_ITEM
```

Scenarios:

1. **The list shows demonstrations**: default KB opened; "List the knowledge bases" → lines:
   `Your knowledge bases:`, `Thyroids (open)`, `Demonstration knowledge bases (open one to get your own copy):`,
   `Contact Lens Prescription`, `Pathology`, `Thyroid Stimulating Hormone`, `Zoo Animals`.
2. **Opening a demonstration asks for a name and opens a copy**: "Open Zoo Animals" → terms `your own copy`,
   `Zoo Animals`; then `Zoo2` → terms `Created`, `Zoo2`, `demonstration and opened it`; displayed KB name is now
   `Zoo2`; count of cases is 101.
3. **Clicking a stored chip opens it**: `A Knowledge Base called Lipids has been created`; list; click chip `Lipids`;
   displayed KB name is now `Lipids`.
4. **Clicking a demonstration chip asks for a name**: list; click chip `Pathology`; terms `your own copy`,
   `Pathology`.
5. **A demonstration cannot be deleted**: "Delete Zoo Animals" → terms `cannot be deleted`; displayed KB still
   `Thyroids`.
6. **A demonstration title cannot be used**: "Create a knowledge base called Pathology" → terms
   `is the name of a demonstration knowledge base`.
7. **No knowledge bases greeting mentions demonstrations**: no `Background` step; start client; terms
   `no knowledge bases yet`, `demonstration`, `Zoo Animals`.

Run: `.\gradlew.bat :cucumber:kb` — needs the live server, model and GUI; ask the user to run it.

---

## Step 10 — Documents

- `documentation/design/kb_management_by_chat.md`: remove "creating from a sample" from the non-goals; add
  `OpenKnowledgeBase` → demonstration and `CopyDemonstrationKnowledgeBase` to the actions table; mention
  `AskForName`.
- `documentation/design/demonstration_knowledge_bases.md`: mark anything that changed during implementation.
- `documentation/design/chat_ui_guidelines.md`: change the KB list chips row to "Yes, built".

---

## Order of commits (suggested messages)

1. `Rename SampleKB.DEMO to PATHOLOGY; SampleKB.demonstrations()`
2. `resolveKbName: demonstration titles, reserved-name check`
3. `KnowledgeBaseService: demonstrations, createFromSample`
4. `KB chat actions: open a demonstration as a named copy; list, delete, create, rename aware of demonstrations`
5. `ChatManager: shared naming workflow for create and demonstration copy`
6. `Greeting and prompt mention demonstration knowledge bases`
7. `Chat: clickable knowledge base chips`
8. `Cukes: explicit "a default KB is opened"; remove lazy default KB and demo seeding`
9. `Cukes: demonstration knowledge base scenarios`
10. `Docs: demonstration knowledge bases`
