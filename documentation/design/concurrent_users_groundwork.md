# Concurrent users — groundwork implementation plan

The groundwork stage of [concurrent_users.md](concurrent_users.md): user identity, the connection registry, per-user
chat and the per-user open KB. Each step is independently shippable and must be a no-behaviour-change for today's
single user — the existing unit and cucumber suites keep passing after every step. No schema changes anywhere in this
stage.

## Step 1 — user identity

**Contract.** Every REST call and the web-socket handshake carry a user id. The third-party system is responsible for
authenticating; OpenRDR trusts an identity header (working name `X-User-Id`). Until a gateway exists, the desktop
client sends its own id; swapping to a gateway-injected header later changes nothing on the server.

**Server.**

- A `USER_ID_HEADER` constant in `common` (`constants.server.Constants`), beside `KB_ID` / `CASE_ID`.
- A helper in `RoutingUtilities.kt`: `fun RoutingContext.userId(): UserId`, reading the header. A missing header
  resolves to a fixed default id (`"local"`), so the packaged demo, curl and the cucumber suite keep working
  unauthenticated. `UserId` is a value class over `String` in `common`.
- The web-socket route (`WebSockets.kt`) reads the same header from the handshake request.
- No route *acts* on the id yet — this step only makes it available.

**Client.**

- `Api` gains a `userId` (constructor parameter, defaulted — e.g. from a system property or the OS user name) and
  installs it as a default request header on the `HttpClient`, so every REST call and the `client.webSocket(...)`
  handshake in `WebSocketApi` carry it without touching individual call sites.

**Tests.** Unit tests for the helper (header present, absent, blank); an `Api` test asserting the header is on an
arbitrary request and on the web-socket handshake.

## Step 2 — connection registry

**Server.** `WebSocketManager` currently holds one `connection: WebSocketSession`. Replace it with a registry:

- `ConcurrentHashMap<UserId, MutableSet<WebSocketSession>>` (a set, because one user may run two windows).
  `setSession(userId, session)` registers the session and removes it in the existing `finally`.
- The push API splits into two shapes:
    - `sendToUser(userId, message)` — for pushes that belong to one user's activity.
    - `broadcast(message)` — for pushes every connected client should see.
- In this stage every existing call site (`sendStatus`, `sendCasesInfo`, `sendRuleSessionCompleted`, `sendKbInfo`,
  `sendKbClosed`) becomes a `broadcast`. With one connected client that is byte-for-byte today's behaviour. Addressing
  the cornerstone pushes to the session owner requires the user id inside `RuleSessionManager`, which belongs to the
  per-user rule-session split (Option B) — explicitly out of scope here. `sendToUser` exists from day one so steps 3–4
  can use it.
- Per-KB subscription (`broadcastToKb(kbId, message)`) is also deferred: it needs the client to tell the server which
  KB it is viewing, which arrives naturally with the project lease (Option A).

**Client.** No change beyond step 1's header. `WebSocketApi`'s dispatch loop is untouched.

**Tests.** `WebSocketManager` unit tests: two users registered, `sendToUser` reaches only the addressee, `broadcast`
reaches both, a closed session is deregistered and a send to it does not throw. (The current class has a swallow-all
`catch` in `send`; keep the behaviour — a dead connection must not fail the request that triggered the push.)

## Step 3 — per-user chat

**Server.** `ChatCoordinator` holds one `chatManager`, one `context` and one `Mutex`. It becomes a registry:

- `private val conversations = ConcurrentHashMap<UserId, UserConversation>` where
  `UserConversation(var chatManager: ChatManager?, var context: ChatContext, val oneTurnAtATime: Mutex)`. The mutex
  moves inside: "one turn at a time" is per user, two users' turns may interleave freely (they hit different
  `Conversation` objects and, until Option B, different KBs).
- `startConversation(userId, context)` and `responseToUserMessage(userId, message)`; the routes in
  `ChatManagement.kt` pass `userId()` from step 1.
- `knowledgeBaseClosed()` becomes `knowledgeBaseClosed(kbId)`: it resets every conversation whose
  `context.kbInfoOrNull?.id == kbId` to `NoKnowledgeBase`, not just the caller's — another user may be mid-conversation
  on the KB that was just deleted. (`ChatContext.endpointOrNull` already exposes what is needed.)
- `context()` — the accessor `ServerApplication.openChatEndpoint()` uses — becomes `contextFor(userId)`; see step 4.
- Eviction: a conversation is dropped when its user's last web-socket connection closes (hook the deregistration in
  step 2's registry), bounding memory without an idle timer.

**Tests.** `ChatCoordinatorTest` grows cases: two users start conversations and each gets responses from their own
`ChatManager` (mockk per user via the factory); a message from a user with no conversation still yields
`NO_CONVERSATION_MESSAGE`; `knowledgeBaseClosed(kbId)` resets exactly the conversations on that KB; one user's held
mutex does not block the other user's turn.

## Step 4 — per-user open KB

The server-wide "open KB" is derived from the single chat context. With step 3 it is per user:

- `ServerApplication.openChatEndpoint()` → `openChatEndpoint(userId)` delegating to
  `chatCoordinator.contextFor(userId).endpointOrNull`.
- `ApplicationKbService` is constructed once with `openEndpoint: () -> KBEndpoint?` and `onClosed: () -> Unit`. These
  close over "the" user, so the service must become user-scoped. Cleanest shape: keep one `ApplicationKbService` but
  give `KnowledgeBaseService`'s open-KB-dependent operations (`openKnowledgeBase`, `close`, `delete`'s was-open check,
  `rename`, `addDemonstrationCase`, `isRuleSessionActive`) a user binding via a thin facade:
  `fun ApplicationKbService.forUser(userId): KnowledgeBaseService`, created by `ChatManagerFactory.create(context)` —
  which is already per conversation — and handed to the chat actions. Chat actions themselves are untouched; they keep
  calling the same interface.
- Pushes these operations make (`sendKbInfo`, `sendKbClosed`) switch from `broadcast` to `sendToUser(userId, …)`:
  opening a KB in *my* chat must not switch *your* window. This is the first behavioural use of `sendToUser`, and it is
  invisible to a single user.
- `onClosed` wiring in `ServerApplication` becomes `{ kbId -> chatCoordinator.knowledgeBaseClosed(kbId) }`, with the
  nuance from step 3 that deleting a KB resets every user's conversation on it — their clients receive `KB_CLOSED`
  (that one stays a broadcast-to-affected-users, i.e. `sendToUser` per affected conversation).

**Tests.** `ApplicationKbServiceTest`: two user facades, user A opens KB-1 and user B opens KB-2;
`openKnowledgeBase()` differs per facade; A's `close()` does not disturb B; deleting the KB B has open resets B's
conversation and pushes `KB_CLOSED` to B only.

## What this stage deliberately does not touch

- `RuleSessionManager` and `KBSession` — the one-rule-session-per-KB state is unchanged; it is safe because nothing in
  this stage lets two users mutate the same KB (that is Option A's lease, then Option B).
- The `KB` object graph and its managers — no locking added yet (Option B's per-KB write lock).
- The wire format of any existing route — only the new header.
- Persistence — no new tables, no migration.

## Sequencing and verification

Steps land in order 1 → 2 → 3 → 4; each is a separate review. After each step:

- `:common:test`, `:server:test` (new unit tests per step as above).
- The full cucumber suite, run by the user — it exercises exactly the single-user path that must not change.
- After step 4, a targeted integration check: two `Api` instances with different user ids against one server, asserting
  independent conversations and open KBs (server-level test with the in-memory persistence provider, no GUI).
