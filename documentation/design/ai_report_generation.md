# AI Report Generation — Design

This document describes the current design of the AI-generated report facility. For what the
facility must do, see `documentation/requirements/ai_report_generation.md`.

## Overview

The report is generated **server-side** by a single, one-shot LLM call and fetched by the client
over HTTP. The client owns the panel's visibility and decides when to (re)fetch; the server owns the
LLM call and a per-case cache. The report flows from case comments to prose in these stages:

```
ReportView (UI)  ->  Api.getCaseReport  ->  GET /api/caseReport  ->  KBEndpoint.caseReport (cache)
                                                                          -> ReportService.generate
                                                                              -> generateText (Gemini)
```

## Shared model

`@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\model\report\CaseReport.kt` is the wire type:
a `@Serializable data class CaseReport(val markdown: String, val generated: Boolean = true)`.

`generated = false` distinguishes "the case had no comments, so we deliberately produced nothing"
from "generation produced empty text". The endpoint path constant `CASE_REPORT = "/api/caseReport"`
lives in `@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\constants\api\Constants.kt`.

The UI element content descriptions `REPORT_PANEL`, `REPORT_TOGGLE`, `REPORT_TEXT`,
`REPORT_DISCLAIMER_ICON` and `REPORT_COPY_ICON`, together with the disclaimer text `REPORT_DISCLAIMER`,
are defined in
`@/C:\repos\OpenRDR\common\src\main\kotlin\io\rippledown\constants.interpretation\Constants.kt` so
they are shared between the UI and the cucumber page objects.

## LLM primitive

`@/C:\repos\OpenRDR\llm\src\main\kotlin\io\rippledown\llm\Gemini.kt` provides `generateText`, a
one-shot text-generation helper modelled on `transcribeAudio`:

```kotlin
fun generateText(systemInstruction: String, userContent: String, timeoutMs: Long = 90_000): String
```

It builds a config from `generateContentConfig` (which uses `temperature(0f)`, so output is stable
and caching is meaningful), issues a single `geminiClient.models.generateContent(...)` call, and is
guarded by `callWithTimeout`. It is **not** the chat machinery (`GeminiChatService` / `Conversation`),
which is a stateful multi-turn function-calling loop used for rule building. A report is stateless,
so a one-shot call is the right tool. Callers wrap it in `retry` to survive transient 503s.

## Server report service

`@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\kb\report\ReportService.kt` is a **class**
(not an object) so it can be injected into `KBEndpoint` and mocked in tests.

- `readPrompt()` loads the system prompt from the resource
  `@/C:\repos\OpenRDR\server\src\main\resources\report\report_system_prompt.md`. The prompt instructs
  the model to write grounded, lightly-structured prose based only on the supplied comments.
- `userContent(viewableCase, attributeById)` builds the user message from two parts: the comments (via
  `Interpretation.toComments` on the *viewable* interpretation, whose `ByDefinition` comment assignments have been
  resolved to their stored definitions; a JSON array of comment texts, with attribute placeholders resolved to `{Name}`)
  and the full case serialized as JSON (via `viewableCase.toJsonString()`, using the
  `ViewableCase` serializer). Sending the whole case lets the model make wording concrete with
  actual attribute values.
- `generate(viewableCase, attributeById)`:
    - Short-circuits when there are no comments: `toComments` returns `"[]"` for an empty
      interpretation, in which case it returns `CaseReport(markdown = "", generated = false)` and
      never calls the LLM.
    - Otherwise calls `generateText` inside `retry(maxRetries = 3)` with `timeoutMs = 30_000`. These
      are deliberately tighter than the `generateText` defaults because this is an interactive,
      UI-triggered path where worst-case latency must stay bounded, rather than batch work.
    - Returns `CaseReport(markdown = text.trim(), generated = true)`.

## KBEndpoint: cache and delegation

`@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\KBEndpoint.kt` injects a
`ReportService` (defaulting to a new instance), holds a per-case cache
(`reportCache: Map<Long, Pair<Int, CaseReport>>`, i.e. caseId -> (commentsHash, report)), and
exposes `suspend fun caseReport(caseId: Long): CaseReport`.

The cache key is the hash of the case's **comment text** (`viewableInterpretation.latestText()`,
which is already computed on the viewable interpretation, so `toComments` is not recomputed here).
Re-requesting a report for a case whose comments are unchanged returns the cached value without
calling the LLM; building or changing a comment changes the comment text, which invalidates the
cache and forces regeneration on the next request.

`attributeById` is passed as `{ null }`: `toComments` then falls back to the case's own attributes,
which is adequate for the report.

## Server route

`@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\routes\ReportManagement.kt` exposes
`GET /api/caseReport`. It resolves the endpoint via `kbEndpoint(application)`, reads the case id via
`caseId()`, and responds with the `CaseReport`. On any exception it logs the cause and responds
`500 InternalServerError` **without** the exception message — report generation failures are
server-side (typically the LLM call), so the detail stays in the server log rather than being
returned to the client. The route is registered alongside `chatManagement(application)` in
`@/C:\repos\OpenRDR\server\src\main\kotlin\io\rippledown\server\OpenRDRServer.kt`.

## Client API

`@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\main\Api.kt` provides
`suspend fun getCaseReport(caseId: Long): CaseReport?`, a GET to `CASE_REPORT` with the KB and case-id
parameters set (mirroring `getCase`). A failed fetch is non-critical, so it returns `null` and the
caller renders the empty-report state; the server logs the underlying cause.

## UI: the ReportView panel

`@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\interpretation\ReportView.kt` is a collapsible
panel:

- A header `Row` carrying `contentDescription = REPORT_TOGGLE`, made clickable with
  `Modifier.clickable { onToggle(!isVisible) }`. It shows a down/right chevron and the label
  "Report", followed by an information icon (`REPORT_DISCLAIMER_ICON`) wrapped in a `TooltipArea`
  that shows the `REPORT_DISCLAIMER` text on hover.
- When `isVisible`, a 4.dp `Spacer` separates the header from the content (matching the case-list
  section headers), then the body is chosen on `isLoading`:
    - `isLoading` -> a `TypingIndicator()` (the chat's three-dot indicator) is shown **between the
      header and the panel**, not inside the card, so it reads like the chat's indicator.
    - otherwise an `OutlinedCard` (content description `REPORT_PANEL`) bounded to
      `heightIn(max = 240.dp)` with an internal `verticalScroll`, so long reports scroll within the
      panel and never push the case body off-screen (the surrounding `CaseInspectionLayout`'s
      `interpretationContent` slot is intentionally not scrollable).
- The card body is a `when` over the panel's state:
    - `!hasComments` -> "No comments to report on."
    - `reportText.isNullOrBlank()` -> "No report."
  - otherwise the report, tagged with `contentDescription = REPORT_TEXT`.

Before rendering, the report text is passed through `formatReportText` (an `internal` function in
`ReportView.kt`), which rewrites `10^N` as `10` followed by Unicode superscript digits (e.g.
`10^12` -> `10¹²`) and the SI micro prefix `u` as the Greek letter mu (`umol/L` -> `μmol/L`). This
mirrors `UnitsCell.formatUnits` in the case view, but returns a plain `String` rather than an
`AnnotatedString` because the Markdown renderer consumes a `String`. The same formatted text is
published on a hidden, zero-size `Text` node (content description `REPORT_TEXT`) so the cucumber
page object and unit tests can read the rendered content, which the Markdown renderer does not
expose to the Java accessibility bridge.

The report is rendered as Markdown via `com.mikepenz.markdown.m3.Markdown` (the
`multiplatform-markdown-renderer-m3` library, added in `gradle/libs.versions.toml` as
`markdownRenderer`/`markdownRendererM3`). The renderer is published against a newer Kotlin than
this project's toolchain, so `ui/build.gradle.kts` forces `kotlin-stdlib` back to the project's
Kotlin version via a `resolutionStrategy`. The system prompt asks the model for short `##`
headings, bullet points, and **bold** for out-of-range values, and to use Australian medical
terminology, so the panel shows structured, formatted output rather than plain text. A custom
`markdownAnnotator` intercepts `STRONG` nodes to render bold (out-of-range) values in the same red
used by the case view, filtering out the `**`/`__` emphasis delimiter tokens so the markers
themselves are not shown.

A copy `IconButton` (content description `REPORT_COPY_ICON`) is overlaid at the card's top-end when
a report is shown (`!reportText.isNullOrBlank() && !isLoading && hasComments`). It is placed **after**
the scrollable `Column` in the `Box` so it renders on top and stays clickable (a preceding sibling
would have its pointer events intercepted by the `Column`'s `verticalScroll`). Clicking it copies the
formatted text to the clipboard via `LocalClipboardManager` and sets `justCopied`, which swaps the
copy icon for a green check for `COPY_CONFIRMATION_MS` (1500ms). The icon rests at `COPY_RESTING_ALPHA`
(0.35) opacity and becomes fully opaque while hovered or just after copying.

## UI: state and generation trigger

`@/C:\repos\OpenRDR\ui\src\main\kotlin\io\rippledown\main\OpenRDRUI.kt` owns the report state and
threads it down through `CaseControl` -> `CaseInspection` -> `ReportView` (as `reportText`,
`reportGenerated`, `isLoadingReport`, `reportVisible`, `onReportToggle`). It holds three pieces of
remembered state: `reportVisible: Boolean`, `report: CaseReport?`, and `isLoadingReport: Boolean`.

During a rule-building session the entire `ReportView` (panel *and* its toggle) is hidden, not just
left ungenerated: `CaseControl` passes `showReport = cornerstoneStatus == null` to `CaseInspection`,
which omits the `ReportView` when a session is active. Since the report is deliberately not
regenerated during a session, hiding it avoids presenting a stale report that no longer matches the
interpretation being edited. `reportVisible` is preserved across the session, so the panel reappears
in its previous state (and regenerates) once the session completes.

Two `LaunchedEffect`s drive generation:

1. Keyed on `currentCaseId`, it clears `report` as soon as the selected case changes, so the previous
   case's report is not shown while a new one is generated.
2. Keyed on `reportVisible`, `currentCaseId`, the case's comment text
   (`currentCase?.viewableInterpretation?.latestText()`) and `ruleInProgress`, it fetches the report
   when the panel is visible, a case is selected, and no rule session is active. Keying on the
   comment text (rather than the whole `ViewableCase`) means it regenerates when the comments change
   but not on every unrelated case refresh (e.g. after each chat message). The server cache makes the
   re-fetch cheap when the comments are unchanged.

`report?.markdown` supplies the panel text and `report?.generated` supplies `hasComments`, so a case
with no comments (`generated = false`) shows "No comments to report on."

## Design decisions

- **Server-side generation, not client-side.** The comments and the Gemini API key live on the
  server, and `ViewableCase` / `toComments` are already available there. Generating server-side keeps
  the key off the client and lets the client stay a thin fetch-and-render layer. The alternative — a
  client-side call — would have leaked the key and duplicated case-serialization logic.
- **One-shot `generateText`, not the chat machinery.** A report is a stateless, single request.
  Reusing `GeminiChatService` / `Conversation` (the multi-turn function-calling loop built for rule
  building) would have imposed conversational state and tool-dispatch overhead for no benefit.
- **Cache keyed on comment-text hash, not on `caseId` alone or on the whole case.** The report is a
  function of the comments, so hashing the comment text gives exactly the right invalidation: unchanged
  comments hit the cache; a rule that changes a comment misses it and regenerates. Keying on `caseId`
  alone would serve stale reports after a rule change; keying on the whole `ViewableCase` would
  regenerate needlessly on unrelated refreshes (e.g. every chat message).
- **Visibility gates generation.** Because the panel is collapsed by default and generation only runs
  when it is visible, browsing cases costs nothing. This is the primary cost/latency lever and the
  reason visibility state lives in the client, which is the only side that knows whether the panel is
  shown.
- **Tighter retry/timeout than the LLM defaults.** This is an interactive path, so `ReportService`
  caps retries at 3 and the per-call timeout at 30s to bound worst-case latency, rather than using the
  longer defaults intended for batch work.
- **Markdown rendering with a structured-report prompt.** The report is rendered with the
  `multiplatform-markdown-renderer-m3` library and the prompt asks the model to structure the
  report with `##` headings (which it chooses per case), bullets, and **bold** for out-of-range
  values. Highlighting of out-of-range values is left to the model rather than done by
  deterministic post-processing in the app: the case JSON already contains each result's
  `referenceRange`, so the model has the information it needs, and post-processing would have to
  locate values within free prose (unreliable when two attributes share a value but differ in
  range status). This is a trial approach and may be revisited if the model proves unreliable at
  identifying out-of-range values.

## Deferred / out of scope

- **Chat-driven show/hide** of the panel (would require bridging a server-side chat action to
  UI-only visibility state via `ChatResponse`).
- **Selecting which attributes are sent to the LLM.** Currently the full case is serialized and sent;
  a future version will let the user choose which attributes to include.
- **Sending the comments' names to the model.** The report is built from the comment *texts* only
  (`Interpretation.toComments`). Now that every comment is a named attribute, the name is signal about the comment's
  role and is meant to be an input too. Keeping
  `ReportView` independent of `InterpretationView` meant the comments redesign did not disturb the
  report.
