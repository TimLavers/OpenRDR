# One server per feature file in the cucumber suite

Implemented; checked against the code on **18 October 2026**. The latest full run passed **230/230 scenarios on
their first attempt** in **29m 47.294s**, including one automatic server replacement after a reset timeout.
See [latest validation](#latest-full-suite-validation-17-october-2026) and the
[full run note](../tickets/cuke_green_run_2026-10-17.md).

## Why

Previously, every cucumber scenario started its own server process and stopped it afterwards. The server is up in about
0.7 s and the
start-plus-shutdown round trip costs 1.8–2.3 s (measured 16 Oct 2026 by launching `server-all.jar` with `InMemory`
three times and polling `/api/ping`; the `@Before` hook also waits for `STARTING_SERVER` to appear in the log, which
adds a little). Over the 236 scenario runs measured then, that was roughly 7–9 minutes of a 37-minute run.
Reusing one server for all the scenarios of a feature file removes most of it: the suite has 55 feature files, so
about 55 starts remain, plus replacements after failed resets and starts in rerun JVMs.

A feature file, rather than a folder or the whole JVM, is the unit because it is what a developer runs by hand
(`cucumberSingleTest`, an IDE run of one `.feature`), so a reset gap shows up in the same run that caused it, and the
failure directory's `server.log` stays small enough to read.

The client is still started afresh for every scenario. That is window hygiene, not state: Robot keystrokes go to the
front-most window, and the page objects must address a window whose accessibility tree they know (see the comment in
`StepsInfrastructure.startClient`). Its cost is not measured here.

## What the server holds between scenarios

The KB and conversation state is owned by the one `ServerApplication` built in `Application.module()`. Requests in
flight can still hold references to that state, so clearing its maps is only safe after those requests have finished.
There is no file-system state apart from the log and the temp directories that export and import create and abandon.

| State                                                             | Where                                                                                       | Reset by                                                                                                                                                    |
|-------------------------------------------------------------------|---------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Knowledge bases: cases, rules, conditions, attributes, user lists | `KBManager.kbInfos`/`openKbs`, `ServerApplication.idToKBEndpoint`, the persistence provider | deleting every KB. `destroyKBPersistence` removes the in-memory KB, or drops the Postgres database, and clears the id store                                 |
| Rule sessions in flight (one per user per KB)                     | `KBSession.ruleSessionManagers`                                                             | destroyed with their KB. A failed scenario can leave one active; `deleteKB` refuses while *another* user is editing, so the reset must cancel first (below) |
| Chat conversations, one per user, never evicted                   | `ChatCoordinator.conversations`                                                             | `knowledgeBaseDeleted(kbId)` resets those on the KB; conversations in the `NoKnowledgeBase` context are not on any KB and need clearing explicitly          |
| Requests in flight, including chat turns waiting for Gemini       | route handlers and their `Conversation`/`ChatManager` references                            | stop admitting new requests and wait for admitted work to finish before deleting state; restart if the wait times out                                       |
| Connected client windows                                          | `WebSocketManager.connections`                                                              | stop the clients, then wait for the server to observe their disconnects before deleting state                                                               |
| `geminiApiCallCount`                                              | `io.rippledown.llm`, process-wide                                                           | monotonic. `ScenarioProgress` already diffs it against a reading taken at scenario start; nothing else reads it                                             |
| `temp/logs/server.log`, `temp/output.txt`                         | files held open by the server                                                               | not reset. `saveServerLogsOnFailure` must copy only the tail written during the failed scenario                                                             |
| `temp/`, `downloads/`                                             | managed by `DirProxy`; construction does not delete files                                   | `ServerProxy.start()` calls `createAndCleanManagedDirectories()` only when starting a process, after the previous process has exited                        |
| `ScriptedKbFileDialogs`, `useFakeVoice`                           | client side                                                                                 | already reset in `StepsInfrastructure.cleanup`                                                                                                              |

Demonstration KBs are read from resources on each request (`SampleKB`), not cached; the user-defined case lists and
favourites live in the KB.

## Design

### Server side: a reset route

A single `POST /api/reset` (constant `RESET` beside `PING` and `SHUTDOWN` in `constants/server`) that puts the server
back to the state it had just after start-up. Enable it only with an explicit `--test-reset` launch flag, independent
of the persistence choice. `InMemory` alone must not enable reset: the packaged demo uses that argument too.

`main` passes the flag into `module(enableTestReset = ...)`, whose default is `false`. With the flag absent, neither
the reset route nor its request gate is installed, and `POST /api/reset` returns `404`. The Cucumber `ServerProxy`
passes `InMemory --test-reset`; normal launches and the packaged demo omit `--test-reset`. A future Postgres test
server can opt in with the same flag. Test route availability with and without the flag for both persistence choices.

#### Finish outstanding work before resetting

Closing a client window does not prove that its server requests have finished. In particular, a Gemini call can be
blocked in `future.get()` while its chat turn retains a `Conversation` and `ChatManager`. Clearing `conversations`
does not cancel that turn; when it resumes it can still execute an action against the application.

Servers with reset enabled therefore have a request gate with an active-request count. Admission and incrementing
the count are atomic with closing the gate. Every application HTTP handler enters before looking up a KB or
conversation and leaves in `finally`, after all its work has finished, including any work that can mutate application
state. This covers reads as well as writes: reading a case can interpret it. `PING`, `SHUTDOWN`, and `RESET` remain
available outside the gate; only one reset can run at a time, and a concurrent reset is rejected. New WebSocket
registrations also use the gate, atomically with registration, while existing sessions can always disconnect.

Reset closes admission and rejects new application requests and WebSocket connections with `503`; it does not queue
them to execute after reset. An upgrade already admitted before reset carries the admission generation; if its
WebSocket registers after reset has begun or completed, close that session rather than attach it to the new state.
It waits up to five seconds, configurable in tests, for both the active-request count to
reach zero and `connectedUsers()` to become empty. The wait holds no KB or conversation locks, so existing work and
disconnect cleanup can complete. Cancelling a coroutine is not evidence that blocking work has stopped: the count
must cover the actual completion of the handler's work.

If either condition is still false at the deadline, return `409` with the remaining request count and connected users.
The reset has not yet deleted any KB, cancelled any rule session, or cleared any conversation. Keep admission closed
and mark the server unusable for further scenarios; the harness must terminate it and start a replacement. A second
reset cannot reopen it. This bounds recovery even when a Gemini call is stuck. Any reset failure or cancellation after
admission closes, including failure part-way through deleting state, leaves the server unusable and requires
replacement.

#### Reset state

`ServerApplication.reset()`:

1. With the request gate exclusively held closed and the bounded wait complete, check that no application request or
   WebSocket session remains. Use an unconditional check, not a JVM `assert`. This is a precondition to every state
   change below; rejecting this precondition must perform no reset mutations. Previously admitted requests may still
   be completing their own work until the process is terminated.
2. For every KB, under `kbSetLock` and the KB's own lock: cancel every user's rule session
   (`KBSession.cancelRuleSessionOf` for each `usersEditing()`), then `kbManager.deleteKB` and drop the endpoint. This
   is `deleteKB` without the project-held refusal and without the per-user web-socket notifications, which have nobody
   to go to once the clients are stopped.
3. `chatCoordinator.reset()`: clear `conversations`. `knowledgeBaseDeleted` is not enough because a
   conversation in the `NoKnowledgeBase` context has no KB id.
4. Reopen request admission only after reset succeeds. There is nothing left to remove from `WebSocketManager`:
   the precondition established that its sessions were already gone.

The route responds `200` only after all of that, so the test's next step sees an empty server.

Why a server-side reset rather than deleting KBs through the public API from the test (the plan in
`tickets/cuke_speedup_plan.md` step 3): `DELETE_KB` goes through one user's `KnowledgeBaseService` and refuses while
another user has a rule session, which is exactly the state a failed multi-user scenario leaves behind; the public path
also pushes `KB_CLOSED` to connected users and needs the caller to know every user id in play. Recovery after a failed
scenario must either establish an idle, empty server or replace its process; it cannot depend on the state being
well-formed.

### Postgres

No scenario is tagged `@database` today, so every cucumber server is in-memory. KB deletion continues to go through
`PersistenceProvider`: `destroyKBPersistence` drops a Postgres database and removes its entry from `PostgresKBIds`.
Whether a reset that drops and recreates databases is faster than a fresh server is unmeasured. If `@database`
scenarios return, retain their existing per-scenario server lifecycle until Postgres reset has been validated and
measured. The existing `@database` tag is enough to select that behavior.

### Test side

[StepsInfrastructure](../../cucumber/src/test/kotlin/steps/StepsInfrastructure.kt) delegates process ownership to
[FeatureServerLifecycle](../../cucumber/src/test/kotlin/steps/FeatureServerLifecycle.kt). The latter keeps the
`UITestBase` (and so the `ServerProxy`, `RESTClient`, `LabProxy`), feature URI, persistence mode and reuse eligibility.
It owns only the current feature's process; returning to a previous feature starts another process. Every process
replacement creates a new `UITestBase`: `ServerProxy.shutdown()` closes its `RESTClient` clients in `finally`, so
those objects must not be reused. Lifecycle operations are synchronized, including both run-cleanup hooks.

`Defs.before(scenario)`:

- If no reusable server is running, or `scenario.uri` differs from the running server's feature, or the persistence
  mode changes: stop any running server and confirm process exit, create a new `UITestBase`, then wipe `temp/` and
  `downloads/` as part of starting the server, and record the feature and mode. `@database` always takes this
  fresh-start path. Windows directory deletion can briefly fail even after process exit; retry it for up to two
  seconds, then fail setup if the directory still cannot be deleted. Validate that both directories resolve
  directly beneath the repository root before deleting either one. Readiness checks allow up to 30 seconds for the
  startup log message and a successful ping.
- Otherwise: call `restClient.reset()`, using a 15-second HTTP timeout, longer than the server's five-second drain
  deadline. On success clear the cached KB/case selection in `RESTClient` and its wrapped `Api` so no request can use
  a selection from the previous scenario.
- On a reset rejection, error, or timeout, preserve the reset diagnostics and log tail before wiping files, report
  the reason for replacing the server, and take the fresh-start path. Do not retry reset against that process or
  treat a successful ping as proof that it is reusable. If termination or replacement fails, fail scenario setup.
- Either way, take the Gemini-call and log-length readings for the scenario.

`Defs.after(scenario)`:

- Screenshot and log tail on failure, then stop all clients in cleanup that still runs if diagnostic capture fails.
  Clear `useFakeVoice` and `fileDialogs` as today. Keep a healthy in-memory server running; stop a Postgres server
  after its scenario. If client teardown fails, mark the server unusable and fail cleanup; a later scenario must
  establish that the old clients and process are stopped before starting a replacement.

Normal run cleanup uses a top-level Kotlin function annotated with `io.cucumber.java.AfterAll` in the `steps` glue
package. It calls `StepsInfrastructure.stopServer()` after the selected scenarios finish, including failed runs.
Cucumber supports this hook; a top-level function supplies the required static JVM method. See the
[Cucumber hook documentation](https://cucumber.io/docs/cucumber/api/#global-hooks).

Register one JVM shutdown hook as a fallback for exits that bypass `@AfterAll`. Both hooks call the same idempotent
cleanup, which also tolerates no server having started or startup having failed. The graceful `SHUTDOWN` request
has a two-second HTTP timeout. In `finally`, forcibly destroy any still-live process and wait up to ten seconds to
verify its exit. Synchronization prevents the two callers from shutting down or closing resources concurrently.
This cleanup also serves feature
changes and reset recovery. The folder task's rerun JVM is a separate process and gets its own server.

`saveServerLogsOnFailure` records `server.log`'s length at scenario start and writes only the bytes after it, so each
failure directory still holds exactly that scenario's log. `output.txt` is rarely useful; copy it whole.

`ScenarioProgress` continues measuring Gemini calls as a difference from a reading taken when the scenario starts.
It also records `serverStartSeconds`: zero for successful reuse, otherwise the elapsed time spent starting a new
process (including directory preparation and readiness checks).

### Isolation failures

There is no demonstrated need for an `@fresh-server` annotation, so this design does not introduce one. Fix reset gaps
when they are found. Automatic process replacement is recovery for a reset that cannot establish safe reuse, and its
diagnostics must make that event visible in the run output.

The folder task's rerun-on-failure runs the failed scenarios in a new JVM, so it can hide reset gaps. A scenario that
fails in the main run and passes on rerun warrants an isolation check, although ordinary flakiness can also cause
that pattern. `ScenarioProgress`'s CSV records both attempts, so this is visible after every run.

## Implementation and regression coverage

The reset route and gate are implemented in
[TestServerReset](../../server/src/main/kotlin/io/rippledown/server/routes/TestServerReset.kt) and
[TestRequestGate](../../server/src/main/kotlin/io/rippledown/server/TestRequestGate.kt).
The harness uses `FeatureServerLifecycle`, `RESTClient.reset()`, the scenario hooks, and
[RunCleanup](../../cucumber/src/test/kotlin/steps/RunCleanup.kt). `ScenarioProgress` writes `serverStartSeconds`
alongside scenario duration and Gemini calls in `cucumber/build/timings/<folder>.csv`.

Headless regression coverage includes:

- [TestRequestGateTest](../../server/src/test/kotlin/io/rippledown/server/TestRequestGateTest.kt): admitted work,
  delayed disconnects, drain timeout, cancellation, failed clearing, concurrent resets and stale WebSocket upgrades.
- [ResetRoutesTest](../../server/src/test/kotlin/io/rippledown/server/ResetRoutesTest.kt): explicit opt-in for both
  persistence choices, HTTP handler completion and exceptions, rejection during reset, and permanent closure after
  drain failure. These tests use controlled work rather than live Gemini calls.
- [ServerApplicationResetTest](../../server/src/test/kotlin/io/rippledown/server/ServerApplicationResetTest.kt):
  clearing KBs, endpoints and conversations, cancelling every user's rule session, and preserving state when a
  connected user prevents reset.
- [FeatureServerLifecycleTest](../../cucumber/src/test/kotlin/steps/FeatureServerLifecycleTest.kt): same-feature
  reuse, feature and persistence-mode changes, reset recovery and diagnostics, unhealthy servers, startup and
  termination failures, Postgres isolation, and idempotent cleanup.
- [ServerProxyTest](../../cucumber/src/test/kotlin/io/rippledown/integration/proxy/ServerProxyTest.kt): forced
  destruction after failed graceful shutdown, refusing replacement before process exit, and closing HTTP clients.

The initial targeted tests and full-suite measurements are recorded below; subsequent full runs are recorded in the
[run note](../tickets/cuke_green_run_2026-10-17.md).

## Decisions and open points

- **Reset requires explicit test opt-in.** `--test-reset` enables it; choosing `InMemory` does not. Normal and demo
  launches leave it disabled.
- **Reset waits for completed requests and observed disconnects before deleting state.** A bounded failure to reach
  that point requires a replacement process. It never permits reuse based only on having closed the client windows.
- **Run cleanup uses Cucumber's `@AfterAll`.** A JVM shutdown hook supplies fallback cleanup through the same
  idempotent, bounded operation.
- **Feature-file granularity, not folder.** Folder would save perhaps another 45 starts (~1.5 min) at the cost of
  logs that span a whole folder and a reset gap surfacing far from its cause. Not worth it.
- **Clients still restart per scenario.** Measured separately if it becomes the next largest fixed cost.
- **The conversation's opening exchange is unaffected**: every scenario starts the client, which starts a conversation
  in a fresh `ChatManager`; nothing about the previous scenario's chat survives `ChatCoordinator.reset()`.
- **No `@fresh-server` annotation.** No scenario has demonstrated a need for it.

## Latest full-suite validation (17 October 2026)

The repeat `gradlew cuT` run at revision `3cbd029c` passed **all 230 scenarios across 14 folders on their first
attempt**, with **zero scenario failures or retries**. It ran from **20:06:01.974 to 20:35:49.269 AEDT**:
**29m 47.294s** from Gradle wrapper start to exit. Gradle itself reported **29m 46s** and exited successfully.

The timing CSVs record **56 server starts**, **174 successful reuses**, **39.540 seconds** spent starting servers,
and **856 Gemini calls**. The 56 starts comprise 55 feature starts and one automatic replacement: before
"The current cornerstone should remain selected if the user removes a condition", reset reached its five-second
deadline with **one active request and no connected users**. The harness preserved diagnostics, replaced the server,
and the scenario passed on that same attempt. This exercised the designed recovery path; the run was not free of
reset timeouts. Completion checks found no remaining Cucumber or server process and no listener on port 8080.

The earlier tooltip failures listed below were addressed by mouse-free tooltip assertions. French and Spanish
feature expectations were also corrected to match the canonical `Age < 50.0` text. The intermittent empty-KB UI
timeout did not recur in this repeat run; that does not establish its root cause.

The [run note](../tickets/cuke_green_run_2026-10-17.md) contains per-folder results and distinguishes this run from
the earlier **24m 6s** run, which became green after three retries. These wall-clock results are not a controlled
performance comparison: Gemini calls, latency, machine load and retries vary between runs.

Local evidence: [archived timing CSVs and run summary](../../build/cucumber-monitor-20261017-200601/) and
[Gradle output, including reset recovery](../../build/cucumber-monitor-20261017-200601/daemon.log).

## Initial implementation validation (17 October 2026)

Implemented the opt-in reset route and request gate, per-feature in-memory server lifecycle, per-scenario Postgres
lifecycle, reset recovery, bounded process cleanup, scenario log tails, and startup timing. No `@fresh-server` tag
was added. The initial KB run exposed intermittent Windows directory deletion failures after process exit; a
regression test and the bounded deletion retry above address that failure.

- **125 targeted unit tests passed**, covering reset admission/draining, state and rule-session cleanup, route opt-in,
  existing server/chat/WebSocket behavior, lifecycle replacement, failed termination, client selection reset,
  directory cleanup, log tails, and timings.
- **Cucumber dry run passed:** 230 scenarios and 3,013 steps have definitions.
- **Full run:**
  `./gradlew.bat :cucumber:cases :cucumber:rulebuilding :cucumber:cucumberTest --continue --console=plain`.
  All 230 scenarios were attempted, with five retries: 227 scenarios passed after retries and three remained failed.
  The Gradle command therefore exited unsuccessfully. Each remaining failure also appears in the recorded baseline
  and reproduced in the folder's fresh-process retry:
    - `rulebuilding`: "The user should be able to build a rule to add a comment with a condition they have selected"
      (condition tooltip lookup).
    - `chat`: "The user should be able to use the chat to add a comment with a valid condition"
      (condition tooltip lookup).
    - `conditions`: "Some suggested conditions can be modified before being added".
      Attribute filtering had a Compose accessibility exception and the empty-KB creation scenario had a UI lookup
      timeout; both passed on retry. The latter's server log shows a fresh no-KB conversation followed by successful
      KB and demonstration-case creation.
- **No reset or directory cleanup failures in the full run.** It used 60 process starts (55 feature starts plus five
  reruns), with 175 successful reuses. Total `serverStartSeconds` was **42.57 seconds**. Run cleanup left no server
  process or listener on port 8080.

The full run took **27m 17s**, versus **42m 33s** in the 15 October baseline. This is not an isolated before/after
measurement of server sharing: Gemini calls also fell from 1,454 to 878, and the baseline had 238 attempts rather
than 235. The startup count and `serverStartSeconds` directly measure this implementation; the overall wall-clock
difference cannot all be attributed to it.

The table sums per-scenario whole-second CSV durations, including retries. Startup times retain fractional seconds.

| Folder         | Attempts | Baseline scenario seconds | Initial run seconds | Server starts | Startup seconds |
|----------------|---------:|--------------------------:|--------------------:|--------------:|----------------:|
| attributes     |        5 |                        17 |                  17 |             3 |            2.08 |
| cases          |       25 |                       133 |                  54 |             4 |            2.75 |
| chat           |       44 |                       532 |                 381 |            14 |           10.21 |
| comments       |       15 |                       151 |                 118 |             2 |            1.41 |
| conditions     |       17 |                       159 |                 138 |             3 |            2.14 |
| demo           |        1 |                        59 |                  41 |             1 |            0.72 |
| inferencing    |       19 |                        99 |                  51 |             3 |            2.11 |
| interpreter    |        3 |                        14 |                  11 |             1 |            0.71 |
| kb             |       57 |                       332 |                 207 |             7 |            4.88 |
| report         |        2 |                        49 |                  31 |             1 |            0.70 |
| rulebuilding   |       35 |                       468 |                 359 |            10 |            7.31 |
| samples        |        9 |                       385 |                  81 |             9 |            6.15 |
| trial-matching |        1 |                        14 |                  12 |             1 |            0.71 |
| voice          |        2 |                         7 |                   2 |             1 |            0.71 |
| **Total**      |  **235** |                 **2,419** |           **1,503** |        **60** |       **42.57** |

Local evidence for the initial run: [full Cucumber output](../../build/shared-server-full-cucumber.log) and
[unit-test output](../../build/shared-server-regressions.log). The live
[timing directory](../../cucumber/build/timings/) is overwritten by later runs; it is not an archive of this initial
run.
