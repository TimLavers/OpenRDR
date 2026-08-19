The following is a review (by Fable) of some of the core design decisions in OpenRDR's chat-driven rule building system.

## 2. Survey: OpenRDR's chat-driven rule building

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

---

## 4. Recommended approach

### 4.1 Core principle

**LLM proposes and translates; the engine verifies; the pathologist approves.**
The LLM must never write to the KB except through a validated tool call, and
`commit` always requires an explicit user confirmation in the UI — not a chat
"yes". OpenRDR proves the validation gate works;

### 4.2 State machine in Kotlin, not in the prompt

This is the key divergence from OpenRDR. The rule-building workflow is a small, crisp state machine — choose action →
gather conditions → review cornerstones → commit — and `InteractiveRuleSession` already *is* one. Let server code own
the phases and expose to the LLM **only the tools legal in the current phase**, via native function calling with
`responseSchema`-constrained output (no JSON-in-prose scraping). In the cornerstone phase the model physically cannot
call `commit`
while conflicts remain, because the tool is not offered.

Consequences:

- Eliminates the class of failures OpenRDR patches with prompt directives ("Your VERY NEXT response MUST be…").
- The prompt shrinks to explaining the *current phase's* task, not policing a 6-step workflow → far more robust to model
  upgrades.
- The tool layer is UI-agnostic: chat, simplified flow, and future surfaces are thin front-ends over the same API.

### 4.3 Two LLM roles, kept separate (as in OpenRDR)

1. **Condition translator**: natural language ("the glucose is high and rising")
   → candidate condition-syntax strings, gated by `parseAndTestCondition`, with the gate's error messages fed back for
   bounded retry. Small prompt built from the predefined-function vocabulary plus the case's attribute names.
2. **Workflow assistant / explainer**: proposes the comment action, adapts comment wording to the pathologist's
   terminology, explains in clinical terms *why* a cornerstone conflicts and what an excluding condition would look
   like.

### 4.4 Chat for dialogue, panels for state

A hybrid surface, following OpenRDR's accidental lesson (its WebSocket cornerstone panel) deliberately. Conversation
handles intent and negotiation; existing structured components display what needs precision: the case data, the running
condition list (individually removable — addresses issue A-2), and the cornerstone diff (`CornerstoneDiffDTO` already
carries original/changed text and a report diff). A pathologist should never have to read a case out of a chat
transcript.

### 4.5 The reframe: report reconciliation as the unit of work

Rather than "help me build a rule", aim at **"help me reconcile the report"** — this directly dissolves issue A-1
(selected comment not fitting the existing KB report):

1. Show the current KB report and (optionally) the AI-generated report.
2. User converses with the AI to produce the **target report** — merging, rewording to preferred terminology, dropping
   stale comments. Pure text editing; low risk; pathologists think in reports, not rules.
3. The system **diffs target vs current KB report** into a plan:
   add X, remove Y, replace Z-with-W. Each diff item is exactly one rule session with a known action — the "doesn't fit"
   case becomes the core mechanic instead of an edge case.
4. Each planned rule runs through the machinery above: AI proposes conditions, engine validates, user adjusts,
   cornerstones reviewed (issue A-3 is stitched in naturally — each item is a normal session), explicit commit.

Rules are still built one at a time on a case, exactly as RDR theory requires; the AI only changes how intent is
captured. Provenance and audit remain intact via the existing command/audit layer.

## 5. Risks and flags

- **Model-version drift**: OpenRDR's history shows workflow behaviour shifts across model versions. The
  state-machine-in-code design reduces but does not eliminate this. Budget for model pinning plus a replay-style
  regression suite of recorded conversations (the `CannedLlmClient` test double and existing SSE wire tests are starting
  points).

