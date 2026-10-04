# Concurrent users — per-user rule sessions

Second increment of Stage 3 in [concurrent_users.md](concurrent_users.md). Builds on the per-KB lock in
[concurrent_users_write_lock.md](concurrent_users_write_lock.md). On its own it changes nothing a user can see:
the Stage 2 lease still admits one editor per KB. It is the structural step that lets the lease be lifted once
commit-time revalidation exists.

## Where the session state is

`RuleSessionManager` is one object per KB and holds both kinds of thing:

- **Engine operations** on the shared `KB` — start/commit/undo a rule, cornerstones, condition hints, attribute
  lookups, rename, move. These read and write the KB object graph and are now serialised by the lock.
- **One user's session state** — `ruleSession`, `currentChange`, `commentAttributeInSession`, `diffAttribute`,
  `replacedDiffAttribute`, `selectedCornerstone`, and the translator's conversation (`conditionChatService`) and
  parser.

Every other piece of the server reaches a rule session through exactly two points: `KBEndpoint.ruleSessionManager()`
(REST) and `context.endpoint.session.ruleSessionManager` in `ChatManagerFactory` (chat). Pushes from inside a session
go to `leaseHolder()`.

## Decision: one `RuleSessionManager` per (user, KB), not a class split

The design sketch in `concurrent_users.md` says "`RuleSessionManager` splits in two". Reading the class, the split is
unnecessary: the engine operations are already stateless with respect to the manager — they act on `kb` — and the
session state is a handful of fields. So the per-user unit is simply **a `RuleSessionManager` instance per user**,
all sharing one `KB` and one lock:

- `KBSession` keeps a `ConcurrentHashMap<UserId, RuleSessionManager>` and exposes `ruleSessionManagerFor(userId)`.
  Each instance is built with the same `kb`, `webSocketManager` and `lock`, and with its own user as the push
  addressee (replacing `leaseHolder`).
- KB-wide operations that happen to live on the manager (`undoLastRuleSession`, `descriptionOfMostRecentRule`,
  `renameAttribute`, `moveAttributeTo`, …) act on `kb`, so calling them through any user's instance is correct.
- Per-user by construction: the translator conversation, the cornerstone cursor, the pending change.

This is ~20 lines of change in `KBSession` and a constructor parameter, against a ~1,100-line class split that
would have to re-thread `kb` through every method. The sketch's `RuleBuildingSessionState` is what the instance *is*; it
does not need a new name.

## Resolving the caller's session

- **REST.** `KBEndpoint` is per KB and today has no user. Rule-session methods on it (`startRuleSession`,
  `commitRuleSession`, `cancelRuleSession`, `updateCornerstone`, `selectCornerstone`,
  `exemptCornerstone`, `addConditionToCurrentRuleBuildingSession`, `commitCurrentRuleSession`,
  `conditionForExpression`, `buildRule`, `conditionHintsForCase`, `descriptionOfMostRecentRule`, `undoLastRule`)
  take a `userId` parameter and call `session.ruleSessionManagerFor(userId)`. The routes already read the
  `X-User-Id` header (`RoutingContext.userId()`), so each guarded route passes it. No wire-format change.
- **Chat.** `ChatManagerFactory.create(userId, context)` already has the user; it builds
  `LeasedRuleService(userId, session, session.ruleSessionManagerFor(userId))`.
- **Sample builders and tests** that call rule-session methods on `KBEndpoint` pass a fixed user.

## Pushes

`sendCornerstoneStatus` / `sendRuleSessionCompleted` address the instance's own user instead of the lease holder.
`KBSession.leaseLostBy(user)` cancels that user's session (`ruleSessionManagerFor(user).cancelRuleSession()`), which
is what it does today through the single instance.

## Lifecycle

Entries are created on first use and never removed while the server runs: an instance with no session in progress
is a few fields. `ChatCoordinator.close(userId)` already releases the lease; it also cancels the user's rule
session, which it does today via the lease release. No expiry mechanism is added.

## Still single-editor

The lease is untouched in this increment, so two users still cannot edit at once; this step only ensures that when
they can, each has their own session. Lifting the lease is its own increment, after
commit-time revalidation, because without revalidation user A's session could commit against an interpretation
user B changed.

## Steps

In order; each keeps the suites green.

1. **Done. `RuleSessionManager` takes its user.** `leaseHolder: () -> UserId?` became `userId: UserId?`
   (nullable for the tests that construct one bare). Pushes go to it.
2. **Done. `KBSession.ruleSessionManagerFor(userId)`.** Map of instances; `leaseLostBy` cancels the losing user's
   session. The `ruleSessionManager` property is gone. Tests in `KBSessionTest`: two users get distinct instances
   with independent session state; the same user gets the same instance back; losing the lease cancels only that
   user's session.
3. **Done. `KBEndpoint` rule-session methods take `userId`.** Routes pass `userId()`. `SampleRuleBuilder` passes
   `SAMPLE_BUILDER`; server tests pass `TEST_USER` (in `server/CommentSessions.kt`).
4. **Done. Chat resolves per user.** `ChatManagerFactory` builds `LeasedRuleService` on
   `ruleSessionManagerFor(userId)`; `ApplicationKbService.isRuleSessionActive` reads the user's own instance.
   `ChatCoordinator.close` needed no change: releasing the lease cancels the session via `leaseLostBy`.
5. **Written, not yet run. Acceptance.** `requirements/kb/Concurrent Users.feature`, "A refused editor does not
   disturb the rule session of the user who holds the knowledge base": alice starts a rule session, bob is refused
   by the lease (unchanged), alice commits and the case carries her comment. This pins that the per-user split did
   not change Stage 2 behaviour. The real two-editor scenarios come with the lease lift.

## Out of scope

- Commit-time revalidation and lifting the lease — next increments.
- Broadcast invalidation.
