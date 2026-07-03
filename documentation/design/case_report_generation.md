# Case Report Generation — Design & Implementation Handoff

**Audience:** an implementing engineer/agent who will build this feature end-to-end.
**Status:** design agreed; not yet implemented.
**Companion artefact:** `documentation/design/report_panel_mockup.svg` (visual of the target UI).

---

## 1. Goal

Generate and display a well-worded, prose **report** for the currently-selected case,
produced by the app's LLM (Gemini) from the **comments given by rules** for that case
(optionally also using the case's attribute/value pairs). The report appears in a new
**collapsible panel beneath the existing Interpretation panel**.

To keep cost and latency down, the report is generated **only when the panel is visible**,
and only regenerated when the underlying comments actually change.

---

## 2. Architecture you must know (all paths verified)

### Case display (UI, `ui` module)

- `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\main\OpenRDRUI.kt` — top-level composable. Owns
  `currentCase: ViewableCase?` and `currentCaseId: Long?` state. Calls `CaseControl(...)` and
  `ChatController(...)`. This is where case-selection happens and where new state + the
  generation-trigger effect must live.
- `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\casecontrol\CaseControl.kt` — wraps
  `CaseInspection`. Owns the case-view filter string (a good precedent for pure-UI state).
- `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\casecontrol\CaseInspection.kt` — assembles the
  header, body and `interpretationContent` slots. The Interpretation panel is rendered here via
  `InterpretationView(...)` inside the `interpretationContent = { Column { ... } }` lambda
  (around lines 122–143).
- `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\casecontrol\CaseInspectionLayout.kt` — a
  two-pass `SubcomposeLayout` with three slots: `caseHeader`, `caseBody`, `interpretationContent`.
  **Important constraint:** `interpretationContent` is pinned to the bottom and is deliberately
  **not scrollable** — the layout assumes it is short. A report can be long, so the report panel
  must manage its own bounded height + scroll (do NOT rely on the outer layout to scroll it).
- `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\interpretation\InterpretationView.kt` — the
  existing comments panel (an `OutlinedCard`). The new `ReportView` will be a sibling in the same
  `Column`, directly below it.

### LLM primitives (`llm` module)

- `@/C:\repos\OpenRDR\llm\src\main\kotlin\io\rippledown\llm\Gemini.kt`:
    - `geminiClient`, `GEMINI_MODEL` (`"gemini-2.5-flash"`).
    - `generateContentConfig(systemInstruction, functionDeclarations = emptyList())` — builds a config
      with `temperature(0f)` (near-deterministic — good for caching) and no thinking.
    - `callWithTimeout(timeoutMs) { ... }` — hard timeout wrapper.
    - `retry(...) { ... }` — suspend retry with backoff for 503s.
    - `transcribeAudio(...)` (lines 114–132) is the **reference pattern** for a one-shot
      `geminiClient.models.generateContent(GEMINI_MODEL, content, config).text()` call. Copy this shape.
- **Do NOT use the chat machinery** (`GeminiChatService.startChat()` / `Conversation`) — that is a
  stateful multi-turn function-calling loop for rule building. A report is a single one-shot call.

### Comments source (`common` module)

- `@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\model\Interpretation.kt`:
  `fun toComments(case: RDRCase, attributeById: (Int) -> Attribute? = { null }): String` — returns the
  rule-given comments as a **JSON array string** (attribute placeholders resolved to `{Name}`). This is
  already what the chat prompt uses for its `COMMENTS` variable. Reuse it.
- `ViewableCase` exposes `.case` (an `RDRCase`, with `.interpretation` and `.caseId.id`) and
  `.attributes()` (list of `Attribute`, each has `.name`).

### Server (`server` module)

- `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\KBEndpoint.kt` — per-KB facade.
  Key methods: `viewableCase(id: Long): ViewableCase` (line 79), `case(id)`, etc. **Add the report
  method here.**
- Route files live in `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\routes\`.
  `ChatManagement.kt` and `CaseManagement.kt` are the closest templates.
- Routes are registered in `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\OpenRDRServer.kt`
  in `Application.module()` (lines 105–115). **Add a `reportManagement(application)` call there.**
- Routing helpers (`@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\routes\RoutingUtilities.kt`):
  `kbEndpoint(application)` and `caseId()` extract the `kb` and `caseId` query params.

### API wiring (`common` + `ui`)

- API path constants: `@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\constants\api\Constants.kt`.
- Client calls: `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\main\Api.kt`. Follow `getCase(caseId)`
  (lines 173–182) as the template for a GET with `setKBParameter()` + `setCaseIdParameter(caseId)`.
- `ChatResponse` model: `@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\model\chat\ChatResponse.kt`.

### Chat action mechanism (only relevant to the deferred Phase 5)

- Chat actions are JSON `{"action": "..."}` values processed **server-side** in
  `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\kb\chat\ChatManager.kt` via
  `ActionComment.createActionInstance()` → `ChatAction.doIt(...)`. The client only displays the
  returned `ChatResponse` text and refreshes the case. There is currently **no client-side action
  dispatch**, so driving a UI-only panel from chat requires extra plumbing (see Phase 5).

---

## 3. Scope of iteration 1

**In scope**

- Server endpoint that generates a Markdown report for a case from its comments (+ attributes).
- Simple hard-coded system prompt stored as a resource file.
- Caching so the same (case, comments) is not regenerated.
- New collapsible **Report** panel below the Interpretation panel.
- Visibility controlled by a **GUI toggle** (the collapse chevron), backed by client-side state.
- Generation triggered only when the panel is visible, the case has comments, and no rule session
  is in progress.

**Explicitly NOT in scope (later phases)**

- Chat command to show/hide the panel (Phase 5 — needs new plumbing).
- User-customisable system prompt.
- The future "indexed list of named/numbered comments as derived attributes" redesign of the
  Interpretation panel. Keep `ReportView` independent so that redesign won't disturb it.

---

## 4. Design decisions & rationale

- **Server-side generation.** The comments live server-side, the Gemini `API_KEY` is server-side,
  and `ViewableCase`/`toComments` are already available there. The client just fetches a string.
- **One-shot call, not the chat.** A report is stateless; the chat loop is the wrong tool.
- **`temperature = 0`** (already the default in `generateContentConfig`) gives stable output, which
  makes caching meaningful.
- **Cache key = `caseId` + hash of the comments string.** Re-selecting an unchanged case = no LLM
  call; building a rule changes the comments, which invalidates the cache and regenerates.
- **Visibility gates generation.** Hidden panel ⇒ never call the LLM. This is the primary cost lever.
- **Iteration-1 rendering = plain text.** No Markdown renderer exists in the repo. To avoid adding a
  dependency in the first cut, render the report as plain, wrapped, selectable text and instruct the
  model to emit lightly-structured prose. A real Markdown renderer is an optional, clearly-scoped
  step (Section 6).

---

## 5. Implementation — do these phases in order

> Line numbers drift as you edit; locate insertion points by the quoted anchor strings, not by number.
> This repo's `.kt`/`.feature` files use LF endings; keep them LF.

### Phase A — Shared model + API constant

1. **New model type.** Create
   `@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\model\report\CaseReport.kt`:
   ```kotlin
   package io.rippledown.model.report

   import kotlinx.serialization.Serializable

   @Serializable
   data class CaseReport(
       val markdown: String,
       val generated: Boolean = true // false when there were no comments to report on
   )
   ```

2. **API path constant.** In
   `@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\constants\api\Constants.kt`, add:
   ```kotlin
   const val CASE_REPORT = "/api/caseReport"
   ```

### Phase B — LLM primitive (`llm` module)

Add a one-shot text generation helper to
`@/C:\repos\OpenRDR\llm\src\main\kotlin\io\rippledown\llm\Gemini.kt` (or a new file
`ReportGeneration.kt` in the same package). Model it on `transcribeAudio`:

```kotlin
/**
 * One-shot text generation. Blocking Gemini call guarded by [callWithTimeout];
 * callers should wrap this in [retry] to survive 503s.
 */
fun generateText(systemInstruction: String, userContent: String, timeoutMs: Long = 90_000): String {
    val config = generateContentConfig(systemInstruction = systemInstruction)
    val content = Content.fromParts(Part.fromText(userContent))
    return callWithTimeout(timeoutMs) {
        geminiClient.models.generateContent(GEMINI_MODEL, content, config).text() ?: ""
    }
}
```

### Phase C — System prompt resource (`server` module)

Create `@/C:\repos\OpenRDR\server\src\main\resources\report\report_system_prompt.md`. Keep it simple.
Because iteration 1 renders as plain text, instruct light structure only:

```
You are a clinical reporting assistant. You are given a set of comments that were
produced by rules for a single patient case, and optionally the case's attribute/value
pairs. Write a clear, professional, well-worded report for the case.

Guidelines:
- Base the report ONLY on the supplied comments. You may refer to attribute values to
  make the wording concrete, but do not invent findings that are not implied by the comments.
- Use plain prose in short paragraphs separated by a blank line.
- You may use a short heading line and simple "- " bullet points, but do not use tables,
  code blocks, or heavy Markdown.
- Do not add a preamble such as "Here is the report". Output only the report text.
- If there are no comments, output nothing.
```

Load it exactly as `KBChatService.readPromptResource` does
(`@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\kb\chat\KBChatService.kt:23-27`), i.e.
`this::class.java.getResource("/report/report_system_prompt.md").readText()`.

### Phase D — Server report service + KBEndpoint method + cache

1. **Report service.** Create
   `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\kb\report\ReportService.kt`:
   ```kotlin
   package io.rippledown.kb.report

   import io.rippledown.llm.generateText
   import io.rippledown.llm.retry
   import io.rippledown.model.Attribute
   import io.rippledown.model.caseview.ViewableCase
   import io.rippledown.model.report.CaseReport

   object ReportService {
       private fun readPrompt(): String =
           (ReportService::class.java.getResource("/report/report_system_prompt.md")
               ?: error("report_system_prompt.md not found")).readText()

       /** Build the LLM user-content from the case's comments (+ attributes). */
       fun userContent(viewableCase: ViewableCase, attributeById: (Int) -> Attribute?): String {
           val comments = viewableCase.case.interpretation.toComments(viewableCase.case, attributeById)
           val attributes = viewableCase.attributes().joinToString("\n") { it.name }
           return "Comments (JSON array):\n$comments\n\nCase attributes:\n$attributes"
       }

       suspend fun generate(viewableCase: ViewableCase, attributeById: (Int) -> Attribute?): CaseReport {
           val comments = viewableCase.case.interpretation.toComments(viewableCase.case, attributeById)
           // toComments returns "[]" for an empty interpretation — do not call the LLM.
           if (comments.isBlank() || comments == "[]") return CaseReport(markdown = "", generated = false)
           val text = retry { generateText(readPrompt(), userContent(viewableCase, attributeById)) }
           return CaseReport(markdown = text.trim())
       }
   }
   ```
   > `attributeById` is the same resolver used elsewhere; on `KBEndpoint` it is available via the
   > rule session manager (see `ChatSessionManager` which passes `ruleSessionManager::attributeById`).
   > If not conveniently reachable from `KBEndpoint`, pass `{ null }` — `toComments` falls back to the
   > case's own attributes, which is adequate for the report.

2. **Cache + KBEndpoint method.** In
   `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\KBEndpoint.kt` add a small cache and
   method. Key on caseId + comments hash so a rule change regenerates:
   ```kotlin
   private val reportCache = mutableMapOf<Long, Pair<Int, CaseReport>>() // caseId -> (commentsHash, report)

   suspend fun caseReport(caseId: Long): CaseReport {
       val viewable = viewableCase(caseId)
       val comments = viewable.case.interpretation.toComments(viewable.case)
       val key = comments.hashCode()
       reportCache[caseId]?.let { (cachedKey, cached) -> if (cachedKey == key) return cached }
       val report = ReportService.generate(viewable) { null }
       reportCache[caseId] = key to report
       return report
   }
   ```
   > If `KBEndpoint` methods are not `suspend`-friendly in the route, you can make the route handler
   > call this in a coroutine — Ktor route lambdas are already suspend. Keep `caseReport` `suspend`.

### Phase E — Server route

1. Create `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\routes\ReportManagement.kt`
   modelled on `ChatManagement.kt` / the `get(CASE)` handler in `CaseManagement.kt`:
   ```kotlin
   package io.rippledown.server.routes

   import io.ktor.http.HttpStatusCode.Companion.BadRequest
   import io.ktor.server.application.*
   import io.ktor.server.response.*
   import io.ktor.server.routing.*
   import io.rippledown.constants.api.CASE_REPORT
   import io.rippledown.log.lazyLogger
   import io.rippledown.server.ServerApplication

   fun Application.reportManagement(application: ServerApplication) {
       val logger = lazyLogger
       routing {
           get(CASE_REPORT) {
               try {
                   val endpoint = kbEndpoint(application)
                   val report = endpoint.caseReport(caseId())
                   call.respond(report)
               } catch (e: Exception) {
                   logger.error("caseReport failed", e)
                   call.respond(BadRequest, e.message.toString())
               }
           }
       }
   }
   ```
2. Register it in `@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\OpenRDRServer.kt`
   next to `chatManagement(application)`:
   ```kotlin
   chatManagement(application)
   reportManagement(application)   // <-- add
   ```

### Phase F — Client API

In `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\main\Api.kt`, add (mirror `getCase`):

```kotlin
suspend fun getCaseReport(caseId: Long): CaseReport? {
    return try {
        client.get("$API_URL$CASE_REPORT") {
            setKBParameter()
            setCaseIdParameter(caseId)
        }.body<CaseReport>()
    } catch (_: Exception) {
        null
    }
}
```

Add imports: `io.rippledown.model.report.CaseReport` and `io.rippledown.constants.api.CASE_REPORT`
(the file already does `import io.rippledown.constants.api.*`, so the constant is covered).

### Phase G — `ReportView` composable

Create `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\interpretation\ReportView.kt`. It is a
**collapsible** panel: a header row (chevron + "Report") and, when expanded, a bounded,
vertically-scrollable body showing the report text. Iteration 1 renders plain text.

```kotlin
package io.rippledown.interpretation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

const val REPORT_PANEL = "REPORT_PANEL"
const val REPORT_TOGGLE = "REPORT_TOGGLE"
const val REPORT_TEXT = "REPORT_TEXT"

@Composable
fun ReportView(
    reportMarkdown: String?,   // null = not yet loaded / loading
    isVisible: Boolean,
    onToggle: (Boolean) -> Unit,
    isLoading: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = REPORT_TOGGLE },
        ) {
            Icon(
                imageVector = if (isVisible) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.clickableToggle(isVisible, onToggle)
            )
            Text(text = "Report", modifier = Modifier.padding(start = 4.dp).clickableToggle(isVisible, onToggle))
        }
        if (isVisible) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                    .semantics { contentDescription = REPORT_PANEL }) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)) {
                    when {
                        isLoading -> Text("Generating report…")
                        reportMarkdown.isNullOrBlank() -> Text("No report.")
                        else -> Text(
                            text = reportMarkdown,
                            modifier = Modifier.semantics { contentDescription = REPORT_TEXT })
                    }
                }
            }
        }
    }
}
```

> `clickableToggle` is shorthand — implement it as a small `Modifier.clickable { onToggle(!isVisible) }`
> extension, or inline `Modifier.clickable`. Use whichever clickable import the module already uses
> (foundation `clickable`). Keep the chevron + label both clickable.

### Phase H — Wire the panel and state into the case view

1. **Thread new params through `CaseInspection`.** Add parameters to
   `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\casecontrol\CaseInspection.kt`:
   `reportMarkdown: String?`, `reportVisible: Boolean`, `onReportToggle: (Boolean) -> Unit`,
   `reportLoading: Boolean`. Inside the `interpretationContent` `Column`, below `InterpretationView(...)`,
   add:
   ```kotlin
   ReportView(
       reportMarkdown = reportMarkdown,
       isVisible = reportVisible,
       onToggle = onReportToggle,
       isLoading = reportLoading,
   )
   ```
2. **Pass through `CaseControl`.** Add the same parameters to
   `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\casecontrol\CaseControl.kt` and forward them
   to `CaseInspection`.
3. **Own the state in `OpenRDRUI`.** In
   `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\main\OpenRDRUI.kt` add:
   ```kotlin
   var reportVisible by remember { mutableStateOf(false) }
   var reportMarkdown by remember { mutableStateOf<String?>(null) }
   var reportLoading by remember { mutableStateOf(false) }
   ```
   Pass `reportVisible`, `reportMarkdown`, `reportLoading`, and
   `onReportToggle = { reportVisible = it }` into `CaseControl(...)`.
4. **Generation trigger.** Add a `LaunchedEffect` keyed on the things that should cause (re)generation.
   Gate on: panel visible, a case is selected, and no rule session is in progress (`cornerstoneStatus == null`).
   The server already returns cached results, so re-fetching on case change is cheap when unchanged:
   ```kotlin
   LaunchedEffect(currentCaseId, reportVisible, currentCase, cornerstoneStatus) {
       val id = currentCaseId
       if (reportVisible && id != null && cornerstoneStatus == null) {
           reportLoading = true
           withContext(dispatcher) {
               val report = api.getCaseReport(id)
               reportMarkdown = report?.markdown
           }
           reportLoading = false
       } else if (!reportVisible) {
           reportMarkdown = null
       }
   }
   ```
   > Rationale for keying on `currentCase`: after a chat action the code already refreshes
   > `currentCase` (see `chatControllerHandler.sendUserMessage`), so a comment change re-runs this
   > effect; the server cache regenerates because the comments hash changed.

---

## 6. Optional: real Markdown rendering (recommended follow-up, not required for iteration 1)

If/when you want true Markdown, add a Compose-Multiplatform Markdown renderer rather than hand-rolling.
Dependency location: `@/C:\repos\OpenRDR\ui\build.gradle.kts` (the `dependencies { }` block, lines 9–31).
A commonly-used option is `com.mikepenz:multiplatform-markdown-renderer-m3`. Add it to the version
catalog if the project uses one, then swap the `Text(reportMarkdown)` in `ReportView` for the
renderer's `Markdown(...)` composable. Verify the license and that it supports Compose Desktop before
committing. Keep the plain-text fallback for empty/error states.

---

## 7. Testing

Follow the repo's existing test styles. Write tests BEFORE or alongside implementation.

- **`common`** — `CaseReport` serializes/deserializes (trivial; only if you add logic).
- **`server`**
    - `ReportServiceTest`: given a `ViewableCase` with comments, `userContent(...)` includes the
      comments and attribute names; with an empty interpretation, `generate(...)` returns
      `CaseReport(markdown = "", generated = false)` and does **not** call the LLM. Mock the LLM
      boundary (mockk) so no real network call happens — see how `llm`/`chat` tests fake Gemini.
    - `KBEndpoint` cache test: two calls for the same case with unchanged comments call the LLM once;
      after the interpretation changes, the next call regenerates.
    - A route test mirroring `@/C:\repos\OpenRDR\server\src\test\kotlin\io\rippledown\server\ChatManagementTest.kt`:
      `GET /api/caseReport?kb=...&caseId=...` returns 200 + a `CaseReport`.
- **`ui`**
    - `ReportViewTest` (Compose UI test, JUnit4 — see existing `interpretation` tests): collapsed state
      hides `REPORT_PANEL`; expanded shows it; loading shows "Generating report…"; blank shows "No report.";
      non-blank shows the text under `REPORT_TEXT`; clicking `REPORT_TOGGLE` calls `onToggle`.
    - Use mock `Api` (see `@/C:\repos\OpenRDR\ui\src\test\kotlin\io\rippledown\mocks\MockEngineDSL.kt`) to
      stub the `CASE_REPORT` endpoint if you add an integration-style test.

Run (do not auto-run; propose to the user):

- Server/common tests: `./gradlew :server:test :common:test`
- UI tests: `./gradlew :ui:test`

---

## 8. Acceptance criteria (iteration 1)

- With the Report panel collapsed, selecting cases makes **no** `/api/caseReport` calls.
- Expanding the panel generates and shows a prose report derived from the case's comments.
- A case with **no** comments shows "No report." and triggers no LLM call.
- Re-selecting a case whose comments have not changed does not re-invoke the LLM (served from cache).
- Building/removing a comment via chat and returning to the case yields an updated report.
- No report generation occurs while a rule session is in progress.
- The report panel scrolls internally for long reports and never pushes the case body off-screen.
- All new and existing tests pass.

---

## 9. Deferred — Phase 5: chat-driven show/hide (do NOT do in iteration 1)

To let the user type "show/hide the report" in chat you must bridge a server-side chat action to
UI-only state. Recommended approach:

1. Add constants (e.g. `SHOW_REPORT`, `HIDE_REPORT`) alongside the other chat action names in the chat
   constants file, and a short instruction section in `server/src/main/resources/chat/instructions/`
   plus registration in `KBChatService.systemPromptMainSections`/`systemPromptVariables`.
2. Add a `ChatAction` implementation (see `io.rippledown.kb.chat.action`) whose `doIt(...)` returns a
   `ChatResponse` carrying the desired visibility.
3. Add a nullable field to `ChatResponse` (e.g. `setReportVisible: Boolean? = null`).
4. In `@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\main\OpenRDRUI.kt`, in
   `chatControllerHandler.sendUserMessage`, after `onBotMessageReceived(response)`, apply
   `response.setReportVisible?.let { reportVisible = it }`.

This is deliberately out of scope for the first cut because it touches the prompt, a new action type,
the shared `ChatResponse`, and the client — the GUI chevron already satisfies the core requirement.

---

## 10. Deferred — future comments redesign (context only)

The owner plans to later turn the Interpretation panel's concatenated comments into an **indexed,
scrollable list** where each comment has a name/number and behaves like a **derived** attribute (usable
as a condition in later rules), distinct from the **primary** attributes supplied by the external
system. Keep `ReportView` fully independent of `InterpretationView` so that redesign does not affect the
report. The AI report stays a separate, prose/Markdown panel.
