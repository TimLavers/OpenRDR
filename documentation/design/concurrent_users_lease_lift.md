# Concurrent users — lifting the lease

Fourth increment of Stage 3 in [concurrent_users.md](concurrent_users.md). With the per-KB lock
([concurrent_users_write_lock.md](concurrent_users_write_lock.md)), per-user sessions
([concurrent_users_per_user_sessions.md](concurrent_users_per_user_sessions.md)) and commit-time revalidation
([concurrent_users_revalidation.md](concurrent_users_revalidation.md)) in place, the Stage 2 lease is no longer
doing the job it was introduced for. This increment removes it, and is the first in Stage 3 that a user can see:
two people can build rules in the same knowledge base at the same time.

## What the lease was protecting, and what takes over

Stage 2 listed the hazards the lease closed (contract points 3 and 7). Each needs a successor or an explicit
acceptance:

| Hazard                                        | Under the lease                                     | After the lift                                                                                                                                                                                             |
|-----------------------------------------------|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Two rule sessions mutating one KB             | one editor per KB                                   | the lock serialises engine calls; sessions are per user                                                                                                                                                    |
| A commits against an interpretation B changed | impossible                                          | revalidation refuses and cancels (previous increment)                                                                                                                                                      |
| B deletes the KB while A is mid-rule          | refused: "held by A"                                | **still refused**, but the predicate is "a user other than the caller has an active rule session", not "holds the lease"                                                                                   |
| B deletes the case A is building a rule on    | refused                                             | **allowed.** A's session holds the case; her commit still adds a sound rule and keeps her case as a cornerstone. Nothing is corrupted, and refusing would need a per-case ownership table for a rare event |
| Two users overwrite the KB description        | refused                                             | **allowed**, last write wins. A one-shot overwrite of prose, with no session behind it                                                                                                                     |
| Rename while another user is editing          | refused                                             | **allowed**; nothing in a session depends on the KB name                                                                                                                                                   |
| Pushes addressed to the holder                | `lease.holder()`                                    | already per user since the per-user increment                                                                                                                                                              |
| Abandoned session blocking others             | 10-minute expiry                                    | nothing to block; an abandoned session is a few fields in memory until the user cancels or the server restarts                                                                                             |
| Closing the KB in the chat                    | releases the lease, cancelling the holder's session | still cancels *that user's* session (the user has walked away from the KB); no lease to release                                                                                                            |

So one refusal survives: deleting a KB that another user is editing. It keeps the Stage 2 sentence ("Thyroids is being
edited by Alice.") and `ProjectHeldException`, so the chat answer, the 409 and the `X-Refusal:
held` header all stay, and the `stale` / `held` distinction in `Api` remains meaningful. What goes is the lease
itself — the thing that made *every* guarded action a refusal.

## Changes

- **`KBSession`.** Drop `lease`, `hold`, `release`, `clock`. Add `usersEditing(): Set<UserId>` (users whose
  `RuleSessionManager` has an active session) and `cancelRuleSessionOf(userId)` (today's `leaseLostBy`, kept for
  close and delete).
- **`ProjectLease`** and its test: deleted; the `kb.lease` package goes with them. `ProjectHeldException` moves to
  `io.rippledown.kb` and is thrown by `ServerApplication.deleteKB` when `usersEditing() - userId` is non-empty,
  naming one of them. `renameKB` keeps its `userId` parameter for logging and drops the `hold`.
- **REST.** `heldKbEndpoint()` becomes `kbEndpoint()` everywhere — the guarded/unguarded distinction disappears from
  the routes. `LeaseRefusals.kt` keeps both handlers, renamed to `Refusals.kt` / `refusals()`.
- **Chat.** `LeasedRuleService` becomes `LockedRuleService(session, delegate)`: the `hold` goes, the lock stays. (The
  plan said "no wrapper at all", but `RuleSessionManager` takes the lock only around the attribute lookups in
  `conditionForExpression`; the surfaces — `KBEndpoint` and this wrapper — take it for everything else.)
  `ApplicationKbService.close()` calls `cancelRuleSessionOf(userId)` instead of `release`. `ChatManager`'s held
  handling stays for the delete case; `projectHeldChatMessage` now says the KB cannot be deleted right now,
  rather than inviting the user to look at its cases.
- **Client.** The `moveAttribute` 409 path in `OpenRDRUI` and `attributeOrderNotChangedWarning` go: a GUI action can
  no longer be refused as held. `KnowledgeBaseHeldException` stays (the `Api` can still see a held 409 from
  `DELETE_KB`). The `Attribute ordering.feature` scenario "cannot be re-ordered while another user is editing" is
  deleted with the behaviour.
- **Tests that pin the lease** — `LeaseGuardTest`, the hold/release parts of `KBSessionTest`,
  `ApplicationKbServiceTest`, `ServerApplicationTest` — are deleted or reduced to the delete refusal.

## Acceptance

In `requirements/kb/Concurrent Users.feature`:

- Deleted (behaviour gone): "A knowledge base being edited by one user is refused to another", "A refused editor
  does not disturb …", "Closing a knowledge base lets another user edit it", "Editing one knowledge base does not
  affect another".
- Kept: "The chat tells a user who is editing the knowledge base they want to delete" (now driven by the active
  session, same words), and "… can still be read by another", now checking the value Bob read rather than a
  vacuous "request succeeds".
- New:
    - *Two users build rules in the one knowledge base at the same time.* Alice and Bob each start a session on their
      own case and add different comments, each with a condition that excludes the other's case; both commits
      succeed; each case has its comment.
    - *A rule committed against an interpretation another user has changed is refused.* Alice starts adding "Go to
      Bondi." to Case1; Bob adds "Go to Bondi." to Case2 with no condition and commits; Alice's commit is refused
      with the interpretation-changed sentence, and Case1 has the comment (from Bob's rule).
    - *Closing a knowledge base does not disturb another user's rule session.* Alice and Bob each have a session;
      Alice closes the KB in the chat; Bob's commit succeeds. (The plan also had Alice's own commit refused
      afterwards, but with no session in progress that is an ordinary server error rather than a sentence the
      client gets, and the cancellation itself is pinned by `KBSessionTest` and `ApplicationKbServiceTest`.)

All REST-driven. The conditions are built in `ConcurrentUsersDefs.kt` from "TSH ≤ 1.0" / "TSH ≥ 10.0" rather than
translated, so no LLM is involved; a commit carries them in its `RuleRequest`. With no REST request on these paths
refusable as held any more, the "request succeeds / is refused with" steps went too.

## Out of scope

- Broadcast invalidation (a "KB changed" push so other clients re-fetch the current case). Without it, Alice's
  *displayed* interpretation can be stale until her next fetch; her *commit* cannot be, thanks to revalidation.
- A "who is editing" indicator (see the Stage 2 follow-up note).
- Stage 4.
