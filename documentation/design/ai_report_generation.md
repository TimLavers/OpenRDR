# AI report generation

Requirements: [ai_report_generation.md](../requirements/ai_report_generation.md).

## Shape

```
ReportView (client) → GET /api/caseReport → KBEndpoint.caseReport (cache) → ReportService.generate → Gemini generateText
```

The client owns the panel's visibility and decides when to fetch; the server owns the model call and a per-case cache.
The wire type is `CaseReport(markdown, generated)`, where `generated = false` means "no comments, so nothing was
produced" as distinct from "the model returned nothing".

## Decisions

- **Server-side generation.** The comments, the case serialisation and the Gemini key are all on the server; a client
  call would leak the key and duplicate the serialisation.
- **A one-shot call, not the chat machinery.** A report is a stateless request; the multi-turn function-calling loop
  built for rule building would add conversational state for no benefit. `generateText` in `llm` is the primitive, with
  temperature 0 so that output is stable and caching is meaningful. `ReportService` wraps it in a tighter retry and
  timeout than the batch defaults because this is an interactive path whose worst case must stay bounded.
- **The model sees the comment texts and the whole case.** Sending the case lets the report quote actual values. The
  comments' *names* are not yet sent, although now that comments are named attributes the name is a useful signal
  about a comment's role.
- **Cache keyed on the hash of the comment text.** The report is a function of the comments, so this is exactly the
  right invalidation: unchanged comments hit; a rule that changes a comment misses. Keying on the case id alone would
  serve stale reports after a rule; keying on the whole case would regenerate on every unrelated refresh, such as each
  chat message.
- **Visibility gates generation.** The panel is collapsed by default and nothing is fetched while it is, so browsing is
  free. The client alone knows whether the panel is open, which is why the trigger lives there: an effect keyed on
  visibility, case id, the case's comment text and whether a rule is in progress.
- **Hidden during a rule session.** Not regenerating during a session would leave a stale report beside a changing
  interpretation, so the panel is removed and comes back, regenerated, when the session ends.
- **Out-of-range highlighting by the model, not by post-processing.** The prompt asks for Markdown with short headings,
  bullets and bold for out-of-range values; a custom annotator renders bold in the case view's red. The case JSON gives
  the model each result's reference range, whereas locating values in free prose deterministically is unreliable when
  two attributes share a value. This is a trial and may be revisited.
- **Failure is non-critical.** The route logs the cause and returns 500 without the message; the client renders the
  empty state. A report is an aid to review, never something the workflow depends on.

## Rendering

The Markdown renderer does not expose text to the accessibility bridge, so the formatted text is also placed on a hidden
node for the page objects and unit tests. `10^N` and the `u` micro prefix are rewritten (`10¹²`, `μmol/L`) to match the
case view's units cell.
