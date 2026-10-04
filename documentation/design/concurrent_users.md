# Concurrent users

Goal: several users work on projects (knowledge bases) at the same time. First milestone: at most one user may modify a
given project at a time, but different users can work on different projects concurrently. Later milestone: several
users modify the same project concurrently. Authentication is handled by a third-party system, not OpenRDR; the server
only needs a trustworthy user identity per request. The unit of identity is the user, not the window: a user's second
window shares their conversation and open KB (see the groundwork plan, step 1).

## Where the single-user assumptions live today

The persistence layer is already multi-project safe (each KB has its own Postgres database, routes carry a `kbId`), so
the work is almost entirely in the server's in-memory session state and the push channel.

| Assumption                           | Where                                                                                                                                                                                                       |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| One web-socket connection per server | `WebSocketManager` holds a single `connection: WebSocketSession`; every push goes to "the" client                                                                                                           |
| One chat conversation per server     | `ChatCoordinator` holds one `chatManager` and one `context`; `responseToUserMessage` has no notion of who is asking                                                                                         |
| One rule session per KB              | `RuleSessionManager.ruleSession` plus its sibling fields (`currentChange`, `selectedCornerstone`, `diffAttribute`, …) are per-KB singletons; `KBSession` creates one `RuleSessionManager` per KB at startup |
| Rule-session routes are unaddressed  | `RuleSessions.kt` routes (`commit`, `cancel`, `exemptCornerstone`, …) act on "the" session of the KB with no session id                                                                                     |
| One "open KB" per server             | `ServerApplication.openChatEndpoint()` / `ApplicationKbService.openKnowledgeBase()` derive a single open KB from the one chat context                                                                       |
| Unsynchronised KB object graph       | `KB`'s managers (attributes, conditions, rules, cases, case view) mutate shared in-memory state with no locking; safe only because one request at a time mutates it                                         |
| No user identity                     | No route reads a user id; nothing is keyed by user                                                                                                                                                          |

## Stage 1 — common groundwork

Implementation plan: [concurrent_users_groundwork.md](concurrent_users_groundwork.md).

1. **User identity.** The third-party system authenticates; OpenRDR consumes an identity. Simplest contract: the server
   sits behind a reverse proxy / gateway that validates the token and forwards a `userId` header (or the server
   validates a JWT signature itself — validation only, no user management). Every REST call and the web-socket
   handshake carry it. A `RoutingContext.userId()` helper reads the header; a missing header is an error. There is
   no fallback identity — every user is authenticated by assumption, and a default id would silently merge users
   behind a misconfigured gateway and defeat the Stage 2 lease. *Done.*
2. **Connection registry.** Replace the single field in `WebSocketManager` with a registry: `userId → connection(s)`.
   Push methods take an addressee: one user (`sendKbInfo` / `sendKbClosed` when *their* chat opens or closes a KB)
   or everyone (`broadcast`). Per-KB addressing of `sendStatus`, `sendCasesInfo` and `sendRuleSessionCompleted` was
   planned here but is deferred to Stage 2: the chat context is the only record of a user's open KB, and the client
   does not start the conversation until its case list has arrived, so a push in that window would reach nobody.
   Stage 2 addresses them by lease holder instead (see the contract below). *Done.*
3. **Per-user chat.** `ChatCoordinator` becomes a registry `userId → (ChatManager, ChatContext)`. The `Mutex` guarding
   "one turn at a time" becomes per-user. The conversation is cheap state (prompt + history), so this is mechanical.
   *Done.*
4. **Per-user "open KB".** The notion of the open KB moves from the server singleton into the per-user chat context,
   which is where it really lives already — `openChatEndpoint()` just needs a user to look it up for. *Done.*

With the groundwork done, the remaining question is how writes to one KB are coordinated. The following stages
answer it in increasing order of ambition; each builds on the one before.

## Stage 2 — exclusive project lock (the first milestone)

One user per project at a time, enforced server-side.

- Each `KBSession` owns a `ProjectLease` holding `(userId, lastActivity)` or nothing. The lease is taken lazily, on the
  user's first
  knowledge-editing action, not on opening the KB: opening is just `START_CONVERSATION` with a `kbId`, there is no
  read-only versus edit open, and a user merely reading a KB must not block an editor. Reads are allowed to anyone.
- Which operations need the lease: knowledge editing — starting and driving a rule session, committing, renaming or
  reordering attributes, editing definitions, renaming or describing the KB. KB metadata is included deliberately:
  it does not touch the rule session, but one rule with no exceptions is simpler to state, test and explain in the
  chat, and a rename under the holder would contradict their prompt and app bar. Case ingestion (`Interpreter.kt`,
  `CaseManagement.kt`) mutates the KB too, but it comes from the laboratory system, not a user, and is never
  lease-guarded. Deleting or renaming a KB that another user holds is refused with the holder's id.
- The lease is a lease, not a lock: it expires on inactivity, so a crashed client cannot strand a project. It does *not*
  expire on web-socket disconnect — the client has no reconnect loop and the ping timeout is 15 s, so a network
  blip would hand the project to someone else mid rule session (the same reasoning that rejected conversation eviction
  on disconnect in the groundwork plan).
- Expiry or release of the lease cancels the KB's rule session. `RuleSessionManager` is still one per KB in this
  stage, so without this the next holder would inherit a half-built rule (`ruleSession`, `currentChange`, cornerstone
  cursor).
- `KBSession` / `RuleSessionManager` are otherwise untouched: the lease guarantees the existing one-session-per-KB
  state is only ever driven by one user, so no internal synchronisation is needed.
- A second user's editing action is refused; the chat, as the primary surface, says who holds the project. An
  app-bar "read-only — locked by X" indicator was considered and dropped: the client learns of the lease only from a
  refused action, and nothing tells it when the lease is released, so the indicator would go stale.

Cheap, correct, and almost all of it survives into the later stages (the lease becomes a finer-grained lock). The
drawback is purely the product constraint it encodes.

### Lease contract

The decisions a test can be written against. Each item is a commitment, not a design sketch; the implementation
plan follows from them.

1. **One holder per KB.** Each `KBSession` owns a `ProjectLease` ("lease" chosen over "lock" to match the
   semantics) holding `(userId, lastActivity)` or nothing. One object per KB rather than a `kbId → lease` map: a
   KB has at most one holder, a user may hold several KBs, and deleting the KB discards its lease for free.
2. **Taken on first guarded action, renewed on every one.** Opening a KB (`START_CONVERSATION`) and every read take
   no lease. The first guarded action by a user on an unheld KB takes it; each later guarded action by the holder
   moves `lastActivity`. If the KB is held by someone else, the action is refused and nothing else happens.
3. **Guarded actions** are exactly the KB mutations a *user* initiates:
    - rule building: start session, add/remove condition, select cornerstone, exempt cornerstone, commit, cancel;
    - attribute edits: rename, reorder, add/edit a derived attribute;
    - comment edits outside a rule session (rename a comment);
    - changes to the set of cases a user makes: deleting a case (REST `DELETE_CASE_WITH_NAME` and the chat's
      `DeleteCaseFromList`), copying a case to a list;
    - KB metadata: rename, describe, delete.

   Not guarded: case ingestion from the laboratory system (`Interpreter.kt`, `CaseManagement.kt` posts) — new cases
   arriving take nothing away from the holder — favourites and views (per-user presentation), exporting, and every
   read. Creating a KB is unguarded (nothing to hold yet).

   Guarding case deletion is what protects an in-progress rule session's case: today nothing stops a case being
   deleted while a rule is being built on it, and the session would carry on against a case that no longer exists
   and copy it back in as a cornerstone at commit. With one user this never happens in practice; with two it is the
   first thing that would.

   Describing the KB is in the set for a different reason. It is a one-shot overwrite with no session: two users
   composing in their chat text areas and pressing Enter a second apart would silently lose the first text. The
   lease is the only thing that turns that into a refusal, so the weakest-looking member of the set is the one
   with no other protection.

   The consequence, accepted deliberately: a one-shot action such as a description edit takes the lease and holds it
   like any other, so a user who only tweaked the description blocks other editors until they close the KB or the
   lease expires. A lighter policy for one-shot actions ("check, don't take") would bring the silent overwrite back,
   and "release when the rule session ends" has the same hole. One policy, no exceptions; the refusal message tells
   the other user that closing the KB hands it over.
4. **One primitive, applied at each surface's entry.** The chat does not go through `KBEndpoint`: its rule actions
   drive `RuleSessionManager` directly as `RuleService`, and its KB-management actions go through
   `ApplicationKbService` to `ServerApplication`. So the single primitive is `KBSession.hold(userId)` (take or renew,
   else throw `ProjectHeldException`), and it is called from three places, each of which is the only way in for its
   surface:
    - REST: each guarded route resolves its endpoint through `heldKbEndpoint()` (`RoutingUtilities.kt`), which reads
      the identity header and calls `hold` before the route body runs; `KBEndpoint` itself is unchanged.
    - Chat rule actions: `ChatManagerFactory` wraps the KB's `RuleSessionManager` in a per-user `LeasedRuleService`
      whose guarded methods call `hold` and delegate; the actions and function-call handlers are untouched.
    - KB metadata: `ServerApplication.renameKB` / `deleteKB` take a `userId` and call `hold`, so the REST
      `KbManagement` routes and `ApplicationKbService` cannot differ; `setDescription` goes through `KBEndpoint`.

   This closes the pre-existing gap where `DELETE_KB` / `RENAME_KB` over REST bypassed `KnowledgeBaseService` —
   they still bypass the *service*, but not the *lease*.
5. **What the loser gets.** Over REST: `409 Conflict`, body `"<kbName> is being edited by <holderId>."`. In the chat:
   the same sentence, followed by what the user can still do (read the cases, open another KB). The chat never
   retries or queues the action. A 409 to a GUI action (today only attribute reordering) is shown as a warning row in
   the chat — warning icon, "The attribute order was not changed: <sentence>" — and the case is re-fetched so the
   table reverts the optimistic reorder; it adds no control (chat-UI guidelines).
6. **Expiry.** A lease with no activity for **10 minutes** is expired; the next guarded action by anyone (including
   the old holder) treats the KB as unheld. There is no background timer: expiry is checked on access, so a server
   with no traffic does nothing. Disconnecting the web-socket does not release the lease (reasoning above).

   The figure is bounded below by the longest natural pause inside a rule session (reading a cornerstone, thinking
   about a condition, a phone call) because expiry cancels the session and loses the half-built rule; it is bounded
   above by how long an abandoned client blocks the project. Ten minutes is a constant, not configuration, and is
   cheap to change once someone is bitten in either direction. `ProjectLease` takes an injected clock
   (`() -> Long`, as `ApplicationKbService` already does), so expiry is unit-tested by advancing a fake clock; the
   cucumber acceptance uses close-to-release and never waits on expiry.
7. **Release.** The holder releases by closing the KB in the chat, by deleting it, or by expiry. There is no explicit
   "release" verb to learn; closing is the natural one. Release of any kind cancels the KB's rule session if one is in
   progress and the holder's client is told via `RULE_SESSION_COMPLETED`-style push so it drops its session state.
8. **Deleting or renaming a held KB** by a non-holder is refused as in 5. By the holder, delete releases the lease and
   resets every user's conversation on that KB (groundwork step 4); rename keeps the lease.
9. **Pushes go to the holder.** `sendStatus` (cornerstone status) and `sendRuleSessionCompleted` are addressed to
   the lease holder of the KB — a rule session exists only under a lease, so the addressee is always defined and
   needs no settled chat context. `sendCasesInfo` stays `broadcast` (ingestion is not a user action; the client
   filters by KB name). This resolves the item deferred from the groundwork.
10. **Identity in the test suite.** The GUI's `Api` sends the OS user name; the cucumber `RESTClient` keeps a raw
    `HttpClient` for some calls that sends no header. Today none of those calls reads the id, so nothing fails;
    once the guard lands on `KBEndpoint`, every guarded route reads it and those calls are refused outright. Before
    the guard lands, the `RESTClient` must present the same identity as the GUI (route everything through its `Api`,
    or give the raw client the same default header). This is a prerequisite task, not a test fix.

### Stage 2 implementation plan

In order; each step keeps the suites green.

1. **Test-suite identity.** The cucumber `RESTClient`'s raw `HttpClient` sends the same `X-User-Id` as its `Api`.
   *Done.*
2. **`ProjectLease`** (`kb.lease`): `hold(userId)` takes, renews or throws; `release()`; `holder()`; expiry
   `LEASE_EXPIRY_MS` checked on access; injected clock. `KBSession` owns one and adds `hold(userId)` /
   `release()`, which also cancel a rule session that loses its lease and push `RULE_SESSION_COMPLETED` to its
   holder. *Done.*
3. **409 over REST.** A `StatusPages` handler maps `ProjectHeldException` to `409 Conflict` with the message;
   installed in `module()` and the server test base. *Done.*
4. **REST guard.** Guarded routes resolve their endpoint through `heldKbEndpoint()`. Guarded: start/commit/
   cancel session, update/select/exempt cornerstone, add condition, build rule, undo, delete case, move attribute,
   set attribute order, set description. `ServerApplication.renameKB` / `deleteKB` take `userId`. *Done.*
5. **Chat guard.** `LeasedRuleService`; `ChatManager.response` turns `ProjectHeldException` into the chat sentence.
   `ApplicationKbService.rename` / `delete` / `setDescription` pass their user. `close()` releases. *Done.*
6. **Pushes to the holder.** `RuleSessionManager` sends cornerstone status and `RULE_SESSION_COMPLETED` to
   `lease.holder()` instead of broadcasting. *Done.*
7. **Acceptance.** The REST-only two-user scenarios in `requirements/kb/Concurrent Users.feature`, one per contract
   point: refused while held, other KBs unaffected, reads unguarded, close releases, chat names the holder on a
   refused delete. *Done.*
8. **Client refusal.** `Api` turns a 409 into `KnowledgeBaseHeldException`; `OpenRDRUI.swapAttributes` posts a
   `WarningMessage` to the chat and refreshes the case. Acceptance: "Attributes cannot be re-ordered while another
   user is editing the knowledge base" in `requirements/attributes/Attribute ordering.feature` (one GUI user plus a
   REST user). *Done.*

The holder's own client shows nothing: single-user behaviour stays byte-for-byte, and the message is for the *other*
user.

### Possible follow-up: a "held by" indicator

The app-bar indicator was dropped on staleness, not on principle (it is a display, not a control, so the chat-UI
guidelines allow it). Now that the per-user chat context records each user's open KB, the server could push
`lease taken` / `lease released` to everyone with that KB open, which removes the staleness on take and release.
Expiry would still be invisible until someone's next guarded action, so the indicator would have to show when the
lease was taken ("held by Alice since 14:02") to be honest. Considered and rejected along the way: letting users
name themselves in the chat (an asserted identity defeats the lease, and arrives after the conversation it would
name), and a server-wide "who is logged on" count (presence is per server, the lease is per KB, so it answers the
wrong question). Not scheduled; revisit if refusal messages prove confusing in use.

## Stage 3 — shared project, single writer at the engine level

Several users in the same project; concurrency resolved by serialising mutations, not by merging them.
Implementation plans: [concurrent_users_write_lock.md](concurrent_users_write_lock.md) (first increment),
[concurrent_users_per_user_sessions.md](concurrent_users_per_user_sessions.md) (second),
[concurrent_users_revalidation.md](concurrent_users_revalidation.md) (third).

- **Per-user rule sessions.** The session state (`ruleSession`, `currentChange`, cornerstone cursor, translator
  conversation) is held per `(userId, kbId)`; the engine operations act on the shared `KB`. Rule-session routes
  resolve the caller's session from the authenticated user — no wire-format change beyond the identity header. *Done* —
  as one `RuleSessionManager` instance per user rather than a class split; see the plan linked above.
- **KB write lock.** Every KB access (reads included: interpreting a case writes into it) runs under one per-KB
  `ReentrantLock` owned by `KBSession`, taken at each surface's entry (`KBEndpoint`, `LeasedRuleService`). Accesses
  are short; users never wait noticeably. This fixes the unsynchronised-object-graph hazard without touching the
  managers. *Done* — see the plan linked above.
- **Commit-time revalidation.** The RDR-specific problem: user A's in-progress session was started against an
  interpretation that user B's committed rule may have changed. At commit, the server re-interprets the session case
  and checks the session's diff still applies; if not, the commit is rejected with "the case's interpretation changed
  while you were building this rule" and the session restarts against the fresh interpretation. Cornerstone sets are
  recomputed at commit under the lock, so a rule never commits against a stale cornerstone review. This is optimistic
  concurrency, and conflicts should be rare (two users building rules for the same comment on overlapping cases).
- **Broadcast invalidation.** When a rule commits, every user subscribed to the KB gets the existing
  `casesInfo` / `rule session completed` style pushes plus a new "KB changed" event; their clients re-fetch the current
  case. Users with an in-progress session get a warning that the KB changed under them.

This is the natural end state for a single-server deployment and the lease from Stage 2 degrades gracefully into the
per-KB write lock.

## Stage 4 — stateless server, database as the coordination point

Multiple server instances; all shared state (locks, rule-session state, chat history) lives in Postgres or a shared
cache, KB object graphs are rebuilt or refreshed from the store, and web-socket fan-out needs a pub/sub layer.

Listed for completeness. It buys horizontal scalability that nothing currently demands, and it forfeits the "one
instance per object within a KB" invariant (see [architecture.md](architecture.md)) that the rule engine relies on.
Not recommended until a deployment actually needs more than one server process.

## Recommended path

1. Stage 1: identity plumbing, connection registry, per-user chat and per-user open-KB (no behaviour change for a
   single user).
2. Stage 2: project lease — delivers the first milestone.
3. Stage 3 incrementally: first the per-KB write lock (safety), then per-user rule sessions and commit-time
   revalidation (lifts the one-user-per-project constraint).

Stage 4 is not planned.

## Testing

Multi-user acceptance tests are REST-first; full UI clients are used only where client behaviour is what is under
test.

- **REST clients cover the server.** Everything the stages change — identity keying, per-user conversations, per-KB
  pushes, the lease, commit-time revalidation — is observable through `Api` plus a web-socket listener. Two `Api`
  instances with different user ids against one in-memory server cover every assertion in the groundwork plan, and the
  chat is reachable the same way (`sendUserMessage` returns the `ChatResponse`), so "the second user is told who holds
  the project" needs no window. Cucumber step defs address users by name (`user "alice" opens KB "X"`); the current
  `RESTClient` wraps one `Api` with one `currentKB`, so it becomes one instance per named user, each with its own
  `userId` and its own `WebSocketApi` listener for push assertions.
- **UI clients cover what REST cannot see.** That user B's window does *not* switch KB when A opens one, does *not*
  show A's cornerstone status, and shows A's lease as a warning in the chat when B's edit is refused — these are
  client reactions to frames that were or were not sent, and a REST client can only observe the frame. One scenario
  per stage of the shape "two users, two windows, A edits, B is refused and is unaffected by A's pushes" is enough.
- **Multi-UI scenarios are kept rare.** A UI run takes over the desktop; two Compose windows double the a11y-tree
  flakiness, both chat panels drive the LLM, and the page objects (`ChatPO`, `InterpretationPO`, …) are singletons that
  need a window parameter. Tag them `@multi-user`, put them in their own feature folder so routine folder runs exclude
  them, and schedule them like the other long UI tests. The second window (a second `TestClientLauncher` plus
  window-scoped page objects) is deferred to Stage 2, when there is first UI behaviour to test; the groundwork needs
  none.

## Out of scope

- User management, login, roles, permissions — the third-party system's job. OpenRDR sees an opaque `userId`.
- Cross-server deployment (Stage 4).
- Merging two users' concurrent edits to the *same* rule session — a session belongs to one user.
