---
trigger: always_on
---

# Running the build and the tests

Run gradle from the repo root with `.\gradlew.bat` and `--console=plain`.

## Unit tests

- After each step of work: `.\gradlew.bat :common:test :server:test`.
- The `io.rippledown.persistence.postgres.*` tests need a local Postgres. The user has one, so a plain `:server:test`
  works for them; from your shell there is none, and the tests block on connecting rather than failing, so you must
  exclude them. `--tests` does NOT support negation (`--tests "!io.rippledown.persistence.postgres.*"` is silently a
  no-op), so name the packages instead:

```
.\gradlew.bat :server:cleanTest :server:test --tests "io.rippledown.kb.*" --tests "io.rippledown.model.*" ^
  --tests "io.rippledown.server.*" --tests "io.rippledown.suggestions.*" --tests "io.rippledown.util.*" ^
  --tests "io.rippledown.persistence.inmemory.*" --console=plain
```

- Do not put `--tests` on a command that also runs `:common:test` — no matching tests is a build failure.
- Always include `:server:cleanTest`: a killed run leaves the test task wrongly UP-TO-DATE.
- Long runs: start the command non-blocking, pipe it to a file, then read the file. Count results by summing
  `tests` / `failures` over `<module>/build/test-results/test/**/*.xml`.
- A hung daemon is cleared with `.\gradlew.bat --stop`.

## Coverage (Kover, on `common`, `server`, `hints` and `chat`)

- `common`: `.\gradlew.bat :common:koverHtmlReport --console=plain`.
- `hints`, `chat`: same form, unfiltered. Their tests take about five minutes together, so run these when a class in
  them was changed, not routinely.
- `server`: `koverHtmlReport` runs `:server:test`, so from your shell put the `--tests` filters for the classes under
  review on the same command line (the Postgres constraint above), e.g.
  `.\gradlew.bat :server:cleanTest :server:test --tests "io.rippledown.kb.chat.*" :server:koverHtmlReport --console=plain`.
- Report: `<module>/build/reports/kover/html/index.html`. Read the class pages of the classes you touched; the module
  total is meaningless under a filter.

## Cucumber

- Compile check: `.\gradlew.bat :cucumber:compileTestKotlin`.
- Every step bound: `.\gradlew.bat :cucumber:cucumberDryRun`.
- One scenario tagged `@single`: `.\gradlew.bat :cucumber:cucumberSingleTest`.
- One folder: `.\gradlew.bat :cucumber:chat` (a task is registered per folder; the folders are listed in
  `cucumber/build.gradle.kts` as `featureFolders`), or `.\gradlew.bat :cucumber:cucumberFolderTest -Pfolder=chat`.
- Every folder: `.\gradlew.bat :cucumber:cucumberTest`.
- Real cucumber runs need a live server, the LLM and the GUI, so ask the user to run them.
