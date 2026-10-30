# Shared Cucumber server

The harness reuses an in-memory server within one feature file, resetting application state between scenarios.
Clients still restart for each scenario. Feature changes and Postgres scenarios use fresh server processes.
This reduces startup overhead while keeping failures close to the feature that produced them.

## Explicit test mode

`ServerProxy` launches the server with `InMemory --test-reset`. The flag enables `POST /api/reset` and
`TestRequestGate`; `InMemory` alone does not. Ordinary launches and the demo omit the flag, so reset returns 404.
The gate also supports an explicitly opted-in Postgres server, but the harness retains per-scenario Postgres isolation.

## Reset protocol

Closing windows does not prove their requests have finished: a chat turn can still be waiting for Gemini.
Reset therefore follows these steps:

1. Close request admission. New application requests and WebSocket registrations are rejected with 503.
   Ping, shutdown and reset remain reachable; concurrent resets are rejected.
2. Wait up to five seconds for admitted handlers to complete and connected users to disappear. Existing work
   and disconnect cleanup can finish without acquiring a reset-held KB or conversation lock. A delayed WebSocket
   upgrade from an earlier admission generation cannot register against the new state.
3. Under the KB-set and per-KB locks, cancel rule sessions, delete every KB and remove its endpoint.
4. Clear all conversations, including those without an open KB.
5. Reopen admission and return 200 only after successful clearing.

The request count covers completion of handler work, not merely coroutine cancellation. A drain timeout returns
409 with diagnostics before reset mutates KB state. Timeout, cancellation or clearing failure leaves admission
closed and the process unusable for subsequent scenarios. The harness replaces it; another reset cannot revive it.

Deleting KBs through the normal public API is not an adequate reset: it refuses deletion during another user's
rule session and does not clear conversations with no KB.

## Harness lifecycle

[FeatureServerLifecycle](../../cucumber/src/test/kotlin/steps/FeatureServerLifecycle.kt) owns the current
`UITestBase`, feature URI, persistence mode and reuse eligibility.

Before each scenario, it either starts a fresh process or calls reset with a 15-second HTTP timeout. Successful
reset clears the client's cached KB/case selection. Reset rejection, timeout or error preserves diagnostics and
replaces the process; a successful ping alone does not make that process reusable. Every replacement gets a new
`UITestBase`, since shutdown closes the old HTTP clients.

New starts wait for process readiness and ping. Managed `temp/` and `downloads/` directories are cleaned only
after the previous process has exited. Their resolved paths must be directly below the repository root.
Directory cleanup retries briefly on Windows, where released files can remain unavailable.

After each scenario, failure diagnostics are captured and clients are stopped even if capture fails. Fake voice
and file-dialog state is cleared. A healthy in-memory server stays up for the next scenario in that feature;
Postgres servers stop. Failed client teardown marks the process unusable.

The Cucumber `@AfterAll` hook and a JVM shutdown fallback share synchronized, idempotent cleanup. Graceful shutdown
has a two-second request timeout, followed when necessary by forced termination and up to ten seconds to verify exit.
A replacement cannot start before the old process is confirmed stopped.

## Isolation and diagnostics

| State | Treatment |
|---|---|
| KBs, cases, rules, attributes, definitions, AI evaluations, change logs and user lists | Deleted with each KB through its persistence provider. |
| Per-user rule sessions | Cancelled before their KB is deleted. |
| Conversations and open-KB contexts | Cleared by `ChatCoordinator.reset()`. |
| Requests and WebSockets | Must drain before reset mutates state. |
| Gemini call count | Monotonic within a process; scenario counts use differences. |
| Logs | Kept for a feature; failures copy the current scenario's tail. |
| Fake dialogs and voice | Reset client-side after each scenario. |

`ScenarioProgress` records duration, Gemini calls and `serverStartSeconds` in
`cucumber/build/timings/<folder>.csv`. A reused process has zero startup time. Folder-task retries run in a fresh
JVM, so a failure that disappears on retry warrants an isolation check as well as investigation of model/UI variance.
Do not infer a startup improvement from total elapsed time alone: model latency and retry counts also vary.

## Regression coverage

- [TestRequestGateTest](../../server/src/test/kotlin/io/rippledown/server/TestRequestGateTest.kt):
  draining, cancellation, timeouts, concurrent resets and stale upgrades.
- [ResetRoutesTest](../../server/src/test/kotlin/io/rippledown/server/ResetRoutesTest.kt):
  opt-in, handler completion and permanently closed admission after failure.
- [ServerApplicationResetTest](../../server/src/test/kotlin/io/rippledown/server/ServerApplicationResetTest.kt):
  clearing KBs and conversations, cancelling sessions and enforcing preconditions.
- [FeatureServerLifecycleTest](../../cucumber/src/test/kotlin/steps/FeatureServerLifecycleTest.kt):
  reuse, replacement, teardown failure, Postgres isolation and cleanup.
- [ServerProxyTest](../../cucumber/src/test/kotlin/io/rippledown/integration/proxy/ServerProxyTest.kt):
  forced shutdown, verified process exit and HTTP-client cleanup.

Commands and desktop execution constraints are in [testing](testing.md).
