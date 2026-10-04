# Concurrent users — commit-time revalidation

Third increment of Stage 3 in [concurrent_users.md](concurrent_users.md). Builds on the per-KB lock
([concurrent_users_write_lock.md](concurrent_users_write_lock.md)) and per-user sessions
([concurrent_users_per_user_sessions.md](concurrent_users_per_user_sessions.md)). It is the last piece needed before
the Stage 2 lease can be lifted; the lease itself is untouched here, so like the previous increment this changes
nothing a user can see today.

## The hazard

Alice starts a rule session on case C to add comment X. Before she commits, Bob commits a rule that changes C's
interpretation — perhaps giving C the comment X by another route, or removing the comment Alice's session is
about to replace. Alice's `RuleBuildingSession` holds the *live* `kb.ruleTree` (`RuleBuildingSession.tree` is the
KB's tree, not a copy), so when she commits, `action.createChanger(tree, …).updateRuleTree(case, conditions)`
walks a tree that no longer gives C the interpretation her session was started against. Depending on the action
this either throws from deep inside the changer or, worse, adds a rule under the wrong parent.

Separately, the conflicting-cornerstone set was computed once at session start from `kb.allCornerstoneCases()`
and the tree as it then was. Bob's commit adds his case as a cornerstone and may change which cornerstones the
rule in progress would disturb. Alice's review is of a stale set.

## Decision: re-check applicability at commit; reject and cancel on failure

Of the three candidate checks — (a) the case's whole interpretation is unchanged since start, (b) the change the
session makes still applies, (c) none — (b) is the one the diff semantically depends on, and it tolerates unrelated
rules landing in the meantime. The predicate already exists: `RuleTreeChange.isApplicable(tree, case)` is what
`RuleSessionManager.startRuleSession` checks before creating the session (an addition needs the conclusion absent,
a removal or replacement needs it present). Revalidation is re-running that same check against the current tree
just before `session.commit()`.

A failed check **rejects the commit and cancels the session**. The alternative — keep the session and restart it
against the fresh interpretation — is not well-defined: if the comment Alice wanted to add is now already given,
there is nothing left to do; if the comment she wanted to remove is gone, likewise. The user has to look at the
case again and decide. Conflicts need two users building rules for the same comment on overlapping cases within
the same minutes, so the lost work is rare and small.

Cornerstones are handled the same way. At commit the conflicting set is recomputed against the current
cornerstones and tree; if it contains a case the user was never shown (one that was not in the session's original
set), the commit is rejected and the session cancelled with a message saying the cornerstones changed. A case that *was*
in the original set and is still conflicting was reviewed (or exempted) and is fine; one that has dropped out
is fine too. Recomputing is a second `RuleBuildingSession` constructed with the same case, action and conditions
against `kb.allCornerstoneCases()` now; its `cornerstoneCases()` is compared with the set the original session found
conflicting when it started (`RuleBuildingSession.namesOfConflictingCornerstonesAtStart`, which includes cases later
exempted or excluded by a condition — all of which the user considered). This reuses the existing conflict logic
rather than duplicating it. Comparison is by case name, since cornerstones from a store are fresh instances.

Both checks run inside `commitCurrentRuleSession`, which already runs under `KBSession.locked` at every entry, so
nothing can change between the check and `session.commit()`.

## Surfaces

- **Exception.** `StaleRuleSessionException(message)` in `common` (`io.rippledown.model`, beside
  `KnowledgeBaseHeldException`) — the client needs the type. Two messages, both constants in `common`:
  `"The interpretation of <case> changed while you were building this rule. The rule session has been cancelled;
  please look at the case again."` and the same with "The cornerstones changed …".
- **REST.** `StatusPages` maps it to `409 Conflict`, like `ProjectHeldException`. `Api`'s response validator
  currently turns *every* 409 into `KnowledgeBaseHeldException`; it needs to tell the two apart. The body stays
  plain text; the distinction is a response header `X-Refusal: held | stale`, read by the validator. No existing
  client handling changes for `held`.
- **Chat.** `ChatManager.response` already catches `ProjectHeldException` and answers with a sentence; it does the
  same for `StaleRuleSessionException`, answering with the exception's message. `LeasedRuleService` needs no
  change — the exception propagates from the delegate.
- **Push.** The cancelled session's owner is told via the existing `RULE_SESSION_COMPLETED` push, as a lease loss
  does today (`KBSession.leaseLostBy`), so the client drops its session state and re-fetches the case. Over REST the
  409 is the user's own request failing, so the client also has the message to show.

## Steps

In order; each keeps the suites green.

1. **Done. `StaleRuleSessionException` and messages** in `common`. Serialisation is not needed (plain-text body).
2. **Done. Applicability check** in `RuleSessionManager.commitCurrentRuleSession`: before `session.commit()`,
   `if (!session.action.isApplicable(kb.ruleTree, session.case))` cancel the session, push `RULE_SESSION_COMPLETED`
   to the session's user, throw. Unit tests on two `RuleSessionManager`s from one `KBSession` (alice and bob) in
   `StaleRuleSessionTest`: bob's addition of the same comment makes alice's addition stale; bob's removal makes
   alice's removal stale; bob's unrelated rule leaves alice's commit untouched; the KB has no half-added rule after
   a rejection.
3. **Done. Cornerstone recheck**, same place: a new conflicting cornerstone that alice never saw rejects her commit;
   a cornerstone she exempted does not; a cornerstone that stopped conflicting does not.
4. **Done. REST.** `StatusPages` handler (`LeaseRefusals.kt`) and the `X-Refusal` header (constants `REFUSAL_HEADER`,
   `REFUSAL_HELD`, `REFUSAL_STALE` in `common`); `Api` validator throws `StaleRuleSessionException` for `stale`.
   `StaleCommitRefusalTest` on the server, `ApiTest` with the mock engine on the client.
5. **Done. Chat.** `ChatManager` catches and answers with the exception's message. Test alongside the existing
   refusal test.
6. **Acceptance.** This cannot be shown end to end while the lease still admits one editor per KB, so the scenario
   waits for the lease lift. The planned `KBEndpoint` two-user test was not written: `StaleRuleSessionTest` already
   drives two users through one `KBSession`, and `KBEndpoint`'s per-user delegation and the route's 409 are each
   tested on their own, so it would have pinned nothing new.

The GUI never commits over REST (commits go through the chat), so the only client-side handling needed is the
`Api` validator; the existing `moveAttribute` catch for `KnowledgeBaseHeldException` is unaffected.

## Out of scope

- Lifting the lease — next increment. It becomes: delete the `hold` calls, keep `ProjectLease` for nothing, and
  write the two-editor acceptance scenarios, including the stale-commit one from step 6.
- Broadcast invalidation ("KB changed" push to everyone with the KB open).
- Keeping the session alive through a stale commit.
