# AI report generation

The comments the rules give a case are terse, rule-authored sentences. The **Report** panel shows a prose report for the
case, written by the language model from those comments and the case data, in the register a clinician would use. It is
a readable summary for review, not a replacement for the comments, which remain the expert's unit of correction.

| Requirement               | Description                                                                                                                                                              | Validation                                                    |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| Panel                     | A collapsible Report panel beneath the Comments panel, toggled from its header.                                                                                          | `report/Report.feature`                                       |
| Content                   | Prose derived only from the comments given for the case, made concrete with the case's values, in Australian medical terminology and spelling.                           | `report/Report.feature` (grounding cannot be tested reliably) |
| Formatting                | Rendered as Markdown; out-of-range values in bold red as in the case view; `10^12` and `umol/L` rendered as `10¹²` and `μmol/L`.                                         | unit tests                                                    |
| No comments               | With no comments the panel says there is nothing to report on, and no model call is made.                                                                                | unit tests                                                    |
| Progress                  | The chat's typing indicator is shown while the report is generated.                                                                                                      | unit tests                                                    |
| Copy                      | A copy icon puts the report text on the clipboard and briefly confirms.                                                                                                  | unit tests                                                    |
| Disclaimer                | An information icon beside the heading says on hover that the report is AI-generated, may be wrong, and must be reviewed before release.                                 | unit tests                                                    |
| Long reports              | The panel is bounded in height and scrolls internally, so the case data is never pushed off screen.                                                                      | unit tests                                                    |
| Only when visible         | A report is generated only while the panel is open, so browsing cases with it closed costs nothing.                                                                      | unit tests                                                    |
| Only when comments change | Re-selecting a case whose comments are unchanged reuses the report; a rule that changes a comment regenerates it.                                                        | `report/Report.feature`                                       |
| Not during a rule         | The panel is hidden while a rule is being built, since the interpretation is in flux and a stale report would mislead; it reappears, regenerated, when the session ends. | unit tests                                                    |
| Failure                   | If generation fails the panel shows the empty state and the cause goes to the server log; the report is never critical.                                                  | unit tests                                                    |

**Not implemented:** choosing which attributes are sent to the model; sending the comments' names as well as their
texts; showing or hiding the panel from the chat.

Design: [design/ai_report_generation.md](../design/ai_report_generation.md).
