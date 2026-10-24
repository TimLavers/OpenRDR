# Awareness of other users: implementation plan

Throwaway. Design is the "Awareness of other users" section of `documentation/design/concurrent_users.md`; delete
this file when the last step lands. Each step is one commit, TDD, `:common:test :server:test` (filtered) after each.

Reviewed 23 Oct 2026 (`change_awareness_review.md`); this version incorporates the accepted points.

Order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8. Step 1 alone already answers "who did that?" from the database. Step 2 is a
prerequisite for every new push. Step 4 is what removes the day-to-day confusion. Steps 6 and 7 are independent of
each other once 1–5 are in.

## Step 1: the change log — DONE (24 Oct 2026)

As planned, except:

- `ChangeLog` has no `onRecorded` hook yet; it arrives in step 4 with its only consumer (the `KbChange` push).
- Queries are `mostRecent(count)` (not `recent`), `since(id: Int?)`, `concerning(attributeId, limit)`.
- Export classes are `ChangeRecordsExporter` + `ChangeRecordsSource`.
- `GET CHANGES` takes `sinceId` and `limit` query parameters (`SINCE_ID`, `LIMIT` in `constants.server`); `limit`
  defaults to `DEFAULT_CHANGES_LIMIT` = 50. Records are returned oldest first, keeping the newest `limit`.
- Existing Postgres databases need the `change_log` table created by hand (see `PostgresChangeLogStore`).

### Model (`common`)

`io.rippledown.model.change`:

```kotlin
@Serializable
enum class ChangeKind { RULES_ADDED, RULES_UNDONE, DEFINITION_EDITED, ATTRIBUTE_RENAMED, CONDITION_RENAMED,
                        ATTRIBUTE_MOVED, CASE_DELETED, DESCRIPTION_SET }

@Serializable
data class ChangeRecord(
    val id: Int?,
    val userId: String,                     // UserId is a value class; keep the wire type plain
    val timestamp: Long,                    // epoch millis, server clock
    val kind: ChangeKind,
    val summary: String,                    // one line, see ChangeSummary below
    val ruleIds: Set<Int> = emptySet(),
    val attributeIds: Set<Int> = emptySet() // what the change concerns; lets "who changed BMI?" filter by id
)

@Serializable
data class KbChange(val kbId: String, val record: ChangeRecord)   // the push payload
```

In `common` because the client renders it (step 4). Tests: `ChangeRecordTest` (serialise/deserialise via
`serializeDeserialize`, empty and non-empty id sets).

### Store (`server`)

- `persistence/ChangeLogStore`: `all(): List<ChangeRecord>` (oldest first), `create(record): ChangeRecord` (assigns
  id), `load(Set<ChangeRecord>)` (import, refuses if non-empty — same contract as `RuleSessionRecordStore`).
- `persistence/inmemory/InMemoryChangeLogStore`.
- `persistence/postgres/PostgresChangeLogStore`, table `change_log`:
  `id SERIAL PK, user_id VARCHAR(256), timestamp BIGINT, kind VARCHAR(32), summary TEXT, rule_ids VARCHAR(2048),
  attribute_ids VARCHAR(2048)`. **Do not reuse `parseToIds`**: `"".split(' ')` is `[""]` and `toInt()` throws, and most
  kinds have no rule ids. Write an `idsFromString` that maps `""` to the empty set (and fix `parseToIds` to use it
  only if a test shows it matters there).
- `PersistentKB.changeLogStore()`; wire in `InMemoryKB`, `PostgresKB`.
- Tests: `InMemoryChangeLogStoreTest`, `PostgresChangeLogStoreTest` (user runs; add the table to `clearTablesOf` in
  `PostgresTestUtils.kt`). Both must cover: empty id sets round-trip; **`load` then `create`** with non-contiguous
  imported ids (e.g. 3, 7) yields an id above 7 — a round-trip test alone does not show that. For Postgres this means
  `load` must leave the sequence above the imported ids (`setval`), as the rule session store presumably does not;
  check `PostgresRuleSessionRecordStore.load` and fix both if it is wrong.

**One-off SQL for every existing KB database** (each KB has its own database — run it in each):

```sql
CREATE TABLE IF NOT EXISTS change_log (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(256) NOT NULL,
    timestamp BIGINT NOT NULL,
    kind VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    rule_ids VARCHAR(2048) NOT NULL DEFAULT '',
    attribute_ids VARCHAR(2048) NOT NULL DEFAULT ''
);
```

Then reconfigure. (Fresh databases get it from `SchemaUtils.create`.)

### Manager

`kb/ChangeLog(store, clock: () -> Long = System::currentTimeMillis, onRecorded: (ChangeRecord) -> Unit = {})`:

- `record(userId: UserId, kind, summary, ruleIds = emptySet(), attributeIds = emptySet()): ChangeRecord?` — builds the
  record, `store.create` inside a `try`; on failure logs at **error** with the record's content and returns the
  unpersisted record anyway (null id). Either way calls `onRecorded(record)`. This is the documented weaker guarantee:
  the mutation has already happened and must not be reported as failed.
- `all()`, `recent(limit: Int)` (newest first), `since(id: Int?)` (id greater than the given one; null = everything),
  `concerning(attributeId, limit)`.
- `KB.changeLog` alongside `ruleSessionRecorder`.
- Tests: `ChangeLogTest` (ordering, `since`, `concerning`, clock injected; store that throws → record returned,
  `onRecorded` still called, nothing else thrown).

### Summary formatter

`kb/ChangeSummary` (server, one object): one line per kind, written for a colleague reading history.
`Rule.actionSummary`
is multi-line (for the panel) and omits conditions, so it is **not** used here.

| Kind                | Summary                                                                                                                                                                                                           |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RULES_ADDED`       | `Added comment "Go to the beach." when Sun is hot, TSH is high` / `Removed comment … when …` / `Replaced comment … with … when …` / `Assigned BMI = weight / height ^ 2 when …`; no conditions → no `when` clause |
| `RULES_UNDONE`      | `Undid: ` + the summary the `RULES_ADDED` record had (look it up by rule ids; fall back to recomputing before deletion)                                                                                           |
| `DEFINITION_EDITED` | the string `editDerivedAttributeDefinition` already returns                                                                                                                                                       |
| `*_RENAMED`         | the strings `renameAttribute` / `renameCondition` already return                                                                                                                                                  |
| `ATTRIBUTE_MOVED`   | `Moved "X" to the position of "Y".`                                                                                                                                                                               |
| `CASE_DELETED`      | `Deleted case "X".`                                                                                                                                                                                               |
| `DESCRIPTION_SET`   | `Set the description.`                                                                                                                                                                                            |

The rule line uses the rule's own conditions (`rule.conditions.map { it.asText() }`), not `conditionsFromRoot`. Tests:
`ChangeSummaryTest`, one per branch, including a rule with no conditions.

### Writers

All under the lock already; each writer gets the caller's `UserId`. Record **after** the mutation has succeeded, as the
last statement of the locked block; a refusal (exception) or a no-op records nothing.

| Where                                                                | Kind                | `attributeIds`                          |
|----------------------------------------------------------------------|---------------------|-----------------------------------------|
| `RuleSessionManager.commitCurrentRuleSession`                        | `RULES_ADDED`       | the attribute the rule assigns/retracts |
| `RuleSessionManager.undoLastRuleSession`                             | `RULES_UNDONE`      | same, from the rules deleted            |
| `RuleSessionManager.editDerivedAttributeDefinition`                  | `DEFINITION_EDITED` | the attribute                           |
| `RuleSessionManager.renameAttribute` / `renameCondition`             | `*_RENAMED`         | the attribute / none                    |
| `RuleSessionManager.moveAttributeTo`, `KBEndpoint.moveAttribute`     | `ATTRIBUTE_MOVED`   | the moved attribute                     |
| `RuleSessionManager.deleteCaseFromUserList`, `KBEndpoint.deleteCase` | `CASE_DELETED`      | none                                    |
| `KBEndpoint.setDescription`                                          | `DESCRIPTION_SET`   | none                                    |

- `RuleSessionManager.userId` is nullable (test fixtures). Record only when non-null: `userId?.let { ... }`. Do not
  invent a default id (see the identity section of the design doc).
- `KBEndpoint.moveAttribute`, `deleteCase`, `setDescription` gain a `userId: UserId` parameter; the routes pass
  `userId()`; `ApplicationKbService.setDescription` has `userId` already. Sample builders pass their fixed user.
- `KB.deletedProcessedCaseWithName` silently succeeds when there is no such case; make it return `Boolean` so the
  endpoint records only when something was deleted.
- Tests, one per writer in the existing `RuleSessionManager*Test` / `KBEndpointTest` classes: after success **exactly
  one** record with the caller's `userId`, the `kind` and the expected `attributeIds`; after a refusal (e.g. a stale
  commit, a rename to a name in use) **no** record; after a no-op (delete a case that is not there) no record. Do not
  re-pin the summary prose here — `ChangeSummaryTest` owns it.

### Export / import

`kb/export/ChangeLogExporter` + `ChangeLogSource`, directory `ChangeLog` in `KBExportImport`. `DirectoryImporter`'s
flag allows an *empty* directory, not a missing one (`checkIsDirectory` in its `init`), so `KBImporter` guards
`changeLogDirectory.isDirectory` as it does for definitions and imports an empty log otherwise. Tests:
`KBExporterTest`/`KBImporterTest` round-trip one record; importing a zip without the directory (one of the existing
test zips) yields an empty log.

### REST

`GET CHANGES` (`KBEndpoint.changes(sinceId: Int?, limit: Int)`) — used by the client in step 5 and by the cukes in
step 8, so it belongs here, not with acceptance.

## Step 2: the push dispatcher (pushes leave the lock)

Prerequisite for every new push; also fixes the existing ones. Design doc, lock section item 4.

- `WebSocketManager` gains a `Channel<Outgoing>(UNLIMITED)` and one consumer coroutine (scope injected; started by
  `OpenRDRServer`, cancelled on shutdown / test reset). `Outgoing` = `(targets: Set<UserId>?, message)`; null = all.
- `fun enqueue(message)` / `fun enqueueTo(userId, message)` — plain functions, `trySend`, never suspend.
- The consumer sends in order; each per-connection `send` runs under `withTimeoutOrNull(SEND_TIMEOUT)`; on timeout or
  exception log a warning and move on (the change log is the recovery path once step 5 is in).
- Migrate the existing pushes: `sendStatus`, `sendCasesInfo`, `sendRuleSessionCompleted`, `sendKbInfo`, `sendKbClosed`,
  `sendChatContext` become enqueue calls; their `suspend` modifiers and the `runBlocking` wrappers in
  `RuleSessionManager` and `KBSession` go. Keep the method names so call sites and tests change minimally.
- `TestRequestGate`: check it does not depend on pushes completing synchronously before a request returns — if a
  cucumber step reads a push immediately after a REST reply, the consumer must have drained first. If it does, give
  the gate a `drain()` that the test server reset awaits.
- Tests: `WebSocketManagerTest` — order preserved across users; a connection whose `send` blocks does not delay
  delivery to the others beyond the timeout; `enqueue` from a thread holding a `ReentrantLock` returns immediately.

## Step 3: presence

### Server

- `common`: `PresenceInfo(kbId: String, users: List<PresentUser>)`, `PresentUser(userId: String, editing: Boolean)`;
  `PRESENCE_PREFIX` beside `CASES_INFO_PREFIX` in `constants/chat`.
- `server/Presence(openKnowledgeBases, webSocketManager, kbSessionFor: (kbId) -> KBSession?)`:
    - `present(kbId): List<PresentUser>` = `usersOn(kbId) ∩ connectedUsers()`, `editing` from
      `session.locked { session.usersEditing() }` — **under the lock**, since `usersEditing` reads session state the
      lock
      protects (today it does not take it; fix in `KBSession.usersEditing` itself so every caller is covered, and
      likewise `cancelRuleSessionOf`).
    - `push(kbId)`: `webSocketManager.enqueue(PRESENCE_PREFIX + PresenceInfo(...).toJsonString())`.
    - `transition(fromKbId: String?, toKbId: String?)`: push for each non-null, distinct id.
- Triggers, each reported by the owner of the state that changed:
    1. `WebSocketManager.setSession`: after register and in the `finally`, call injected
       `connectionsChanged: (UserId) -> Unit`; `ServerApplication` wires it to
       `presence.push(kbId of openEndpointFor(userId))`, if any.
    2. `ChatCoordinator`: replace the `contextChanged: suspend (UserId, ChatContext) -> Unit` constructor lambda's
       contract with `(userId, from: ChatContext, to: ChatContext)` and call it from **all three** places the context
       changes — `startConversation`, `knowledgeBaseClosed`, `knowledgeBaseDeleted` (the latter two reset immediately;
       a hook on start alone leaves a departed user listed). `ServerApplication` keeps sending `CHAT_CONTEXT` for the
       start case and calls `presence.transition(from.kbId, to.kbId)` for all.
    3. `editing` flag: `KBSession` gives each `RuleSessionManager` a `sessionStateChanged: () -> Unit` that it calls on
       session start, commit, cancel and stale refusal; `KBSession` forwards to `presence.push(kb.id)`.
- `GET PRESENCE` route → `presence.present(kbId)`; used by the client for its snapshot, below.
- Tests: `PresenceTest` (intersection; disconnected user drops out; user on another KB excluded; editing flag),
  `WebSocketManagerTest` (callback on open and on close), `ChatCoordinatorTest` (transition reported on start, on
  close with the socket still up, on delete; two windows of one identity count once), `KBSessionTest` (`usersEditing`
  under the lock).

### Client

- `WebSocketApi.startSession` gains `presenceChanged: (PresenceInfo) -> Unit`; `OpenRDRUI` keeps
  `othersPresent: List<PresentUser>` for `info.kbId == kbInfo?.id`, minus `api.userId`.
- **Snapshot**: a `LaunchedEffect(kbInfo?.id)` fetches `GET PRESENCE` once the window has a KB. Needed because a new
  window can receive the connection-triggered push before `kbInfo` is set and discard it, and a window that adopts an
  existing conversation starts none of its own, so no push is guaranteed to follow.
- `ApplicationBar(kbInfo, description, othersPresent: List<String>)`: after the name, a subdued
  `Text("Also here: Bob, Carol")`, nothing when empty. Content description constant `PRESENCE_DESCRIPTION` in
  `constants/kb` for the page object.
- Tests: `ApplicationBarTest` (absent when alone, names joined), `OpenRDRUITest` (excludes self; snapshot fetched on
  KB adoption; push for another KB ignored), `WebSocketApiTest` frame routing.

## Step 4: change notices

### Server

- `KB_CHANGED_PREFIX` constant; `KBSession` wires `ChangeLog.onRecorded` to
  `webSocketManager.enqueue(KB_CHANGED_PREFIX + KbChange(kb.id, record).toJsonString())`. Writers call `record` and
  nothing else.
- Stale refusals: `RuleSessionManager` stores `changeLogIdAtStart` in `startRuleSession`; `refuseStaleCommit(message)`
  appends `kb.changeLog.since(changeLogIdAtStart).filter { it.userId != userId }.takeLast(3)` as
  `" Since you started: Bob: Added comment …"` — context, not a claimed cause. `StaleRuleSessionTest` gains one
  assertion.
- Tests: `KBSessionTest` (record → enqueue, payload carries kbId; a record the store failed to persist is still
  pushed), `StaleRuleSessionTest`.

### Client

- `WebSocketApi.startSession` gains `kbChanged: (KbChange) -> Unit`.
- `OpenRDRUI`, on a `KbChange` whose `kbId == kbInfo?.id` (any author — the author's **other** windows are stale too):
    1. remember `lastSeenChangeId[kbId] = record.id` (step 5 uses it); drop the event if the id was already seen;
    2. **invalidate by kind**, unless a rule session is in progress in this window (`CornerstoneStatus` / session flag),
       in which case set `refreshPendingAfterSession = true` and refresh when `ruleSessionCompleted` fires:
        - `RULES_ADDED`, `RULES_UNDONE`, `DEFINITION_EDITED`, `*_RENAMED`, `ATTRIBUTE_MOVED`: capture `(kbId, caseId)`
          now,
          `api.getCase(caseId)`, apply only if the window still shows that `kbId`/`caseId`; a `null` result leaves the
          current case in place;
        - `DESCRIPTION_SET`: re-run the description load;
        - `CASE_DELETED`: nothing (`casesInfo` handles the list and the selection);
    3. **notice only if `record.userId != api.userId.value`**:
       `chatState.notice("${record.userId}: ${record.summary}")`.
       The notice does not mention refreshing.
- `ChatState.notice(text)` appends `NoticeMessage(text)` (new `ChatMessage` type, `isUser = false`).
- `ChatPanel`: `is NoticeMessage -> NoticeRow(text, index)` — subdued, full-width, no bubble, italic; content
  description prefix `NOTICE_PREFIX` so `ChatPO` can read it. Not counted by `ChatTestHook.mostRecentBot`.
- Tests: `ChatStateTest`, `ChatPanelTest` (notice row rendered, not a bot message), `OpenRDRUITest`: author's own
  change refreshes without a notice; other KB ignored; other user's change refreshes and posts a notice; no refresh
  during a rule session, one refresh when it completes; case switched during the fetch → result discarded; `getCase`
  returns null → case unchanged; `DESCRIPTION_SET` reloads the description; duplicate id dropped.

## Step 5: socket recovery

A pre-existing gap (`WebSocketApi.startSession` opens one connection; the design doc already noted there is no
reconnect loop) that awareness makes load-bearing. Separate commit because it also fixes `casesInfo` and cornerstone
pushes going silent after a drop.

- `WebSocketApi.startSession` loops: connect, collect until the connection ends, then back off (1 s doubling to 30 s)
  and reconnect, until the scope is cancelled. Report `connected: (Boolean) -> Unit`.
- `OpenRDRUI`: while disconnected the application bar shows *Reconnecting…* (subdued, display only; constant
  `RECONNECTING_DESCRIPTION`).
- On each (re)connect, **after** the socket is open: re-fetch presence (`GET PRESENCE`), re-fetch the displayed case,
  and `GET CHANGES since=lastSeenChangeId[kbId]`; feed the returned records through the same `KbChange` handler as the
  live frames (so dedupe by id, invalidation and notices all apply). Order: subscribe, then snapshot, so nothing falls
  between.
- Tests: `WebSocketApiTest` (reconnects after the server closes; backoff grows; `connected` toggles), `OpenRDRUITest`
  (replayed records produce notices once; a record seen live and again in replay is dropped; indicator shown while
  down).

## Step 6: chat actions

Both are `KbManagementAction`s (`changesContext = false`) through `KnowledgeBaseService`, **not** `ChatAction`s: the
case-less `ChatManager` (`ChatManagerFactory.caseLess`) has no `RuleService`, and both are meaningful with a KB open
and no case selected.

- `KnowledgeBaseService` gains `recentChanges(limit: Int, attributeName: String?): List<ChangeRecord>` and
  `usersPresent(): List<PresentUser>`; `ApplicationKbService` implements them via the open endpoint's
  `kb.changeLog` (locked) and `Presence`.
- `kb/chat/action/ListRecentChanges(limit: Int = 10, attribute: String? = null)`: when `attribute` is given, resolve it
  to an id (`attributeNamedExactly`) and use `changeLog.concerning(id, limit)`; unknown name → say so. Text: one line
  per record, newest first, `"<time> — <user>: <summary>"`, time `HH:mm` today else `d MMM HH:mm`. Empty → "No
  changes have been recorded for this knowledge base." / "No recorded changes concern \"BMI\"."
- `kb/chat/action/ListUsersPresent`: "Also working on <KB>: Bob (building a rule), Carol." / "Nobody else is working
  on <KB> right now."
- Instruction `21_awareness.md`: both actions, trigger phrases ("what changed", "who changed X" → with `attribute`,
  "recent changes", "who else is here / working on this"). Register in `KBChatService` in both the case and case-less
  section lists. `ActionComment` gains `limit` and `attribute` (optional; reflection binds by name).
- `ListCapabilities` builds the capability card in code — add an "Other users" section there (and mention it in
  `16_listing_capabilities.md` only if that file enumerates sections).
- Tests: `ListRecentChangesTest` (incl. attribute filter and unknown attribute), `ListUsersPresentTest`,
  `ActionCommentParsingTest`, and a `ChatManager`-level test with a KB open and **no case** that the action runs.

## Step 7: undo bound to what was shown

The flow is two turns (`SHOW_LAST_RULE_FOR_UNDO` → `UNDO_LAST_RULE` on affirmation, instruction
`11_undoing_the_report_change.md`) and the two look up the latest session independently, so a commit by anyone in
between makes the "yes" undo the wrong rule.

- `RuleSessionManager.descriptionOfMostRecentRule()`: remember `pendingUndoRecordId = record.id` (per-user state, the
  manager is per user). Prefix the description with `"The most recent rule was added by <user>: "` when the matching
  `RULES_ADDED` change record's `userId` differs from the caller's (pre-log rules have no record → no prefix).
- `undoLastRuleSession()`: under the lock, `check(latest.id == pendingUndoRecordId)`; on mismatch clear the pending
  id and throw `IllegalStateException("The most recent rule changed since it was shown. <new description> Undo it?")`
  — the chat shows the message, and the instruction says that an affirmation after this message is a fresh
  confirmation (re-emit `SHOW_LAST_RULE_FOR_UNDO` first). Clear `pendingUndoRecordId` after a successful undo.
- Already logged (step 1) and pushed (step 4), so the owner gets a notice.
- Tests: `RuleSessionManagerTest` — own rule: plain description; other user's rule: prefixed; commit between preview
  and confirmation: refused with the new description, nothing deleted; undo after a fresh preview: succeeds.

## Step 8: acceptance

- `requirements/kb/Concurrent Users.feature` (REST, no LLM):
    - "Bob's commit is in the change log": Alice reads `GET CHANGES limit=5` and sees one record with user `Bob`, kind
      `RULES_ADDED`, a summary containing the comment text.
    - "Alice's stale refusal lists Bob's change": extend the existing stale scenario's message assertion to contain
      "Since you started: Bob".
    - "Presence": Alice and Bob open the KB; `GET PRESENCE` lists both; Bob closes the KB in chat (socket stays up);
      Alice's list drops him. The `Api` instances in `ConcurrentUsersDefs` must hold a web socket for this — check how
      `Api` opens its socket and whether the REST-only client can; if not, cover it in `PresenceTest` and the windows
      scenario only.
    - "Undo is bound to the rule shown": Alice previews undo, Bob commits, Alice confirms → refused, Bob's rule intact.
- `requirements/kb/Concurrent Users with windows.feature`: extend the one scenario, do not add a second:
    - after both windows are on the KB: `Then Bob's window should show that Alice is also here` (`ApplicationBarPO`
      reads `PRESENCE_DESCRIPTION`);
    - after Alice commits: `Then Bob's window should show a notice containing "Alice"` and his case shows the new
      comment **without** reselecting (replaces the current "no change to his case until he reselects it" step; design
      doc already updated).
- `cucumberDryRun` green; the user runs the real scenarios.

## Deferred / decided against

- Hard refusal of undo across users (disclosure plus binding instead; see design doc).
- Presence of users logged on but on other KBs (noise; the KB list could show a count later).
- Relative timestamps in notices (live) — only `ListRecentChanges` formats times.
- Re-fetching the cornerstone on `KB_CHANGED` (no refresh at all during a session; revalidation decides).
- Refreshing the model's context (`COMMENTS` in the system prompt) after a `KB_CHANGED` — already stale after the
  user's own commit today; separate work.
- One transaction for mutation + record (no unit of work in the persistence layer; weaker guarantee documented).
- Coalescing bursts of `KB_CHANGED` into one refresh (fetch is ms-scale; add if observed).
- Pruning the change log. Append-only; revisit if a KB accumulates tens of thousands of records.
