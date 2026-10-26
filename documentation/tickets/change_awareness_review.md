# Change awareness: design and implementation-plan review

Reviewed: 23 October 2026.

Completion: the eight implementation stages and live acceptance finished on 26 October 2026. This document retains
the original review findings; current behaviour and the explicitly deferred items are described in
[the maintained design](../design/concurrent_users.md).

Documents reviewed:

- [Concurrent users design](../design/concurrent_users.md)
- Change awareness implementation plan (temporary document, removed after completion; final version in commit
  `16aeb84d`)

This is a static review of the documents and the relevant server, UI, persistence, prompt, and test code. No tests
were run as part of the review. References describe the code at review time; the awareness feature is proposed work.

The overall approach is sound: **presence, a persisted change log, and unobtrusive notices are a good fit for this
application.** Keeping awareness separate from the existing locking and commit validation is sensible. Allowing users
to undo a colleague's rule is also reasonable, provided the confirmation reliably identifies what will be removed.

The implementation plan should be revised in the following areas before implementation.

1. **Bind undo to the rule session the user actually confirmed.**

   Adding the author to the confirmation is useful, but insufficient. Alice can see "undo Bob's rule", Carol can
   commit another rule, and Alice's subsequent "yes" will undo Carol's rule.
   [`descriptionOfMostRecentRule` and
   `undoLastRuleSession`](../../server/src/main/kotlin/io/rippledown/kb/RuleSessionManager.kt)
   independently look up the latest session.

   Store a pending undo target when displaying the confirmation. At execution, under the KB lock, verify that this
   is still the latest session. If it changed, show the new description and request confirmation again. This preserves
   the proposed policy while making the confirmation meaningful.

2. **Separate invalidation from suppression of notices about your own changes.**

   Step 3 ignores the entire event when its author is the current user. That leaves the author's **second window**
   stale: only the window performing the chat turn currently refreshes its case.

   Every window displaying the affected KB should process invalidation, including the author's windows. Suppress
   the redundant notice separately. This also handles changes made through another REST client using the same
   identity, without changing the decision that identity belongs to the user.

3. **Give presence complete lifecycle handling and an initial snapshot.**

   Passing the previous context through `startConversation` handles switching between KBs, but misses closing.
   [`ChatCoordinator.knowledgeBaseClosed`](../../server/src/main/kotlin/io/rippledown/kb/chat/ChatCoordinator.kt)
   resets the context immediately. By the time the client starts its next conversation, the previous KB has already
   been forgotten. Other users can continue seeing someone who has left.

   Emit the transition when the context is actually changed or reset, including close and deletion. Also fetch a
   presence snapshot once the window has adopted its KB. A new window can receive the connection-triggered broadcast
   before its `kbInfo` is set, discard it, then adopt an existing conversation without triggering another start.

   When implementing the editing flag, make
   [`KBSession.usersEditing()`](../../server/src/main/kotlin/io/rippledown/kb/KBSession.kt) a snapshot taken under the
   KB lock. Its current implementation reads mutable session state without acquiring that lock, and
   [`ApplicationKbService.close`](../../server/src/main/kotlin/io/rippledown/server/ApplicationKbService.kt) also
   calls cancellation without that protection.

4. **Include socket recovery in the feature's correctness requirements.**

   [`WebSocketApi.startSession`](../../ui/src/main/kotlin/io/rippledown/main/WebSocketApi.kt) opens one connection and
   has no reconnect loop. After a connection loss, REST/chat can continue working while presence and notices silently
   stop.

   Include reconnect with backoff, a visible indication when awareness is unavailable, and resynchronisation after
   reconnect. The change log already provides the basis: retain the last observed change ID per KB, fetch subsequent
   records, and deduplicate socket/replayed records by `(kbId, changeId)`.

   Move the presence/history endpoints into their respective implementation steps and use them in the client,
   rather than introducing them only for acceptance testing. Define the initial snapshot and subscription ordering
   so changes cannot fall between them unnoticed.

5. **Give the refresh path stronger rules than "fetch the current case".**

   The proposed callback needs to capture the KB and case IDs, fetch using those explicit IDs, and apply the result
   only if the window still displays that context. Otherwise, a slow response can overwrite a newly selected case or
   a case from another KB. `Api.getCase` currently derives the KB from mutable `currentKB`.

   [`Api.getCase`](../../ui/src/main/kotlin/io/rippledown/main/Api.kt) returns `null` on any failure. Checking only
   `currentCaseId != null` can therefore announce "Your case has been refreshed" after a failed fetch.

   `DESCRIPTION_SET` also needs different handling: refreshing the case leaves the displayed description stale.
   Its loading effect in [`OpenRDRUI`](../../ui/src/main/kotlin/io/rippledown/main/OpenRDRUI.kt) is keyed to KB/chat
   changes, not these events.

   Use change-kind-specific invalidation, retain existing content on transient refresh failure, and handle deletion
   of the displayed case explicitly. Burst events can share one refresh while retaining their individual history
   records.

6. **Decide how live updates interact with the active rule review and the model's context.**

   The current plan refreshes one part of the screen while leaving other parts on older state.
   [`RuleBuildingSession`](../../server/src/main/kotlin/io/rippledown/model/rule/RuleBuildingSession.kt) retains
   materialised case and cornerstone data from session creation. The client retains its existing `CornerstoneStatus`.
   [`ChatManager`](../../server/src/main/kotlin/io/rippledown/kb/chat/ChatManager.kt) retains the case supplied at
   conversation start, and the model's initial prompt contains that case's comments.

   Consequently, the displayed case can change while the review and chat still describe earlier values. Another
   user's commit does **not** push refreshed cornerstone status to the reviewing user, so the document's justification
   that "its own session pushes cover it" is incomplete.

   The preferred approach is to keep an active review coherent, display a notice that the KB changed, and retain
   commit-time revalidation. Outside a review, refresh normally. Also provide updated case/change context to the
   model on the next turn without restarting the conversation. The visual notice itself can remain outside model
   history.

7. **Do not extend the practice of broadcasting while holding the KB lock.**

   Step 3 proposes `runBlocking` broadcasts from `ChangeLog.onRecorded`.
   [`WebSocketManager.broadcast`](../../server/src/main/kotlin/io/rippledown/server/websocket/WebSocketManager.kt)
   sends sequentially to every connection. Ktor explicitly allows `send` to suspend when its outgoing queue is full;
   buffering does not guarantee a short stall. See the
   [Ktor documentation](https://api.ktor.io/3.3.x/ktor-websockets/io.ktor.websocket/send.html).

   A slow client, even one viewing another KB, can therefore delay operations on the edited KB.

   Record and enqueue an immutable event under the lock, then deliver outside it through an ordered dispatcher.
   Preserve per-KB ordering and bound slow-client delivery. The persisted log supplies recovery if delivery fails;
   a message broker is unnecessary for the current architecture.

8. **Specify what happens when recording the change fails after the edit succeeds.**

   The KB lock gives mutual exclusion, not transactional persistence. The existing Postgres stores, including
   [
   `PostgresRuleSessionRecordStore`](../../server/src/main/kotlin/io/rippledown/persistence/postgres/PostgresRuleSessionRecordStore.kt),
   perform their own transactions, and a rule commit already spans multiple writes.

   Simply appending `changeLog.record()` creates another failure point after the knowledge has changed. A logging
   failure could leave an applied edit without history, potentially returning an error that encourages the user to
   repeat it.

   Prefer one persistence transaction for the mutation and its record, with explicit handling of the in-memory state
   if persistence rolls back. If that is outside this increment, document the weaker guarantee and define the failure
   response. Socket delivery failure should remain separate from mutation success.

9. **Make both awareness actions KB-level actions.**

   `ListRecentChanges` is proposed as a `ChatAction` using `RuleService`, but
   [`ChatManagerFactory.caseLess`](../../server/src/main/kotlin/io/rippledown/kb/chat/ChatManagerFactory.kt) constructs
   a manager with no `RuleService`. An open KB without a selected case would consequently report that no KB is open
   when this action executes.

   Implement history through `KnowledgeBaseService`, like presence, with `changesContext = false`. Both are meaningful
   without a case. Also update
   [`ListCapabilities`](../../server/src/main/kotlin/io/rippledown/kb/chat/action/ListCapabilities.kt), which generates
   the actual capability display; changing the instruction file alone will not update that display.

Several smaller but concrete corrections should also be incorporated:

| Area                      | Suggested correction                                                                                                                                                                                                                                                                                                               |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Older exports             | [`DirectoryImporter`'s flag](../../server/src/main/kotlin/io/rippledown/kb/export/DirectoryImporter.kt) permits an **empty** directory, not a missing directory. Guard `changeLogDirectory.isDirectory`, as the definitions importer does.                                                                                         |
| Empty rule IDs            | [`parseToIds`](../../server/src/main/kotlin/io/rippledown/model/rule/RuleSessionRecord.kt) cannot parse `""`. Most proposed event kinds have no rule IDs, so explicitly support the empty set.                                                                                                                                     |
| Imported IDs              | Test **import followed by append**, including non-contiguous IDs. A round-trip test alone does not verify that generated IDs remain above imported records.                                                                                                                                                                        |
| Summary wording           | [`Rule.actionSummary`](../../common/src/main/kotlin/io/rippledown/model/rule/Rule.kt) produces multiline action descriptions and omits conditions. It does not produce the example "added a rule ... when Sun is hot". Define a concise history formatter and capture the conditions if they are part of the intended explanation. |
| "Who changed BMI?"        | Returning the latest ten unrelated records cannot reliably answer this. Add affected attribute/condition/case IDs and filtering, or narrow the advertised capability to recent activity. Stable IDs also survive renames.                                                                                                          |
| Stale refusal attribution | "Changes since you started" is useful context, but does not establish which change caused the refusal. Avoid calling every listed author the culprit; bound the list and distinguish concurrent activity from a proven cause.                                                                                                      |
| Writer tests              | Assert **exactly one** record after success and none after rejection/cancellation/no-op. For example, deleting a nonexistent processed case currently succeeds without changing anything.                                                                                                                                          |
| Documentation consistency | Update the rejected presence-indicator entry in [chat UI guidelines](../design/chat_ui_guidelines.md), as well as the old two-window expectation. Clarify that an offline user's abandoned session can still block deletion despite their absence from presence.                                                                   |

Expand the headless tests around the failure cases above:

- Another commit between undo preview and confirmation.
- Two windows sharing an identity.
- Closing a KB while retaining the socket.
- Joining an existing conversation and receiving the initial presence snapshot.
- Reconnect, replay, ordering, and duplicate delivery.
- Switching KB or case during a refresh.
- Failed refresh and deletion of the displayed case.
- Remote changes during rule review.
- History access with an open KB but no selected case.
- Importing an older export, empty rule IDs, and appending after import.
- Mutation/logging failure behaviour and exactly-once recording of successful changes.

These tests should carry most of the concurrency coverage, with the single two-window scenario confirming the visible
experience. The proposed six-step structure is workable after these amendments. Move reliable delivery and
resynchronisation into the presence and notice steps, and treat undo target validation as part of undo disclosure.
