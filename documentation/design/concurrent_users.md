# Concurrent users

Goal: several users work on projects (knowledge bases) at the same time. First milestone: at most one user may modify a
given project at a time, but different users can work on different projects concurrently. Later milestone: several
users modify the same project concurrently. Authentication is handled by a third-party system, not OpenRDR; the server
only needs a trustworthy user identity per request.

## Where the single-user assumptions live today

The persistence layer is already multi-project safe (each KB has its own Postgres schema, routes carry a `kbId`), so
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

## Common groundwork (needed for every option)

Implementation plan: [concurrent_users_groundwork.md](concurrent_users_groundwork.md).

1. **User identity.** The third-party system authenticates; OpenRDR consumes an identity. Simplest contract: the server
   sits behind a reverse proxy / gateway that validates the token and forwards a `userId` header (or the server
   validates a JWT signature itself — validation only, no user management). Every REST call and the web-socket
   handshake carry it. A Ktor plugin extracts it into a `UserPrincipal`; routes stop being anonymous.
2. **Connection registry.** Replace the single field in `WebSocketManager` with a registry: `userId → connection(s)`.
   Push methods take an addressee — either one user (`sendStatus` for *their* rule session) or all users subscribed to
   a KB (`sendCasesInfo` when the case list changes).
3. **Per-user chat.** `ChatCoordinator` becomes a registry `userId → (ChatManager, ChatContext)`. The `Mutex` guarding
   "one turn at a time" becomes per-user. The conversation is cheap state (prompt + history), so this is mechanical.
4. **Per-user "open KB".** The notion of the open KB moves from the server singleton into the per-user chat context,
   which is where it really lives already — `openChatEndpoint()` just needs a user to look it up for.

With the groundwork done, the remaining question is how writes to one KB are coordinated. Three options, in increasing
order of ambition.

## Option A — exclusive project lock (the first milestone)

One user per project at a time, enforced server-side.

- A `ProjectLockManager` maps `kbId → lease(userId, expiry)`. A user acquires the lease when they open the KB for
  editing; every mutating route checks the lease; reads are allowed to anyone.
- The lease is a lease, not a lock: it expires on inactivity or web-socket disconnect, so a crashed client cannot
  strand a project. A second user opening the project is told who holds it and gets read-only access.
- `KBSession` / `RuleSessionManager` are untouched: the lease guarantees the existing one-session-per-KB state is only
  ever driven by one user, so no internal synchronisation is needed.
- Client change: surface "read-only — locked by X" in the app bar, disable mutating UI.

Cheap, correct, and almost all of it survives into the later options (the lease becomes a finer-grained lock). The
drawback is purely the product constraint it encodes.

## Option B — shared project, single writer at the engine level

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

This is the natural end state for a single-server deployment and the lease from Option A degrades gracefully into the
per-KB write lock.

## Option C — stateless server, database as the coordination point

Multiple server instances; all shared state (locks, rule-session state, chat history) lives in Postgres or a shared
cache, KB object graphs are rebuilt or refreshed from the store, and web-socket fan-out needs a pub/sub layer.

Listed for completeness. It buys horizontal scalability that nothing currently demands, and it forfeits the "one
instance per object within a KB" invariant (see [architecture.md](architecture.md)) that the rule engine relies on.
Not recommended until a deployment actually needs more than one server process.

## Recommended path

1. Groundwork: identity plumbing, connection registry, per-user chat and per-user open-KB (no behaviour change for a
   single user).
2. Option A: project lease — delivers the first milestone.
3. Option B incrementally: first the per-KB write lock (safety), then per-user rule sessions and commit-time
   revalidation (lifts the one-user-per-project constraint).

## Out of scope

- User management, login, roles, permissions — the third-party system's job. OpenRDR sees an opaque `userId`.
- Cross-server deployment (Option C).
- Merging two users' concurrent edits to the *same* rule session — a session belongs to one user.
