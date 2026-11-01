# Testing

## Test layers

| Layer | Location and requirements |
|---|---|
| Unit tests | Module `src/test` directories. New tests use Kotlin, JUnit 5, Kotest and Mockk; legacy UI tests use JUnit 4 through the Vintage engine. |
| Compose UI tests | `ui/src/test`; many open desktop windows. Pure presentation helpers also have headless tests. |
| Store integration tests | `server/src/test/.../persistence/postgres`; require configured Postgres and database privileges. |
| Model integration tests | Selected `chat`, `hints`, `llm` and `server` tests use Gemini and require `API_KEY`. Mocked tests do not establish model quality. |
| Acceptance tests | Features under `cucumber/src/test/resources/requirements`, run through Cucumber tasks. Many use the desktop and live Gemini. |
| Packaging smoke test | `packaging:test` builds the demo ZIP, extracts it and starts its server. |

Follow [AGENTS.md](../../AGENTS.md), including approval before running more than one UI unit test or one Cucumber
test. Compiling test sources does not execute them. Keep incidental console prints out of unit tests; use assertions
and failure clues. Preserve logging assertions and acceptance-harness failure diagnostics.

## Commands

Run from the repository root. Use `./gradlew` on Unix or `.\gradlew.bat` in PowerShell; no separate Gradle install
is needed. Select known deterministic tests when credentials, a database or desktop access are unavailable.

```powershell
.\gradlew.bat :common:test
.\gradlew.bat :server:test --tests 'io.rippledown.kb.KBSessionTest'
.\gradlew.bat :llm:test --tests 'io.rippledown.llm.GeminiBooleanClassifierTest'
.\gradlew.bat :ui:compileTestKotlin :cucumber:compileTestKotlin
```

A bare `test` may select several modules, including desktop and packaging tests; use module-qualified tasks.
These acceptance commands are alternatives for when the desktop is available and required approval is given:

```powershell
.\gradlew.bat :cucumber:cucumberSingleTest
.\gradlew.bat :cucumber:chat
.\gradlew.bat :cucumber:cucumberFolderTest -Pfolder=chat
.\gradlew.bat :cucumber:cucumberTest
```

`cucumberSingleTest` selects `@single` scenarios/features. Named folder tasks such as `:cucumber:chat` retry failed
scenarios once in a fresh JVM; `cucumberFolderTest` does not add that retry. `cucumberTest` runs all named folder tasks.
`cucumberDryRun` checks bindings without scenario hooks, desktop interactions or model calls:

```powershell
.\gradlew.bat :cucumber:cucumberDryRun
```

The [AI demo guide](../ai_derived_attribute_demo.md) documents its desktop task and opt-in headless authoring and
model-quality tests. Build a demo with `:demoZip`; `:verifyDemoZip` runs its smoke test.

## Observing the client

Page objects use shared accessibility IDs, Java accessibility and Robot for input. Where the bridge cannot expose
state reliably, hooks such as `ChatTestHook` and `CornerstoneTestHook` provide structured snapshots. Hooks are keyed
by user so two windows do not overwrite each other's observations. Tooltips can be checked without moving the pointer.
OCR through Gemini remains a fallback for rendered content, not the default polling path.

Page objects are scoped to a list (`ProcessedCaseListPO`, `CornerstoneCaseListPO`, `UserCaseListPO`) because names
can appear in several lists. Hidden text/semantics expose Markdown and pending-change state where needed.

`@file-dialogs-are-fake` scripts native file selection while retaining real transfer behaviour. `@voice-is-fake`
installs fake recognition for deterministic voice scenarios. Untagged voice input uses the microphone and Gemini;
transcription appears only after recording stops.

## Isolation and evidence

The harness reuses an in-memory server within a feature, resets between scenarios and starts fresh clients.
Postgres scenarios use separate server lifecycles. See [shared server design](shared_cucumber_server.md).
Multi-user REST scenarios exercise independent identities; a two-window scenario verifies presence, notices and
refresh. See [concurrent users](concurrent_users.md#testing).

`ScenarioProgress` writes duration, Gemini calls and server-start duration to `cucumber/build/timings/<folder>.csv`.
Failure artifacts include a screenshot and the scenario's server-log tail. Folder retries append timing rows, so a
green result after retry is not a failure-free first run. Build reports and logs are local and may be overwritten;
retain evidence separately when comparing runs.
