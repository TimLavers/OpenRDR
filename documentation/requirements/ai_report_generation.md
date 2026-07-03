# AI Report Generation

When reviewing a case, the expert sees the interpretive report as a set of comments given by the
rules in the knowledge base. These comments are concise, rule-authored sentences. The AI report
generation facility produces a longer, well-worded prose **report** for the case, written by the
application's large language model (LLM) from those comments together with the case's data.

The report is presented in a collapsible **Report** panel shown beneath the existing Interpretation
panel in the case view. It is intended as a readable, narrative summary of the case that a clinician
could use directly, distinct from the terse rule comments.

## Functionality

| Requirement          | Description                                                                                                                                                                               | Validation       |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| Report panel         | A collapsible **Report** panel is shown beneath the Interpretation panel for the current case.                                                                                            | `Report.feature` |
| Toggle visibility    | The user can show or hide the report by clicking the panel's header (a chevron and the label "Report").                                                                                   | `Report.feature` |
| Report content       | When shown, the report is a well-worded prose narrative derived from the comments given by rules for the case, and may refer to the case's attribute values to make the wording concrete. | `Report.feature` |
| Grounded in comments | The report is based only on the comments given for the case; it does not introduce findings that are not implied by those comments.                                                       |                  |
| No comments          | If the case has no comments, the panel indicates there is nothing to report on rather than generating a report.                                                                           | unit test only   |
| Long reports         | The panel is bounded in height and scrolls internally so that a long report never pushes the case data off-screen.                                                                        |                  |

## When the report is generated

To keep cost and latency down, report generation is deliberately limited:

| Requirement               | Description                                                                                                                                                  | Validation       |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| Only when visible         | The report is generated only when the Report panel is visible. Selecting cases while the panel is collapsed generates no reports.                            |                  |
| Only when comments change | A report is regenerated only when the comments for the case change. Re-selecting a case whose comments are unchanged reuses the previously generated report. |                  |
| Not during rule building  | No report is generated while a rule-building session is in progress.                                                                                         |                  |
| Reflects comment changes  | After the user builds, removes or replaces a comment via a rule, the report for the case updates to reflect the new comments.                                | `Report.feature` |

## Failure behaviour

| Requirement      | Description                                                                                                                                                                                                                            | Validation |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| Graceful failure | If report generation fails (for example, the LLM call errors or times out), the failure is non-critical: the panel shows an empty-report state rather than surfacing an error, and the underlying cause is recorded in the server log. |            |
