# Concurrent users — groundwork implementation plan

Stage 1 of [concurrent_users.md](concurrent_users.md): user identity, the connection registry, per-user
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
  unauthenticated. The fallback is logged at WARN: behind a gateway, a missing header would silently merge every
  user into one. `UserId` is a value class over `String` in `common`.
- The web-socket route (`WebSockets.kt`) reads the same header from the handshake request.
- No route *acts* on the id yet — this step only makes it available.
- The identity unit is the *user*, not the window. A user's second window shares their conversation and open KB
  (selecting a case in one window restarts the conversation seen by the other). Accepted for now; if it is ever not
  acceptable, the header contract grows a client-instance id, so decide before step 1 lands.

**Client.**

- `Api` gains a `userId` (constructor parameter, defaulted — e.g. from a system property or the OS user name) and
  installs it as a default request header on the `HttpClient`, so every REST call and the `client.webSocket(...)`
  handshake in `WebSocketApi` carry it without touching individual call sites.

**Cucumber.** The GUI's `Api` and the test `RESTClient`'s `Api` run as the same OS user, so they present the same
identity. Only chat state is per user and the `RESTClient` never chats, so the suite does not depend on this; it is
noted so nobody is surprised. `ServerApplicationTest` uses `openChatEndpoint()`, which gains the user argument in step

4.

**Tests.** Unit tests for the helper (header present, absent, blank); an `Api` test asserting the header is on an
arbitrary request and on the web-socket handshake.

## Step 2 — connection registry

**Server.** `WebSocketManager` currently holds one `connection: WebSocketSession`. Replace it with a registry:

- `ConcurrentHashMap<UserId, MutableSet<WebSocketSession>>` (a set, because one user may run two windows; the set is
  `ConcurrentHashMap.newKeySet()`, since sends iterate it while connections come and go).
  `setSession(userId, session)` registers the session and removes it in the existing `finally`.
- The push API splits into two shapes:
    - `sendToUser(userId, message)` — for pushes that belong to one user's activity.
    - `broadcast(message)` — for pushes every connected client should see.
- In this stage every existing call site (`sendStatus`, `sendCasesInfo`, `sendRuleSessionCompleted`, `sendKbInfo`,
  `sendKbClosed`) becomes a `broadcast`. With one connected client that is byte-for-byte today's behaviour.
  `sendToUser` exists from day one so steps 3–4 can use it.
- Per-KB addressing (`sendToUsersOn(kbId, message)`) is added in step 4, not here: it needs to know each user's open
  KB, which step 4 provides. It does *not* need a client change — `START_CONVERSATION` already carries the `kbId`.
- `send` swallows exceptions with `printStackTrace()`; switch it to the class's (currently unused) `logger`.

**Client.** No change beyond step 1's header. `WebSocketApi`'s dispatch loop is untouched.

**Tests.** `WebSocketManager` unit tests: two users registered, `sendToUser` reaches only the addressee, `broadcast`
reaches both, a closed session is deregistered and a send to it does not throw. (The current class has a swallow-all
`catch` in `send`; keep the behaviour — a dead connection must not fail the request that triggered the push.)

## Step 3 — per-user chat

**Server.** `ChatCoordinator` holds one `chatManager`, one `context` and one `Mutex`. It becomes a registry:

- `private val conversations = ConcurrentHashMap<UserId, UserConversation>` where
  `UserConversation(var chatManager: ChatManager?, var context: ChatContext, val oneTurnAtATime: Mutex)`. The mutex
  moves inside: "one turn at a time" is per user, two users' turns may interleave freely (they hit different
  `Conversation` objects and, until Stage 3, different KBs).
- `startConversation(userId, context)` and `responseToUserMessage(userId, message)`; the routes in
  `ChatManagement.kt` pass `userId()` from step 1.
- `knowledgeBaseClosed()` splits in two, because closing and deleting are different events:
  - `knowledgeBaseClosed(userId)` — the caller closed their KB; only their conversation resets to `NoKnowledgeBase`.
  - `knowledgeBaseDeleted(kbId)` — resets every conversation whose `context.kbInfoOrNull?.id == kbId`, not just the
    caller's: another user may be mid-conversation on the KB that was just deleted, and the deleter need not have had
    it open. Returns the affected user ids so step 4 can push `KB_CLOSED` to each.
- `context()` — the accessor `ServerApplication.openChatEndpoint()` uses — becomes `contextFor(userId)`; see step 4.
- `usersOn(kbId)` — the users whose context is on that KB; step 4's per-KB addressing is built on it.
- **No eviction in this stage.** Eviction means the server discarding a user's entry from `conversations` — the
  `ChatManager` with its model conversation (system prompt plus message history) and the `ChatContext` holding their
  open KB. Without it the map holds one entry per user who has connected since the server started; it is never
  trimmed. Evicting when the user's last web-socket closes was considered and rejected:
  - The client opens its web socket once (`WebSocketApi.startSession` has no reconnect loop) and the server's ping
    timeout is 15 s, so a network blip or a laptop sleep closes it for good.
  - Today such a drop only stops the pushes; chat keeps working because the REST calls are independent of the socket.
  - Tied to eviction, the same drop would also delete the conversation: the next message would be answered with
    `NO_CONVERSATION_MESSAGE` and the server-side open KB would be gone, while the window still shows the KB and
    case. That is a new failure mode for a single user, which this stage promises not to introduce.
  - The memory held is a handful of conversation objects — negligible at the expected user counts.

  Stage 2's lease already has an expiry, and an expired lease is the right trigger for dropping the conversation too.

**Tests.** `ChatCoordinatorTest` grows cases: two users start conversations and each gets responses from their own
`ChatManager` (mockk per user via the factory); a message from a user with no conversation still yields
`NO_CONVERSATION_MESSAGE`; `knowledgeBaseClosed(userId)` resets only that user; `knowledgeBaseDeleted(kbId)` resets
exactly the conversations on that KB and reports them; one user's held mutex does not block the other user's turn.

## Step 4 — per-user open KB

The server-wide "open KB" is derived from the single chat context. With step 3 it is per user:

- `ServerApplication.openChatEndpoint()` → `openChatEndpoint(userId)` delegating to
  `chatCoordinator.contextFor(userId).endpointOrNull`.
- `ApplicationKbService` is constructed once with `openEndpoint: () -> KBEndpoint?` and `onClosed: () -> Unit`. These
  close over "the" user, so the service must become user-scoped. Cleanest shape: keep one `ApplicationKbService` but
  give `KnowledgeBaseService`'s open-KB-dependent operations (`openKnowledgeBase`, `close`, `delete`'s was-open check,
  `rename`, `addDemonstrationCase`, `isRuleSessionActive`) a user binding via a thin facade:
  `fun ApplicationKbService.forUser(userId): KnowledgeBaseService`, handed to the chat actions. Chat actions themselves
  are untouched; they keep calling the same interface.
- `ChatManagerFactory.create(context)` has no user today, so it becomes `create(userId, context)` (or the factory is
  constructed with `(UserId) -> KnowledgeBaseService`). The coordinator's own `kbService`, used only for the greeting
  (`knowledgeBases()`, `demonstrations()`), is user-independent and stays unscoped.
- *Every* `sendKbInfo` / `sendKbClosed` the service makes switches from `broadcast` to `sendToUser(userId, …)` — that
  is `open`, `create`, `createFromSample`, `rename` and `close`, not only the open-KB-dependent ones: opening or
  creating a KB in *my* chat must not switch *your* window. This is the first behavioural use of `sendToUser`, and it
  is invisible to a single user.
- The two callbacks replace `onClosed`: `close()` calls `chatCoordinator.knowledgeBaseClosed(userId)` and pushes
  `KB_CLOSED` to the caller; `delete()` calls `chatCoordinator.knowledgeBaseDeleted(kbId)` and pushes `KB_CLOSED` to
  each user it returns (`sendToUser` per affected user), whether or not the deleter had the KB open.
- **Per-KB addressing.** `WebSocketManager.sendToUsersOn(kbId, message)` resolves the addressees through
  `chatCoordinator.usersOn(kbId)`. `sendStatus`, `sendRuleSessionCompleted` and `sendCasesInfo` switch from
  `broadcast` to it: `RuleSessionManager` has its `kb`, and the case routes have the `kbId`, so no user id is needed
  inside `RuleSessionManager`. Without this, two users on different KBs — the first milestone — would see each other's
  cornerstone status and `RuleSessionCompleted`; the client filters `casesInfo` by `kbName` (`OpenRDRUI.kt`), but
  nothing filters those two. Under Stage 2's lease, per-KB is per-user, so this also covers what the exclusive-lock
  milestone needs from the push channel.
- **The KB set is shared state.** Nothing in this stage lets two users mutate the same KB, but they can both create,
  delete, rename or import KBs. `ServerApplication.idToKBEndpoint` is a plain `mutableMapOf` and `KBManager` is
  likewise unsynchronised. Make `idToKBEndpoint` a `ConcurrentHashMap` and run the KB-set mutations (`createKB`,
  `createKBFromSample`, `importKBFromZip`, `deleteKB`, `renameKB`) under one lock in `ServerApplication`. This is
  small and belongs here, not in Stage 2.

**Tests.** `ApplicationKbServiceTest`: two user facades, user A opens KB-1 and user B opens KB-2;
`openKnowledgeBase()` differs per facade; A's `close()` resets A only and pushes `KB_CLOSED` to A only; A deleting
KB-2 (which A does not have open) resets B's conversation and pushes `KB_CLOSED` to B only; A's `create` pushes
`KB_INFO` to A only. `WebSocketManagerTest`: `sendToUsersOn(kbId)` reaches the users on that KB and no one else.

**Pre-existing, out of scope.** The REST `DELETE_KB` and `RENAME_KB` routes in `KbManagement.kt` bypass
`KnowledgeBaseService`, so a KB deleted or renamed over REST resets no conversation and pushes nothing. The cucumber
suite uses these routes. Not a groundwork regression; note it for Stage 2.

## What this stage deliberately does not touch

- `RuleSessionManager` and `KBSession` — the one-rule-session-per-KB state is unchanged; it is safe because nothing in
  this stage lets two users mutate the same KB (that is Stage 2's lease, then Stage 3). The KB *set* is the
  exception, handled in step 4.
- The `KB` object graph and its managers — no locking added yet (Stage 3's per-KB write lock).
- The wire format of any existing route — only the new header.
- Persistence — no new tables, no migration.

## Sequencing and verification

Steps land in order 1 → 2 → 3 → 4; each is a separate review. After each step:

- `:common:test`, `:server:test` (new unit tests per step as above).
- The full cucumber suite, run by the user — it exercises exactly the single-user path that must not change.
- After step 4, a targeted integration check: two `Api` instances with different user ids against one server, asserting
  independent conversations and open KBs, and that a cornerstone status pushed for one KB reaches only the user on it
  (server-level test with the in-memory persistence provider, no GUI).
