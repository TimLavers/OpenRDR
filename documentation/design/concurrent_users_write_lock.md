# Concurrent users — the per-KB write lock

First increment of Stage 3 in [concurrent_users.md](concurrent_users.md). It changes no behaviour that any user or
test can observe; it makes a KB safe to touch from two threads at once.

## The hazard

Ktor runs each request as its own coroutine on a thread pool, so two requests for the same KB can be inside `KB` at
the same instant. Nothing in `io.rippledown.kb` is synchronised. The Stage 2 lease serialises *user* edits against
each other, but not against case ingestion from the laboratory system, which is deliberately unguarded and is the one
writer that is always running. Races that exist today with a single user:

- `KB.processCase` creates attributes (`attributeManager.getOrCreate`) while the holder's `moveAttribute` rewrites the
  case-view order.
- `KB.processCase` interprets the new case (`ruleTree.apply`) while `commitCurrentRuleSession` adds a node to the tree.
- Anything that iterates `caseManager` (session start, cornerstone computation, `allProcessedCases`) while
  `caseManager.add` inserts.
- Two lab posts carrying the same new attribute name both see `byName(name) == null` and both create it.

Reads are not pure either: `KBEndpoint.case(id)` calls `kb.interpret(case)`, which writes the interpretation into the
stored `RDRCase` instance, so two concurrent reads of one case race with each other. A read/write lock would therefore
protect nothing that an exclusive lock does not, and every KB access is ms-scale, so one exclusive lock is the honest
choice.

## Decisions

1. **Primitive: a `ReentrantLock` per `KBSession`**, exposed as `fun <T> locked(block: () -> T): T`. The engine (`KB`,
   `RuleSessionManager`, `LeasedRuleService`) is non-suspend by design — it is CPU-bound, in-memory work — so a
   coroutine `Mutex` would fit nothing that calls it, and `Mutex` is not reentrant. `locked` takes a plain lambda, so
   suspending while holding the lock is impossible at compile time.
2. **Reentrant by necessity.** `KBEndpoint.commitRuleSession` → `RuleSessionManager.commitRuleSession` →
   `kb.interpret`; `KBSession.hold` → `leaseLostBy` → `cancelRuleSession`. Nested `locked` calls on one thread must
   simply proceed, which `ReentrantLock` gives for free.
3. **Applied at the entry of each surface**, mirroring the lease:
    - REST and ingestion: every public method of `KBEndpoint` runs its body in `session.locked { }`. REST routes, the
      lab's `processCase` / `addCornerstoneCase` posts and the sample-KB builders all go through `KBEndpoint`, so one
      class covers them. `KBEndpoint` is the right level because it is a thin adapter: wrapping its methods changes
      no engine code.
    - Chat: `LeasedRuleService` wraps every `RuleService` call — guarded or not — in `session.locked { }`, since the
      chat's reads (`viewableCase`, `cornerstoneStatus`, …) reach the engine directly rather than through `KBEndpoint`.
      Guarded calls take the lease inside the lock, so "check lease, then mutate" is atomic.
    - KB set operations (`ServerApplication.createKB` / `deleteKB` / `renameKB` / `importKBFromZip`) keep the existing
      `kbSetLock`; it guards the *set* of KBs, which is a different resource. `deleteKB` additionally takes the
      doomed KB's `locked` so an in-flight mutation finishes before the KB is removed.
4. **The lock is never held across I/O.** One engine entry point calls the LLM synchronously:
   `RuleSessionManager.conditionForExpression`, via `ConditionGenerator.conditionFor(userText)`
   (`runBlocking { transform() }`). (`buildRule` looks similar but uses the deterministic
   `ConditionExpressionParser`, so it is locked like everything else.) Holding the KB lock across a Gemini call would
   make a lab post wait up to the LLM timeout, so the entry wrappers (`KBEndpoint`, `LeasedRuleService`) do *not*
   lock `conditionForExpression`; `RuleSessionManager` holds the lock itself only around what touches the KB — the
   `attributeFor` lookups the translator is handed, and the validation afterwards. The `ConditionParser` interface is
   unchanged, so the tests' mock parsers keep working. `KBSession` passes its lock to the `RuleSessionManager` it
   creates. `KBEndpoint.caseReport` likewise reads the viewable case under the lock and generates the report
   outside it.
5. **Pushes stay inside for now.** `RuleSessionManager` sends web-socket frames with `runBlocking` from inside
   commit/cornerstone paths. That is I/O under the lock, but to a local, buffered socket, so the stall is short and
   bounded. Extracting pushes as results the caller sends after unlocking is a wider refactor of the `RuleService`
   surface; it is noted as a follow-up for when Stage 3's per-user sessions touch that code anyway.
6. **Lease and lock are separate concerns.** The lease answers "may this user edit?"; the lock answers "is the KB
   being touched right now?". Stage 3's later increments lift the lease's one-editor rule; the lock stays as the
   thing that makes that safe.

## Steps

In order; each keeps the suites green.

1. **`KBSession.locked`.** `ReentrantLock`; unit tests: two threads cannot both be inside at once (latch-based,
   deterministic); a nested call on the same thread proceeds; the lock is released when the block throws. *Done.*
2. **`KBEndpoint` under the lock.** Every public method's body in `session.locked { }`; `caseReport` locks only the
   read. The existing mocked-session tests stub `locked` and verify it was entered. *Done.*
3. **Chat under the lock.** `LeasedRuleService` locks every call; `held` takes the lease inside the lock. *Done.*
4. **Translate outside, apply inside** (decision 4). Asserted by a `KBSessionTest` whose parser checks, while it
   runs, that another thread can enter the lock. *Done.*
5. **Stress test** (`KBEndpointConcurrencyTest`): four threads post cases with fresh attribute names through
   `processCase` while another thread reorders attributes and builds rules through the same `KBEndpoint`; assert no
   exception, the external attributes are exactly the names the cases carried, every case stored. With the lock
   disabled by hand it failed on the first run (exceptions from the ingesting threads), so it does detect the race.
   *Done.*
6. **`deleteKB` takes the KB's lock** before removing it from the map. *Done.*

## Out of scope

- Per-user rule sessions and commit-time revalidation — the next increments.
- Moving pushes out of the engine (decision 5).
- The KB object graph's internal synchronisation. The lock makes it unnecessary; the managers stay as they are.
