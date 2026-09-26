# Testing

## Layers

| Layer                | Where                                      | What it pins                                                                                                                         |
|----------------------|--------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Unit tests           | each module's `src/test`                   | Every class; JUnit 5, kotest matchers, mockk. Chat policy classes are tested without a model; the model is stubbed at `ChatService`. |
| Compose UI tests     | `ui/src/test`                              | Panels and their pure merge functions, through the Compose test rule.                                                                |
| Postgres store tests | `server/src/test/.../persistence/postgres` | Need a local Postgres; the in-memory stores share the same contract tests.                                                           |
| Acceptance tests     | `cucumber/`                                | The requirements, as Gherkin, against the real server, client and language model.                                                    |

The acceptance features under `cucumber/src/test/resources/requirements/` are the executable requirements; the
requirements docs point at them by name. A folder per area maps to a Gradle task per folder, so one area can be run
alone.

## Driving the real client

The cucumber steps launch the Compose Desktop client and read and operate it through the **Java Accessibility API**:
every element a test needs carries a content description or test tag from the shared constants in `common`, so the same
identifiers are used by the UI, its unit tests and the page objects. Typing and clicking go through the accessibility
tree or `java.awt.Robot`.

Page objects are scoped to a section (`ProcessedCaseListPO`, `CornerstoneCaseListPO`, `FavouriteCaseListPO`) because a
case name can appear in more than one list. State that is invisible to the accessibility tree — a background colour, a
Markdown-rendered report — is exposed as a content description or on a hidden text node
([previewing_pending_changes.md](previewing_pending_changes.md), [ai_report_generation.md](ai_report_generation.md)).

Where the accessibility tree cannot give a test what it needs, the last resort is **OCR** of a screenshot of the
component's bounds, using Gemini. Gemini was chosen over Tesseract so that nothing has to be installed and configured on
a developer's machine; it has not proved too slow or costly, and reading what a human actually sees has some value of
its own. It is used sparingly, since polling a component through a model call is slow; counting nodes in the
accessibility tree is preferred wherever possible.

Native file dialogs are stubbed at the dialog boundary (features tagged `@file-dialogs-are-fake`), so the HTTP transfer
and application state stay real while the selection is scripted.

## The model in tests

Acceptance tests run against the live Gemini model, so a scenario can fail through model variance. The steps therefore
wait for server-composed messages (the reason acknowledgement, the cornerstone status, the completion message) rather
than for model prose, and the chat carries deterministic text wherever the server has decided the outcome. Unit tests
never call a model.

Some client-side behaviour that depends on the model is checked through `ChatTestHook`, which exposes the most recent
structured response (suggestions, KB listing) to the page objects.
