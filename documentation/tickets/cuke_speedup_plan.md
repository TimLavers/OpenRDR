# Cuke speed-up: backdoor setup and a shared server per folder

Throwaway plan. Delete when the work lands.

## Baseline (measured 15 Oct 2026, full `cucumberTest`; CSVs in `cuke_baseline_timings/`)

Wall clock 42m 33s. 238 scenario runs (230 + 8 reruns), 2419 s inside scenarios, **1454 Gemini calls**.

| folder         | scenarios | minutes | Gemini calls | failed              |
|----------------|-----------|---------|--------------|---------------------|
| attributes     | 4         | 0.3     | 9            |                     |
| cases          | 25        | 2.2     | 74           |                     |
| chat           | 44        | 8.9     | 298          | 1 (×2)              |
| comments       | 15        | 2.5     | 97           |                     |
| conditions     | 17        | 2.6     | 94           | 1 (×2)              |
| demo           | 1         | 1.0     | 30           |                     |
| inferencing    | 19        | 1.6     | 68           |                     |
| interpreter    | 3         | 0.2     | 10           |                     |
| kb             | 57        | 5.5     | 170          | 1 (passed on rerun) |
| report         | 2         | 0.8     | 25           |                     |
| rulebuilding   | 38        | 7.8     | 285          | 4 (3 ×2)            |
| samples        | 10        | 6.4     | 291          | 1 (passed on rerun) |
| trial-matching | 1         | 0.2     | 1            |                     |
| voice          | 2         | 0.1     | 2            |                     |

What the numbers say, against the estimates in the analysis:

- **Infrastructure is cheap, not 15–20 min.** The nine scenarios that make no Gemini call take 0–1 s each *including*
  the server start and client start (both inside the measured interval); the 43 one-call scenarios average 2 s.
  Scenario time (2419 s) is within 134 s of the wall clock. Fixed cost of the whole run ≈ 4–6 min. **So step 3 (shared
  server) would save 2–3 min at the cost of inter-scenario state; drop it.**
- **Gemini is ~85 % of the run**: 1454 calls at ≈1.5 s. Far more than the ~1050 estimated.
- **The opening greeting is the single largest item.** `samples` is 291 calls for 10 scenarios, and the two Zoo
  scenarios are 84 calls each — one per `I select case …`, nothing else. Across the suite the greeting is
  roughly 500 calls ≈ 12 min. This is the lever that was "not in this plan" (`ChatCoordinator.greetingFor`
  returns null for `CaseInKnowledgeBase`, so the model writes the greeting; the other two contexts are canned).
- Step 1 (Given-position rule builds, ~100 calls) ≈ 2.5 min. Step 2 (copy-to-list, ~25 calls) ≈ 40 s.
- Failures are pre-existing flakiness or bugs, not caused by the instrumentation (which only reads the log):
  `Show conditions for rule action` ×3 and `Build rule to add comment` (condition selected) failed on both
  attempts; worth a look separately.

## Step 0 — measure (done)

- `steps/ScenarioProgress.kt` prints each scenario's duration and Gemini calls, appends a row to
  `cucumber/build/timings/<folder>.csv` (`folder,feature,scenario,status,seconds,geminiCalls`) and prints the run
  totals. The folder task deletes its CSV before the run; the rerun JVM appends. The dry run writes none.
- Calls are counted as `Gemini API call #N` lines in `temp/logs/server.log` (`ServerProxy.geminiCallsSoFar`,
  `GeminiCallCounter.kt`), as a difference from the reading taken in `StepsInfrastructure` once the server is up —
  so the same code measures a shared server later. The line is written by `callWithTimeout`, which every Gemini
  request goes through (chat, condition parsing, report). Its logger had to be renamed under `io.rippledown` for
  logback to route it to the file.
- Check: `demo` scenario = 42 s, 30 calls.
- Baseline: run `.\gradlew.bat :cucumber:cucumberTest`, then summarise:

```
Import-Csv cucumber\build\timings\*.csv | Group-Object folder | ForEach-Object {
  [pscustomobject]@{ folder=$_.Name; scenarios=$_.Count
    minutes=[math]::Round(($_.Group | Measure-Object seconds -Sum).Sum/60,1)
    gemini=($_.Group | Measure-Object geminiCalls -Sum).Sum } } | Format-Table -AutoSize
Import-Csv cucumber\build\timings\*.csv | Measure-Object seconds, geminiCalls -Sum | Select-Object Property, Sum
```

(`Import-Csv` needs `Get-ChildItem … | ForEach-Object { Import-Csv $_.FullName }` for several files.)

## Step 1 — backdoor the *Given*-position rule building (~100 calls, no production change)

Existing steps in `steps/BackdoorRuleStepDefs.kt` go through `RuleSessionManager.buildRule`, which parses conditions
deterministically — no Gemini. Convert only steps that set up the state the scenario then tests; leave the `When`
under test on chat.

Candidates (file:line of the `And I build a rule …` / `And I start to build …` setup step):

| Feature                                                 | Lines                | Note                                                                                                                      |
|---------------------------------------------------------|----------------------|---------------------------------------------------------------------------------------------------------------------------|
| chat/Naming and renaming                                | 45                   |                                                                                                                           |
| comments/Add, replace or remove comments with variables | 24, 36, 92, 111, 147 | template comments are fine: `buildRule` runs `internalForm` on the text, so `{wave}` becomes a `CommentTemplate` variable |
| comments/Help for comments with variables               | 27, 37               | 27 is unconditioned                                                                                                       |
| kb/Comment ordering                                     | 14                   | 15 then needs the plain (non-"another") chat variant                                                                      |
| kb/Cornerstone review                                   | 19, 147              |                                                                                                                           |
| report/Report                                           | 10, 13               |                                                                                                                           |
| rulebuilding/Build rule to add comment                  | 26, 62               |                                                                                                                           |
| rulebuilding/Build rule to replace comment              | 37                   |                                                                                                                           |
| rulebuilding/Build rule                                 | 28, 66               |                                                                                                                           |
| rulebuilding/Cornerstone cases                          | 15                   | 19 then needs the plain variant                                                                                           |
| rulebuilding/Undo rule                                  | 17                   |                                                                                                                           |

Mechanics:

- Use `a backdoor rule is built for case {word} to add the comment {string} with conditions:`; for unconditioned
  rules add a `… with no conditions` variant (exists for formulas, not for comments).
- After a backdoor setup the conversation has no suggestion list, so a following `I build another rule …` (which
  waits on a stale list) must become `I build a rule …`. Check each of the "another" variants in
  `RuleMakerStepDefs.kt` for this assumption.
- The chat's system prompt carries the case's comments at `startConversation`; a backdoor rule built *before*
  `I start the client application` is seen. One built after the client started needs a case re-select (the UI
  re-fetches the case after every chat message, and `startConversation` fires on a case change). Keep setup before
  the client start where the scenario allows; otherwise re-select the case.
- Verify each converted feature with its folder task; `cucumberDryRun` for binding.

Skip: anything where the chat-built rule *is* the behaviour (the `When` lines in the same files, the whole `chat`,
`conditions`, `inferencing` folders).

## Step 2 — backdoor copy-to-list in `cases/User defined case lists.feature` (~20 calls)

- New route `COPY_CASE_TO_LIST` (PUT, `KB_ID`, body `{caseId, listName, newName?}`) → `KBEndpoint` →
  `RuleSessionManager.copyCaseToList` (so `sendCasesInfo` still pushes to the client — that push is what the list
  assertions exercise). Add `RESTClient.copyCaseToList(caseName, listName, newName)` and a step
  `the case {word} is copied to the {string} case list( with name {string})` in `ChatCaseDefs.kt` or a new
  `BackdoorCaseListStepDefs.kt`.
- Keep on chat: scenario 1 ("A case can be copied …", the canonical chat path), "Cases cannot be copied to the
  cornerstone cases list" (asserts the chat refusal), and the export scenario's chat tail. Convert the rest.
- Server test for the route in `KBEndpointTest`/route test, same shape as the `BUILD_RULE` tests.

## Step 3 — one server per cucumber JVM, reset between scenarios (~10–15 min)

Design:

- `StepsInfrastructure` keeps the `UITestBase`/`ServerProxy` across scenarios. `Defs.before` becomes:
    - `@database` or `@fresh-server` tag, or no server running → start a fresh server as today.
    - otherwise → `resetServer()`.
- `resetServer()` is client-side, no production change: `restClient.kbNames().forEach { restClient.deleteKB(it) }`
  then clear `RESTClient.currentKB`. The `DELETE_KB` route goes through the user's `KnowledgeBaseService`, so
  `ChatCoordinator.knowledgeBaseDeleted` resets every conversation on the KB (`Concurrent Users` scenarios have
  several user ids — all covered, since reset is per KB not per user). The client is still restarted per scenario
  (window hygiene — see the comment in `startClient`), so `startConversation` is fresh anyway.
- `@After`: stop the clients only; stop the server in a JVM shutdown hook (or the last `@After` — cucumber has no
  "after all", so a `Runtime.addShutdownHook` in `StepsInfrastructure` is simplest).
- `@database` scenarios: start the Postgres server, and mark the server as needing a fresh start for the next
  scenario (the in-memory path must not inherit it).
- `DirProxy` wipes `temp/` and `downloads/` on construction — only do that when a server is actually started.
  `LabProxy.provideCase` is REST, so no case files leak between scenarios.
- `saveServerLogsOnFailure` copies the whole `server.log`; record the log length at scenario start and copy only
  the tail, or accept the larger file. `ScenarioProgress` from step 0 already needs the start offset.

What leaks and how it is handled:

| State                                                | Handling                                                                                                                                          |
|------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| KBs, cases, rules, conditions, attributes            | deleted with the KB                                                                                                                               |
| chat conversations (`ChatCoordinator.conversations`) | reset by `knowledgeBaseDeleted`; verify no-KB greeting still fires for "The option to create a KB is given if there are none"                     |
| `KBManager` id counter / name index                  | deletion removes the entries; ids need not restart at 1 — check nothing asserts an id                                                             |
| rule sessions in flight                              | a failed scenario can leave one; `deleteKB` runs under the KB lock and destroys the session with the KB — confirm in `ServerApplication.deleteKB` |
| `geminiApiCallCount`                                 | monotonic across scenarios; step 0 diffs it, nothing else reads it                                                                                |
| `ScriptedKbFileDialogs`, `useFakeVoice`              | client-side, already reset in `cleanup`                                                                                                           |

Escape hatch: `@fresh-server` tag for any scenario that proves order-sensitive. The existing rerun-on-failure in the
folder task reruns failed scenarios in a new JVM (fresh server), which also masks order bugs — watch for scenarios
that only pass on rerun; that is the signal that a reset is incomplete.

Verification: run each folder task; the per-folder times from step 0 give the saving. Run `kb` first — it is the
folder that mutates KBs most and will expose reset gaps.

## Not in this plan (separate decisions)

- Deterministic case greeting in `ChatCoordinator.greetingFor` (~250 calls, a product change).
- Backdooring `rulebuilding` `When` steps (grey zone, scenario by scenario).

## Order

0 → 3 → 1 → 2. Step 3 is the largest saving and independent of the others; doing it second lets steps 1–2 be
measured on the faster harness.
