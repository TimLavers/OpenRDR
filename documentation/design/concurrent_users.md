# Concurrent users

Several users work on knowledge bases at the same time, including building rules in the same knowledge base at once.
Authentication is a third-party system's job; OpenRDR consumes a trustworthy user identity per request. The unit of
identity is the user, not the window: a user's second window shares their conversation and open KB.

The persistence layer is multi-project by construction (each KB has its own database, routes carry a `kbId`), so the
design is about the server's in-memory state — connections, conversations, rule sessions, the KB object graph — and
about what happens when two users' edits meet.

## Identity

- Every REST call and the web-socket handshake carry the user id in the `X-User-Id` header (`USER_ID_HEADER` in
  `common`). `UserId` is a value class over `String`.
- `RoutingContext.userId()` (`RoutingUtilities.kt`) reads it; the web-socket route reads it from the handshake. A
  missing or blank header is an error (`MISSING_USER_ID`), handled like a missing `kbId`. There is **no fallback
  identity**: behind a misconfigured gateway a default id would silently merge every user into one, with only a log
  line to show for it. The laboratory system never hits a route that reads the id.
- The desktop `Api` takes its `userId` from the `openrdr.userId` system property, else the OS user name (neither
  available is an error), and installs it as a default request header, so every call and the handshake carry it
  without touching call sites. Swapping to a gateway-injected header changes nothing on the server.
- The cucumber `RESTClient`'s raw `HttpClient` sends the same header as its `Api`, so the GUI and the test client
  present one identity.

## Connections and pushes

`WebSocketManager` keeps `ConcurrentHashMap<UserId, MutableSet<WebSocketSession>>` — a set because one user may run
two windows. Two push shapes:

- `sendToUser(userId, message)` for pushes that belong to one user's activity: `KB_INFO` / `KB_CLOSED` when *their*
  chat opens, creates or closes a KB (opening a KB in my chat must not switch your window); cornerstone status
  and `RULE_SESSION_COMPLETED`, addressed to the user whose rule session they concern. A rename is the exception:
  every window filters `casesInfo` by KB *name*, so the renamed `KB_INFO` goes to every user whose conversation is on
  the KB (`OpenKnowledgeBases.usersOn`), or their case lists would silently stop updating. `CHAT_CONTEXT` goes to
  all of one user's windows after each conversation start (see "One conversation, several windows").
- `broadcast(message)` for `casesInfo`: case ingestion is not a user action and the client already filters by KB
  name.

Per-KB addressing ("everyone with this KB open") was considered and rejected: the client starts its conversation only
once its context has settled (after the case list for a newly opened KB has arrived), so for a round-trip or two the
server does not know the user is on the KB, and a push for a case posted in that window would reach nobody.

A dead connection never fails the request that triggered the push: `send` logs and swallows.

## Per-user conversation and open KB

`ChatCoordinator` owns one `Conversation(chatManager, context, oneTurnAtATime: Mutex)` per user. "One turn at a
time" is per user; two users' turns interleave freely. The user's open KB lives in their `ChatContext` — there is no
server-wide open KB. `ServerApplication.openChatEndpoint(userId)` looks it up.

Closing and deleting are different events:

- `knowledgeBaseClosed(userId)` resets only the caller's conversation and pushes `KB_CLOSED` to them.
- `knowledgeBaseDeleted(kbId)` resets every conversation on that KB — another user may be mid-conversation on it, and
  the deleter need not have had it open — and pushes `KB_CLOSED` to each of them. This lives in
  `ApplicationKbService.delete`, and the REST `DELETE_KB` route goes through the caller's service rather than
  `ServerApplication.deleteKB` directly, so both entry points invalidate the same way.

`ApplicationKbService` is constructed per user (`ServerApplication.kbServiceFor(userId)`) with the `userId` and an
`OpenKnowledgeBases` (`openEndpointFor`, `knowledgeBaseClosed`, `knowledgeBaseDeleted`) that `ChatCoordinator`
implements; the interface breaks the construction cycle coordinator → factory → service → coordinator.

### One conversation, several windows

The client starts a conversation whenever its context settles (a KB opened or closed, a case selected), and each start
replaces the user's `ChatManager`. With two windows this used to be a trap: the second window opened the *first* KB
on startup and started a conversation about it, silently retargeting the first window's chat — rules the user then
asked for in window one were built on the wrong KB. So the windows now follow the conversation rather than each
owning one:

- `GET CHAT_CONTEXT` returns the user's `ChatContextInfo(kbInfo, caseId)` (204 if they have no conversation yet). A
  new window adopts it instead of opening the first KB, and does not start a conversation of its own.
- After every `startConversation` the coordinator reports the new context (`ChatCoordinator.contextChanged`) and the
  server pushes `CHAT_CONTEXT` to all of the user's windows. A window whose KB or case differs adopts the pushed
  context — switches KB, reloads the case list, selects the case — but does not restart the conversation
  (`OpenRDRUI.adoptedContext`), or it would reset the chat the other window is in the middle of. A push that matches
  what the window already shows is the echo of its own start and is ignored, as is any push arriving while its own
  start is in flight, since the response to that start settles the context.

The windows do not share chat *history*: a window shows the turns it took part in. That is enough for the
problem at hand; the point is that whatever window the user types in, the chat acts on the KB and case they can see.

Conversations are **never evicted** while the server runs. Evicting when the user's last web socket closes was
rejected: the client has no reconnect loop and the ping timeout is 15 s, so a network blip or a laptop sleep would
delete the conversation and the server-side open KB while the window still shows them. A conversation is a prompt and
a history — negligible at the expected user counts.

## The KB set

Two users can both create, delete, rename or import KBs. `ServerApplication.idToKBEndpoint` is a `ConcurrentHashMap`
and the KB-set mutations (`createKB`, `createKBFromSample`, `importKBFromZip`, `deleteKB`, `renameKB`) run under one
`kbSetLock`. Readers (`kbList`, `kbForName`) do not take it, so `KBManager`'s collections are concurrent ones, which
iterate without `ConcurrentModificationException`. This guards the *set* of KBs; each KB's contents have their own
lock, below.

## The per-KB lock

Ktor runs each request as its own coroutine on a thread pool, so two requests for the same KB can be inside `KB` at
the same instant, and nothing in `io.rippledown.kb` is synchronised. Case ingestion from the laboratory is the one
writer that is always running: `processCase` creates attributes while a user reorders them, interprets the new case
while a commit adds a tree node, and two lab posts with the same new attribute name both see `byName == null`. Reads
are not pure either: `KBEndpoint.case(id)` calls `kb.interpret(case)`, which writes the interpretation into the
stored `RDRCase`. A read/write lock would therefore protect nothing an exclusive lock does not, and every KB access
is ms-scale, so one exclusive lock is the honest choice.

1. **Primitive.** A `ReentrantLock` per `KBSession`, exposed as `fun <T> locked(block: () -> T): T`. The engine is
   non-suspend by design (CPU-bound, in-memory), so a coroutine `Mutex` would fit nothing that calls it, and `Mutex`
   is not reentrant. `locked` takes a plain lambda, so suspending while holding the lock is impossible at compile
   time. Reentrancy is needed: `KBEndpoint.commitRuleSession` → `RuleSessionManager.commitRuleSession` →
   `kb.interpret`.
2. **Applied at the entry of each surface.**
    - REST and ingestion: every public method of `KBEndpoint` runs its body in `session.locked { }`. REST routes, the
      lab's posts and the sample-KB builders all go through `KBEndpoint`, a thin adapter, so wrapping it changes no
      engine code.
    - Chat: the chat's rule actions drive `RuleSessionManager` directly as `RuleService`, not through `KBEndpoint`, so
      `ChatManagerFactory` wraps the user's manager in `LockedRuleService`, which runs every call under the lock.
    - `deleteKB` takes the doomed KB's lock (inside `kbSetLock`) so an in-flight mutation finishes before the KB
      goes.
3. **Never held across I/O.** One engine entry calls the LLM synchronously: `RuleSessionManager.conditionForExpression`
   via `ConditionGenerator.conditionFor`. Holding the KB lock across that call would make a lab post wait up to the
   LLM timeout, so the entry wrappers do *not* lock `conditionForExpression`; `RuleSessionManager` holds the lock
   itself only around what touches the KB (the attribute lookups the translator is handed, and the validation
   afterwards). `KBSession` passes its lock to the managers it creates for this. `buildRule` uses the deterministic
   `ConditionExpressionParser` and is locked like everything else. `KBEndpoint.caseReport` reads the viewable case
   under the lock and generates the report outside it.
4. **Pushes stay inside.** `RuleSessionManager` sends web-socket frames with `runBlocking` from inside commit and
   cornerstone paths. That is I/O under the lock, but to a local, buffered socket, so the stall is short and bounded.
   Extracting pushes as results the caller sends after unlocking would widen the `RuleService` surface for no
   observed benefit.

The lock makes the managers' internal synchronisation unnecessary; they stay as they are. `KBEndpointConcurrencyTest`
drives the lab feed and a user against one `KBEndpoint` from several threads; with the lock removed by hand it fails
on the first run.

## Per-user rule sessions

`RuleSessionManager` holds engine operations on the shared `KB` (which are stateless with respect to the manager) and
one user's session state (`ruleSession`, `currentChange`, cornerstone cursor, the translator's conversation and
parser). So the per-user unit is simply **one `RuleSessionManager` instance per user**: `KBSession` keeps
`ConcurrentHashMap<UserId, RuleSessionManager>` behind `ruleSessionManagerFor(userId)`, each built with the same `kb`,
`webSocketManager` and lock, and with its own user as push addressee. KB-wide operations that happen to live on the
manager (`undoLastRuleSession`, `renameAttribute`, `moveAttributeTo`, …) act on `kb`, so calling them through any
user's instance is correct. A class split into "engine" and "session state" was rejected as a ~1,100-line re-threading
of `kb` for no gain.

- **REST.** `KBEndpoint`'s rule-session methods take a `userId` and call `session.ruleSessionManagerFor(userId)`; the
  routes pass `userId()`. No wire-format change beyond the identity header. Sample builders and tests pass a fixed
  user.
- **Chat.** `ChatManagerFactory.create(userId, context)` builds `LockedRuleService(session,
  session.ruleSessionManagerFor(userId))`.
- **Lifecycle.** Instances are created on first use and never removed while the server runs; one with no session in
  progress is a few fields. `KBSession.usersEditing()` is the set of users whose instance has a session in progress.
  `KBSession.cancelRuleSessionOf(userId)` cancels that user's session, if any, and pushes `RULE_SESSION_COMPLETED`
  to them; `ApplicationKbService.close()` calls it, since a user who closes the KB has walked away from it.

## Commit-time revalidation

Alice starts a rule session on case C to add comment X. Before she commits, Bob commits a rule that changes C's
interpretation. Alice's `RuleBuildingSession` holds the *live* `kb.ruleTree`, so her commit would walk a tree that no
longer gives C the interpretation her session was started against — throwing from deep inside the changer or adding a
rule under the wrong parent. Separately, her conflicting-cornerstone set was computed at session start; Bob's commit
adds a cornerstone and may change which cornerstones her rule disturbs.

**Decision: re-check at commit everything that was checked when the session started or a condition was added;
reject and cancel on failure.** Of the candidate checks — the case's whole interpretation is unchanged, or the change
the session makes still applies — the second is what the diff semantically depends on, and it tolerates unrelated
rules landing in the meantime. The predicate already exists: `RuleTreeChange.isApplicable(tree, case)` is what
`startRuleSession` checks before creating a session. Cornerstones are handled the same way: a second
`RuleBuildingSession` with the same case, action and conditions is constructed against `kb.allCornerstoneCases()` now,
and if its conflicting set contains a case the user was never shown (not in
`RuleBuildingSession.idsOfConflictingCornerstonesAtStart` — by id, because two stored cornerstones can share a name),
the commit is rejected. A cornerstone the user reviewed or exempted, or one that stopped conflicting, is fine.

Two more things another user's commit can invalidate are checked the same way, in `commitCurrentRuleSession`:

- **Dependency cycles.** The action's expression and each condition were acyclic when accepted, but a rule committed
  since can close the loop (Alice assigns `A = B + 1`, Bob assigns `B = A + 1`, both individually fine). The
  dependency graph is rebuilt from the current tree and the session's action and conditions rechecked.
- **Conditions that no longer hold.** A condition on a derived attribute can become false for the session case when
  someone edits that attribute's definition. Rebuilding the session would throw from `addCondition`; instead the
  conditions are tested against the freshly materialised case first and a failure is a stale refusal.

All checks run inside `commitCurrentRuleSession`, under the lock, so nothing changes between check and
`session.commit()`.

**Assign-by-definition sessions stage their definition.** Comment attributes are keyed by their text, so two users
adding the same comment share one attribute and neither overwrites the other. A derived attribute is keyed by name,
and storing its definition when the session *starts* let a second user's session overwrite the formula the first
user was still reviewing — her committed rule would then compute his formula. The definition is therefore held in the
`RuleSessionManager` (`pendingDefinition`), overlaid on `kb.definitionResolver` for the session's own evaluation
(`sessionResolver`), and stored at commit — but only if the stored definition is still what it was when the session
started; otherwise the commit is refused as stale ("The definition of <attribute> changed …").

A failed check cancels the session rather than restarting it against the fresh interpretation: if the comment Alice
wanted to add is now already given, or the one she wanted to remove is gone, there is nothing left to do — she has to
look at the case again. Conflicts need two users building rules for the same comment on overlapping cases within the
same minutes, so the lost work is rare and small.

Surfaces:

- `StaleRuleSessionException(message)` in `common` (`io.rippledown.model`), with four messages: "The interpretation of
  <case> changed while you were building this rule. The rule session has been cancelled; please look at the case
  again." and the same for "The cornerstones changed …", "The definition of <attribute> changed …" and "A rule added
  while you were building this rule means yours would make a derived attribute depend on itself. …".
- REST: `StatusPages` (`Refusals.kt`) maps it to `409 Conflict` with the message as a plain-text body and the header
  `X-Refusal: stale`.
- Chat: `ChatManager.response` answers with the exception's message.
- Push: the session's owner gets `RULE_SESSION_COMPLETED`, so the client drops its session state.

## Deleting a knowledge base someone is editing

Two users' edits otherwise meet only through the lock and revalidation. The one refusal is deleting a KB on which a
user *other than the caller* has a rule session in progress: `ServerApplication.deleteKB(id, userId)` checks
`session.usersEditing() - userId` under the KB's lock and throws `ProjectHeldException(kbName, holder)`, naming one of
them. Deleting pulls a half-built rule's KB out from under its user; nothing else does.

| Situation                                  | Outcome                                                                                                                                                                      |
|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| B deletes the KB while A is mid-rule       | refused: "<KB> is being edited by <A>."                                                                                                                                      |
| B deletes the case A is building a rule on | allowed. A's session holds the case; her commit still adds a sound rule and keeps her case as a cornerstone. Refusing would need a per-case ownership table for a rare event |
| Two users overwrite the KB description     | allowed, last write wins. A one-shot overwrite of prose, with no session behind it                                                                                           |
| B renames the KB while A is editing        | allowed; nothing in a session depends on the KB name                                                                                                                         |
| A closes the KB in the chat                | A's own session is cancelled; nobody else is affected                                                                                                                        |
| A abandons a session                       | nothing to release; a few fields in memory until A cancels or the server restarts                                                                                            |

The refusal reaches the user as: over REST, `409 Conflict`, body "<kbName> is being edited by <holderId>.", header
`X-Refusal: held`; in the chat, `projectHeldChatMessage` — the KB cannot be deleted right now, try again when the
other user has finished their rule.

## Client

`Api`'s response validator turns a 409 into `StaleRuleSessionException` or `KnowledgeBaseHeldException` according to
the `X-Refusal` header. The GUI never commits over REST (commits go through the chat), and no GUI action can be
refused as held, so the GUI has no special handling: a user's own client behaves exactly as with a single user, and
the chat is where refusals are read.

Not built: a "who is editing" indicator. It would be a display, not a control, so the chat-UI guidelines allow it, but
it needs a push on session start and end to avoid going stale, and the delete refusal already names the other user.
Revisit if refusals prove confusing in use.

## Testing

Multi-user acceptance is REST-first (`requirements/kb/Concurrent Users.feature`, `ConcurrentUsersDefs.kt`): several
`Api` instances with different user ids against one server, the chat reached through `sendUserMessage`. Everything
above — identity keying, per-user conversations and pushes, two editors in one KB, the stale-commit refusal, the delete
refusal — is observable that way. Conditions in those scenarios are built in the step definitions rather than
translated, so no LLM is involved.

What a REST client cannot see is what another user's *window* does with the frames it was or was not sent. One
two-window scenario covers that (`requirements/kb/Concurrent Users with windows.feature`): Alice builds a rule with a
cornerstone in her window while Bob, on the same KB, sees no cornerstone and no change to his case until he reselects
it. `StepsInfrastructure` keeps one `LaunchedClient` per named user and a current one that the page objects address;
"I switch to Alice's window" redirects them and brings her window to the front, since Robot clicks land on whichever
window is on top. The in-JVM observation hooks (`ChatTestHook`, `CornerstoneTestHook`) are keyed by `UserId`, each
window publishing under its own `Api.userId`, so a page object reads the state of its own window and not whichever
window recomposed last. Both chats are driven by the real model, so this is the slowest scenario in the suite and
there is deliberately only one.

Server-level: `KBSessionTest` (lock, per-user managers, `usersEditing`, cancellation), `StaleRuleSessionTest` (two
users through one `KBSession`), `KBEndpointConcurrencyTest` (the lab feed against a user), `RefusalsTest` (the two
409s and their header), `ChatCoordinatorTest` and `ApplicationKbServiceTest` (per-user conversations and pushes),
`WebSocketManagerTest`.

## Out of scope

- User management, login, roles, permissions — the third-party system's job. OpenRDR sees an opaque `userId`.
- Broadcast invalidation: a "KB changed" push so other clients re-fetch the current case. Without it a user's
  *displayed* interpretation can be stale until their next fetch; their *commit* cannot be, thanks to revalidation.
- A stateless, multi-instance server with the database as coordination point. It buys horizontal scalability nothing
  demands and forfeits the "one instance per object within a KB" invariant (see [architecture.md](architecture.md))
  that the rule engine relies on.
- Merging two users' concurrent edits to the *same* rule session — a session belongs to one user.
