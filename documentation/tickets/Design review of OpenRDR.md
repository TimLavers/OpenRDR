AI-Assisted Rule Building — Findings and Recommended Approach

*Working document, 2026-08-17. Basis for discussion; nothing here is decided.*

## 1. Problem statement

Rule building in RippleDown is powerful but not user-friendly. The wizard workflow (choose action → optional objective →
enter conditions in strict syntax → review cornerstone cases → add excluding conditions → commit) has been essentially
unchanged for decades and demands that the user think in condition syntax rather than in clinical language.

The long-term goal is AI-guided rule building — pathologist-in-the-loop for safety, AI-in-the-loop for usability — with
OpenRDR's chat-driven approach as one reference point. A starting point will be built at a 2-day "AI DLC" training
course.

## 2. Survey: OpenRDR's chat-driven rule building

Repo: `~/Developer/OpenRDR` (Gradle modules: `chat`, `llm`, `hints`, `server`, `ui`,
`common`, `cucumber`). Paths below are relative to that root.

### 2.1 Overall architecture

Two **independent** Gemini conversations, both server-side; the UI never talks to an LLM directly.

**Conversation A — the "operator" chat** drives the whole workflow:

```
ui/ChatPanel + ChatController → server/routes/ChatManagement.kt
  → KBEndpoint → KBSession → kb/ChatSessionManager
    → kb/chat/ChatManager            (mediator: parses model JSON, dispatches actions)
      → chat/Conversation            (LLM turn loop, retries)  → GeminiChatService
      → kb/chat/RuleService          (narrow interface onto the rule engine)
        implemented by kb/RuleSessionManager
```

**Conversation B — the condition translator** (`hints` module): a separate long-lived Gemini chat per
`RuleSessionManager` whose only job is natural-language expression →
`ConditionSpecification` JSON, turned into a real `Condition` by
`ConditionGenerator` via reflective instantiation of predicate/signature classes. The split is a documented, deliberate
design decision (`documentation/design/chat_design.md`).

The operator model communicates through a **mixed protocol**:

| Mechanism                                 | Used for                                                                                                                                 |
|-------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Native function calling                   | only 3 functions: `transformReasonToFormalCondition`, `getSuggestedConditions`, `selectSuggestedCondition`                               |
| Free-text JSON embedded in the reply body | *all* state-changing actions (`AddComment`, `CommitRule`, `ExemptCornerstone`, `UndoLastRule`, …), scraped out by `extractJsonFragments` |
| Plain free text                           | fallback, shown to the user verbatim                                                                                                     |

No JSON-schema-constrained output mode (`responseSchema`) is used; JSON shape is enforced by prompt text plus a lenient
deserializer and string patch-ups (`ChatManager.sanitizeLlmJson`).

Transport to the UI: `ChatResponse(text, suggestions, tip)` over HTTP, **plus** a side-channel WebSocket that pushes
cornerstone-panel state to the desktop UI out-of-band — i.e. even OpenRDR does not display the cornerstone case in the
chat itself.

### 2.2 Rule-building flow

There is **no explicit state machine in code**. The phases live in a numbered 18-part system prompt
(`server/src/main/resources/chat/instructions/1_task.md` …)
— roughly 1,550 lines of markdown — plus one boolean of real state (`RuleService.isRuleSessionActive()`). Everything
else is the LLM's conversation history. The encoded flow:

1. Model asks add/remove/replace, collects comment text, asks confirmation.
2. Model emits `{"action":"AddComment", ...}` → server starts the rule session.
3. The resulting cornerstone status string is fed **back into the model as if it were a user turn** (recursive re-entry
   into `ChatManager.response`).
4. Model must call `getSuggestedConditions`. Suggestions are **deterministically generated** by a ranking engine
   (`suggestions/ConditionSuggester.kt` +
   `RelevanceRanker`) — never LLM-generated. The LLM only resolves the user's pick.
5. Free-text reasons go through the translator conversation and are then validated (see 2.4) before being added.
6. Model emits `{"action":"CommitRule"}`.

Two deterministic safety nets: the server calls `getSuggestedConditions` itself if the model forgets, and every user
message during a session is prefixed with the current cornerstone status so the model cannot drift on the count.

### 2.3 Cornerstone review in chat

Handled in-chat via actions `ExemptCornerstone` / `NextCornerstone` /
`PreviousCornerstone`, with the case body shown in the normal desktop cornerstone panel (WebSocket push). "Disallow" is
not an action — the user supplies another reason, which shrinks the cornerstone set. `CornerstoneReviewMessage.kt` is
the most instructive file about failure modes: when the cornerstone count hits zero it must append an imperative
directive forcing the model to emit `CommitRule`, because the model otherwise loops back to offering suggestions; a
sibling message deliberately omits that directive because it caused premature commits.

### 2.4 Validation before anything touches the KB

The last layer is genuinely strong. `RuleSessionManager.conditionForExpression`
rejects an LLM-produced condition unless **all** hold:

1. it parses to a non-null `Condition`;
2. every attribute it references exists in the materialised case (attribute names are fuzzy-resolved: exact →
   punctuation-normalised → Levenshtein, never trusted verbatim);
3. `condition.holds(materialisedCase)` — it is actually true for the case;
4. it creates no derived-attribute dependency cycle.

Failures are returned *to the model* as the function result, phrased as corrective instructions — an elegant in-band
steering mechanism. Retry handling is mature:
per-turn timeouts, exponential backoff, empty-response nudges, workarounds for specific google-genai SDK defects.

### 2.5 Provider and prompts

Gemini via the `com.google.genai` Java SDK; model `gemini-3.1-flash-lite`, temperature 0, thinking disabled, all safety
settings OFF (clinical text trips them). The operator prompt is the 18 markdown files + 5 example files with
`{{PLACEHOLDER}}` substitution; the case's **attribute names and current comments**
are injected, but attribute *values* are not — the operator model never sees case data. The translator prompt is
assembled from predicate/signature listings plus
~258 lines of examples; attribute names are sent per-message so one chat serves a whole session. The `llm` module also
does Gemini voice transcription feeding a voice-input UI.

### 2.6 Judgement

**Notably good — worth copying:**

- **The LLM cannot invent a condition.** Everything is re-derived server-side and must evaluate true on the case before
  entering a rule. The KB is protected by semantics, not prompt discipline. The single best decision in the design.
- Deterministic, ranked suggestions; LLM used only as a selector.
- Two separate LLM contexts (workflow vs condition translation).
- Corrective error strings returned as function results.
- A `SuggestionsBuffer` so the model never has to echo suggestion lists back (removes a whole class of transcription
  errors).
- Unusually mature failure handling (timeouts, token logging, SDK workarounds).

**Notably fragile — worth avoiding:**

- **~1,550 lines of prompt as the control-flow engine.** Dense with shouted negative constraints, each a scar from an
  observed model failure; comments document behaviours that flipped when a directive was added or removed. Correctness
  is empirically tuned per model; every model upgrade is a re-tuning exercise.
- No structured-output schema; JSON scraped from prose and string-patched.
- Reflective class loading keyed on an LLM-supplied string (`Class.forName(".$action")`), plus dead actions listed in
  the prompt (`ShowCornerstones`) that resolve to "Sorry, I don't understand."
- Re-entrant recursion (action → model → action) with no depth guard.
- English-only heuristics (hard-coded "allow" phrase list, `[editable]` marker parsed by both model and UI) in an
  explicitly multilingual product.
- One conversation per KB endpoint (`lateinit var chatManager`, no session id) — will not survive concurrent users.
