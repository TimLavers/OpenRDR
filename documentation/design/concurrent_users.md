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
   handshake carry it. A `RoutingContext.userId()` helper reads the header; a missing header falls back to a fixed
   local id (logged), so the packaged demo, curl and the cucumber suite keep working unauthenticated.
2. **Connection registry.** Replace the single field in `WebSocketManager` with a registry: `userId → connection(s)`.
   Push methods take an addressee — either one user (`sendKbInfo` when *their* chat opens a KB) or all users whose
   open KB is a given one (`sendStatus`, `sendCasesInfo`). The server learns each user's open KB from the chat
   context, so per-KB addressing needs no client change and lands in the groundwork, not with the lease.
3. **Per-user chat.** `ChatCoordinator` becomes a registry `userId → (ChatManager, ChatContext)`. The `Mutex` guarding
   "one turn at a time" becomes per-user. The conversation is cheap state (prompt + history), so this is mechanical.
4. **Per-user "open KB".** The notion of the open KB moves from the server singleton into the per-user chat context,
   which is where it really lives already — `openChatEndpoint()` just needs a user to look it up for.

With the groundwork done, the remaining question is how writes to one KB are coordinated. The following stages
answer it in increasing order of ambition; each builds on the one before.

## Stage 2 — exclusive project lock (the first milestone)

One user per project at a time, enforced server-side.

- A `ProjectLockManager` maps `kbId → lease(userId, expiry)`. The lease is taken lazily, on the user's first
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
- A second user's editing action is refused; the chat, as the primary surface, says who holds the project. The client
  additionally shows "read-only — locked by X" in the app bar, which is a status indicator, not a control, so it is
  within the chat-UI guidelines.

Cheap, correct, and almost all of it survives into the later stages (the lease becomes a finer-grained lock). The
drawback is purely the product constraint it encodes.

## Stage 3 — shared project, single writer at the engine level

Several users in the same project; concurrency resolved by serialising mutations, not by merging them.

- **Per-user rule sessions.** `RuleSessionManager` splits in two: the stateless engine operations stay per-KB; the
  session state (`ruleSession`, `currentChange`, cornerstone cursor, translator conversation) moves into a
  `RuleBuildingSessionState` held per `(userId, kbId)`. Rule-session routes resolve the caller's session from the
  authenticated user — no wire-format change beyond the identity header.
- **KB write lock.** All KB mutations (commit rule, rename comment, add case, reorder attributes, …) run under one
  per-KB lock (a `Mutex` or single-threaded dispatcher per KB). Mutations are short; users never wait noticeably. This
  also fixes the unsynchronised-object-graph hazard without touching the managers.
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
  show A's cornerstone status, and shows the read-only indicator with editing disabled — these are client reactions to
  frames that were or were not sent, and a REST client can only observe the frame. One scenario per stage of the shape
  "two users, two windows, A edits, B sees read-only and is unaffected by A's pushes" is enough.
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
