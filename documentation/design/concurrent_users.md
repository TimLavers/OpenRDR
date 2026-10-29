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
- `broadcast(message)` for `casesInfo`, `PRESENCE`, and `KB_CHANGED`: clients filter the case list by KB name and
  awareness messages by KB id. Case ingestion is not a user action.

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
rejected: the ping timeout is 15 s, so a network blip or a laptop sleep would
delete the conversation and the server-side open KB while the window still shows them. A conversation is a prompt and
a history — negligible at the expected user counts.

## The KB set

Two users can both create, delete, rename or import KBs. `ServerApplication.idToKBEndpoint` is a `ConcurrentHashMap`
and the KB-set mutations (`createKB`, `createKBFromSample`, `importKBFromZip`, `deleteKB`, `renameKB`) run under one
`kbSetLock`. Readers (`kbList`, `kbForName`) do not take it, so `KBManager`'s collections are concurrent ones, which
iterate without `ConcurrentModificationException`. This guards the *set* of KBs; each KB's contents have their own
lock, below.

## The per-KB lock

Ktor runs each request as its own coroutine on a thread pool, so without the per-KB lock two requests could access
the KB's shared object graph at the same instant. Case ingestion from the laboratory is the one
writer that is always running: `processCase` creates attributes while a user reorders them, interprets the new case
while a commit adds a tree node, and two lab posts with the same new attribute name both see `byName == null`. Reads
are not pure either: engine interpretation can mutate an `RDRCase`. AI-aware endpoint reads use isolated interpretations,
but still inspect shared rules and definitions under the lock. A read/write lock would therefore protect nothing an exclusive lock does not, and local inference work is normally short, so one exclusive lock is the honest choice.

1. **Primitive.** A `ReentrantLock` per `KBSession`, exposed as `fun <T> locked(block: () -> T): T`. The engine is
   non-suspend by design (CPU-bound, in-memory), so a coroutine `Mutex` would fit nothing that calls it, and `Mutex`
   is not reentrant. `locked` takes a plain lambda, so suspending while holding the lock is impossible at compile
   time. Reentrancy is needed: `KBEndpoint.commitRuleSession` → `RuleSessionManager.commitRuleSession` →
   `kb.interpret`.
2. **Applied at the entry of each surface.**
    - REST and ingestion: `KBEndpoint` protects shared KB access with `session.locked { }`. AI-dependent operations
      prepare under the lock and suspend outside it for evaluation-record/provider I/O before retrying.
    - Chat: the chat's rule actions drive `RuleSessionManager` directly as `RuleService`, not through `KBEndpoint`, so
      `ChatManagerFactory` wraps the user's manager in `LockedRuleService`, which protects shared state and prepares demanded AI evaluations outside the lock.
    - `deleteKB` takes the doomed KB's lock (inside `kbSetLock`) so an in-flight mutation finishes before the KB
      goes.
3. **LLM calls run outside the lock.** Condition translation calls the LLM through
   `RuleSessionManager.conditionForExpression`
   via `ConditionGenerator.conditionFor`. Holding the KB lock across that call would make a lab post wait up to the
   LLM timeout, so the entry wrappers do *not* lock `conditionForExpression`; `RuleSessionManager` holds the lock
   itself only around what touches the KB (the attribute lookups the translator is handed, and the validation
   afterwards). `KBSession` passes its lock to the managers it creates for this. `buildRule` parses reasons with the deterministic
   `ConditionExpressionParser`; AI-dependent interpretation still uses preparation outside the lock. `KBEndpoint.caseReport` reads the viewable case
   under the lock and generates the report outside it.
4. **Pushes are enqueued inside, sent outside.** `RuleSessionManager` originally sent web-socket frames with
   `runBlocking` from inside commit and cornerstone paths, on the argument that a local, buffered socket makes the
   stall short. That argument does not hold: Ktor's `send` suspends when a connection's outgoing queue is full, and a
   broadcast visits every connection, so one slow client on any KB could stall edits on this one. `WebSocketManager`
   therefore has a dispatcher: `broadcast` / `sendToUser` enqueue without suspending, a single consumer sends in order,
   and a socket whose send fails or exceeds the timeout is removed and its connection coroutine cancelled.
   Later broadcasts do not wait for that socket again; its other windows remain connected. Connection cleanup and
   the presence notification run outside the dispatcher, and the client reconnects to recover its state. All pushes go
   through it; the engine never performs socket I/O under the lock. Synchronous persistence writes still run under
   the lock; the mutation and its change record are separate transactions, as described below.

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
  session.ruleSessionManagerFor(userId), endpoint.coordinator)`.
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
the revalidated candidate session's commit.

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

- `StaleRuleSessionException(message)` in `common` (`io.rippledown.model`), with explanations including "The interpretation of
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

| Situation                                  | Outcome                                                                                                                                                                            |
|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| B deletes the KB while A is mid-rule       | refused: "<KB> is being edited by <A>."                                                                                                                                            |
| B deletes the case A is building a rule on | deletion is allowed, but A's commit is refused if the stored source case was removed or changed during review       |
| Two users overwrite the KB description     | allowed, last write wins. A one-shot overwrite of prose, with no session behind it                                                                                                 |
| B renames the KB while A is editing        | allowed; nothing in a session depends on the KB name                                                                                                                               |
| A closes the KB in the chat                | A's own session is cancelled; nobody else is affected                                                                                                                              |
| A abandons a session                       | nothing to release; a few fields in memory until A cancels or the server restarts. A is no longer *present* (below) once their socket drops, but the session still blocks deletion |

The refusal reaches the user as: over REST, `409 Conflict`, body "<kbName> is being edited by <holderId>.", header
`X-Refusal: held`; in the chat, `projectHeldChatMessage` — the KB cannot be deleted right now, try again when the
other user has finished their rule.

## Client

`Api`'s response validator turns a 409 into `StaleRuleSessionException` or `KnowledgeBaseHeldException` according to
the `X-Refusal` header. The GUI never commits over REST (commits go through the chat), and no GUI action can be
refused as held, so the GUI has no special handling: a user's own client behaves exactly as with a single user, and
the chat is where refusals are read.

## Awareness of other users

The lock and the revalidation keep two users' edits *sound*; they do nothing to keep them *comprehensible*. In use,
user A saw their case's interpretation change under them with no indication of what had happened or who had done it.
Presence, attributed change notices and history make those edits visible. They supplement the locking and
revalidation rules; see [chat UI guidelines](chat_ui_guidelines.md).

### Presence: who is here

A user is *present* on a KB when they are on it (`OpenKnowledgeBases.usersOn`) **and** connected
(`WebSocketManager.connectedUsers()`). The intersection matters: conversations are never evicted, so `usersOn` alone
would list a user who closed their laptop days ago forever, while the socket's 15 s ping timeout makes "connected"
close to live. "Logged on anywhere" is not shown on the main screen: it names people who cannot affect what the user
is looking at. Presence and `usersEditing()` are different questions: a user who has dropped off presence can still
hold a rule session, and that session still blocks deletion of the KB.

The set changes when a socket opens or closes and when a conversation's context changes, and the owner of each state
reports the transition. `WebSocketManager` reports connect and disconnect. `ChatCoordinator` owns the context, so it
reports every transition `(fromKbId, toKbId)` itself — from `startConversation`, `knowledgeBaseClosed` and
`knowledgeBaseDeleted` alike; the latter two reset the context immediately, long before the client's next
conversation start, so a hook on start alone would leave a departed user listed. A transition pushes for the KB left
and the KB joined. Rule-session start, commit, and cancellation also push presence to update the editing flag.
Presence captures membership and editing state and enqueues its push under the same KB lock. This prevents a
calculation waiting for that lock from publishing older membership after a newer update. The lock is reentrant
because rule-session changes already hold it; no additional lock order is introduced.

The push is `PRESENCE` + `{kbId, users}`, a broadcast filtered by the client on `kbId`, for the same reason
`casesInfo` is (a client's conversation lags the KB its window shows). A window also fetches a snapshot (`GET PRESENCE`)
once it has adopted its KB: a new window can receive the connection-triggered push before its
`kbInfo` is set and discard it, and a window that adopts an existing conversation starts none of its own, so no push
is guaranteed to follow. The client shows a people icon and a count of other users at the right of the application
bar, and nothing when alone. Hover, click, or keyboard focus reveals **Others with <KB name> open:** followed by
a bulleted list of names. The count and list update as users join or leave; changing or closing the KB dismisses
the old popup. The current user's name stays in the window title.

### The change log: who did what, when

The supported edit operations append, under the KB lock, to a persisted per-KB `ChangeRecord(id, userId,
timestamp, kind, summary, ruleIds, attributeIds)`: a rule session committed or undone, a derived definition edited, an
attribute or condition renamed, an attribute moved, a case deleted, the description set. These operations record successful changes; refusals and deleting a nonexistent processed case record nothing.
Case ingestion, list copying and KB-set operations are not recorded by this log.

The `summary` is one line written for history, by a formatter of its own — *Added comment "Go to the beach." when Sun
is hot* — because `Rule.actionSummary` is laid out over several lines for the panel and omits the conditions, which
are the part a colleague needs. `attributeIds` are the attributes the change concerns (the attribute a rule assigns,
the attribute renamed or redefined), so that "who changed BMI?" is a filter by id and survives a rename.

Assignments made by definition show the stored expression, for example *Assigned x2 = 2 * x when x ≤ 2.0.*
The summary captures the definition at the time of the change; later definition edits leave earlier history intact.
Undo previews also resolve definitions, showing the expression currently used by the rule being reviewed.

`RuleSessionRecord` stays as the undo mechanism; the log is append-only and is never consulted by the engine. It is
exported and imported with the KB like the rule session records; an export without the directory imports as an empty
log.

`GET CHANGES` accepts `sinceId` (exclusive) and `limit` (default 50). It returns records in ascending ID order,
keeping the newest `limit` records after that cursor. The chat action presents recent changes newest first.

**The guarantee is weaker than transactional.** The lock gives mutual exclusion, not atomicity: the stores run their
own transactions and a rule commit already spans several writes. The record is appended after the mutation inside the
same locked block. If the append fails the mutation stands, the failure is logged at error level, and the push below
still carries the (unpersisted) record — the user is not shown an error that invites them to repeat an edit that
succeeded. One transaction for mutation and record would need a unit of work the persistence layer does not have;
noted under "Out of scope".

The log is the foundation for the rest: without it, a change arriving at another user's window has no name and no
author, and "who did that?" cannot be answered after the fact even from the database.

### Change notices: the cause next to the effect

After a record is written the server broadcasts `KB_CHANGED` + `{kbId, record}`. Invalidation and the notice are
separate decisions:

- **Every window on that KB invalidates, including the author's other windows.** Before change pushes, only the
  window that made the chat turn refreshed its case; other windows sharing the identity could remain stale.
- **Only windows of users other than the author post the notice** — *Bob: Added comment "Go to the beach." when Sun
  is hot.* A notice uses the same white rounded bubble, padding, and subtle shadow as a bot response, with grey
  italic text and the author's name retained. It remains a distinct message type, not a bot turn or model history.
  It puts the explanation beside the thing that changed, which is what was missing.

What invalidation means depends on the kind: rules, definitions, renames and moves re-fetch the displayed case (names
and order travel with the viewable case); `DESCRIPTION_SET` re-fetches the description; `CASE_DELETED` does nothing,
since the case list arrives by `casesInfo`. A refresh captures the KB and case ids at the push, fetches with those,
and applies the result only if the window still shows that context, or a slow response would overwrite a newly
selected case. A failed fetch (`Api.getCase` answers `null`) leaves what is shown in place; the notice never claims
the case was refreshed.

**No refresh during the user's own rule session.** The session is frozen on the server (`RuleBuildingSession` holds
the materialised case and cornerstones from its start) and the window holds its `CornerstoneStatus`; refreshing the
case under a review would show values the review and the chat are not about. The notice still appears, and
commit-time revalidation remains the arbiter of whether the rule can land. Once the session ends the next
`KB_CHANGED` refreshes as usual, and the client re-fetches on session end if one arrived meanwhile.

Stale-commit refusals add context the same way: the manager remembers the log position at session start and the
refusal lists the other users' changes since (at most a few) — *"… Since you started: Bob: Added comment …"*. This is
concurrent activity, not a proven cause; the wording says "since you started", not "because of".

### Socket recovery

`WebSocketApi` reconnects after failed connection attempts or closed sessions, with backoff from 1 second up to
30 seconds, reset after a successful connection. Cancellation stops the loop. While disconnected the application bar says so
(*Reconnecting…*, a display). Reconnection first re-reads the user's chat context, adopting a KB or case changed in
another window or clearing a closed KB. A missing server conversation starts a fresh chat without a KB. Until the
context is known, chat input and sending are disabled; failed context reads retry. The API selection and UI both
reject a snapshot superseded by a newer context change.

Once the context is settled, recovery re-fetches presence, rule-session status, case lists, and the displayed case.
`GET CORNERSTONE_STATUS` returns 204 for an inactive session and a status for an active one, including a session
without cornerstones. This clears review state when its completion frame was missed. Case-list recovery removes
deleted cases, discovers added cases, and reconciles the selection. Live session and list pushes take precedence
over snapshots already in flight. Recovery also fetches `GET CHANGES sinceId=<last id seen>` for its KB, posting
the missed notices. Each window remembers the last
change id it has seen per KB, and a record arriving twice (socket and replay) is dropped by `(kbId, id)`. The snapshot
is fetched after the subscription is open, so nothing falls between them. Recovery captures its starting ID before
live frames arrive and retains it if replay fails or the connection drops again, even before the recovery task runs.
It requests all records after that ID rather than the endpoint's default newest 50. A case refresh is deferred while
the recovered server session is still active; a response for a KB the window has left is discarded.

### On demand: history and presence through the chat

Two read-only actions: *"What has changed recently?"* / *"Who changed BMI?"* → `ListRecentChanges(limit,
attribute?)`, the log newest first with user and time, filtered by attribute id when one is named; *"Who else is
working on this?"* → `ListUsersPresent`, the presence set with those in `usersEditing()` marked as mid-rule. Both are
`KbManagementAction`s served through `KnowledgeBaseService` (`changesContext = false`): they are meaningful with a KB
open and no case selected, and the case-less `ChatManager` has no `RuleService`. `ListCapabilities` lists them; the
capability card is built there, not from the instruction file.

### Undo across users

`undoLastRuleSession` is KB-wide: with two users, A's "undo" removes B's rule. Two things make that an informed act
rather than an accident:

- **The confirmation is bound to what was shown.** The undo flow is two turns — `SHOW_LAST_RULE_FOR_UNDO`, then
  `UNDO_LAST_RULE` on an affirmation — and the two looked up the latest session independently, so Carol committing
  between them would make Alice's "yes" undo Carol's rule. `descriptionOfMostRecentRule` now remembers, per user, the
  record it described; `undoLastRuleSession` checks under the lock that it is still the latest, and if not refuses,
  describes the new latest rule and asks again.
- **The author is named.** *"The most recent rule was added by Bob: … Undo it?"*, and the undo is itself logged and
  broadcast, so Bob sees a notice.

A hard refusal was considered and not adopted: there are legitimate reasons to undo a colleague's rule.

Preview and undo both take the KB lock, including when called directly. An undo requires the caller's own
preview; every undo attempt consumes it. A refusal describes the new latest rule without recording another
preview. Chat carries that refusal into the next model turn, so an affirmation first requests a fresh preview
and then waits for a new confirmation. An empty history is reported without requesting confirmation.
`UndoRuleRefusedException` distinguishes these expected refusals from unexpected errors; REST returns it as
`409 Conflict` with the existing `X-Refusal: stale` header.

The in-memory rule-session and rule stores allocate increasing IDs across deletions and advance their counters
past imported IDs. Reusing a deleted session ID could otherwise authorise undo of a replacement session; reusing
a deleted rule ID could associate that replacement with an older author's change record.

## Testing

Multi-user acceptance is REST-first (`requirements/kb/Concurrent Users.feature`, `ConcurrentUsersDefs.kt`): several
`Api` instances with different user ids against one server, the chat reached through `sendUserMessage`. Direct rule,
history, and undo steps use deterministic REST calls; conditions are built in the step definitions without a model.
Steps that send chat messages use the live model. Presence checks hold WebSockets open alongside REST requests,
with connections and clients closed by the scenario teardown hook.

What a REST client cannot see is what another user's *window* does with the frames it was or was not sent. One
two-window scenario covers that (`requirements/kb/Concurrent Users with windows.feature`): Alice builds a rule with a
cornerstone in her window while Bob, on the same KB, sees no cornerstone; once she commits, his window shows the
presence icon and popup, a notice naming her, and his case updated without his reselecting it (before the awareness work his
case stayed stale until he reselected it). `StepsInfrastructure` keeps one `LaunchedClient` per named user and a
current one that the page objects address; "I switch to Alice's window" redirects them and brings her window to the
front, since Robot clicks land on whichever window is on top. The in-JVM observation hooks (`ChatTestHook`,
`CornerstoneTestHook`) are keyed by `UserId`, each window publishing under its own `Api.userId`, so a page object
reads the state of its own window and not whichever window recomposed last. Both chats are driven by the real model,
so this is the slowest scenario in the suite and there is deliberately only one.

Server-level: `KBSessionTest` (lock, per-user managers, `usersEditing`, cancellation), `StaleRuleSessionTest` (two
users through one `KBSession`), `KBEndpointConcurrencyTest` (the lab feed against a user), `RefusalsTest` (the two
409s and their header), `ChatCoordinatorTest` and `ApplicationKbServiceTest` (per-user conversations and pushes),
`WebSocketManagerTest`.

Awareness is covered headless first, the two-window scenario confirming only the visible experience: `ChangeLogTest`
and the two `ChangeLogStoreTest`s (empty rule ids, import of an older export, import followed by append with
non-contiguous ids); the writer tests in the `RuleSessionManager` and `KBEndpoint` classes (exactly one record on
success, none on refusal or no-op, mutation stands when the store throws); `WebSocketManagerTest` (dispatcher order,
slow-connection bound); `PresenceTest` and `ChatCoordinatorTest` (intersection; transitions on start, close and
delete; a KB closed while the socket stays up; two windows of one identity); `StaleRuleSessionTest` (the "since you
started" list); `UndoConfirmationTest` and `UndoConfirmationChatTest` (intervening commits and fresh preview after
refusal); `ListRecentChangesTest`, `ListUsersPresentTest`, and `AwarenessChatTest` (actions with a KB open and no case).
On the client, `OpenRDRUIChangeTest` checks the author's second window refreshing without a notice, other KBs being
ignored, deferral during a session, switching case during a refresh, and failed refresh. `WebSocketRecoveryTest`
checks reconnection; `OpenRDRUIRecoveryTest` checks replay, duplicate delivery, recovery failures, and context changes.
Over REST, `Concurrent Users.feature` covers Bob committing and Alice reading the log;
Bob leaving presence when he closes the KB while his socket stays connected; and an intervening commit causing
Alice's undo to be refused until she previews again, followed by a successful undo attributed to her. The
two-window scenario checks Bob's window showing Alice in the presence popup, the notice, and his case updated without
reselecting it.

## Out of scope

- User management, login, roles, permissions — the third-party system's job. OpenRDR sees an opaque `userId`.
- Refreshing the model's context after a `KB_CHANGED`. The system prompt's `COMMENTS` are set when the conversation
  starts and are already stale after the user's *own* commit, so this is not a gap the feature opens. Feeding the
  model updated case and change context on its next turn, without restarting the conversation, is a separate piece of
  work.
- One persistence transaction for a mutation and its change record (see the change log's guarantee).
- Coalescing a burst of `KB_CHANGED` pushes into one refresh. Cached case fetches are usually cheap, but an AI-dependent refresh can require provider work; measure
  before adding coalescing.
- A stateless, multi-instance server with the database as coordination point. It buys horizontal scalability nothing
  demands and forfeits the "one instance per object within a KB" invariant (see [architecture.md](architecture.md))
  that the rule engine relies on.
- Merging two users' concurrent edits to the *same* rule session — a session belongs to one user.

## AI work in flight

The AI coordinator releases the KB lock before record I/O or provider work. Evaluation results are shared by
key, while acceptance and rule-session mutations remain per user. Session changes and closing a KB invalidate
waiting operation guards. Candidate cases and cornerstones use fresh interpretations. Failed condition changes
and commit requests preserve the prior review; all-in-one rule creation uses a temporary session.
Provider work has an operation deadline, and cancelling one waiter does not cancel shared work for other users.
