# Comment naming and attribute ordering

This document records two related changes:

1. A comment's name is assigned by the server (`C1`, `C2`, …), not proposed by the model. The user can rename it
   afterwards.
2. Comment and derived-attribute ordering should be persisted and user-changeable by drag and drop, as external
   attribute ordering is in the current session.

**Status:** change 1, predictable server-assigned comment names, is implemented. Change 2, persisted user-controlled
ordering, is **not implemented**. Comments are currently sorted by attribute id, derived values by attribute name, and
neither panel offers reordering. The implementation sections below remain the plan for change 2 and a historical record
for change 1.

## A finding that change 2 depends on

External attribute ordering is **not** in fact persisted today.
`OrderedEntityManager` takes `orderStore` as a constructor parameter, reads
`idToIndex()` in its `init`, and never calls `store()`
(`server/src/main/kotlin/io/rippledown/kb/OrderedEntityManager.kt`).
`OrderStore.store(id, index)` has no production caller anywhere; the only writer is `load()`, from `KBImporter`. So a
drag-and-drop reorder of external attributes survives only until the server restarts. Change 2 therefore has to add
persistence, not merely extend it.

---

# Change 1 — the server names comments

## Former flow

`19_naming_and_renaming.md` tells the model to emit an `attributeName` field with `{{ADD_COMMENT}}` and
`{{REPLACE_COMMENT}}`. `ActionComment.attributeName`
is reflectively bound to the `attributeName` parameter of `AddComment` /
`ReplaceComment`, which pass it as `proposedAttributeName` to
`RuleService.startRuleSessionTo{Add,Replace}Comment`, thence to
`RuleSessionManager.commentAttributeFor(template, proposedName)` and
`AttributeManager.createCommentAttribute(proposedName)`, which validates the proposal and otherwise falls back to `C1`,
`C2`, ….

## Steps

Bottom-up, keeping the build green after each.

### 1.1 `AttributeManager`

- `createCommentAttribute()` loses its parameter; the body reduces to the smallest-unused-`Cn` search plus
  `getOrCreate(name, COMMENT)`.
- Delete `isUsableProposedName` and the `MAX_PROPOSED_ATTRIBUTE_NAME_LENGTH`
  constant. Keep `rename` unchanged; it checks for a conflicting name directly.

### 1.2 `RuleSessionManager`

Drop `proposedAttributeName` from the two overrides, the two `internal`
overloads and `commentAttributeFor`. Behaviour is otherwise unchanged:
reuse of the attribute of an existing template still wins over creation.

### 1.3 `RuleService`

Drop the parameter from both signatures. Keep
`nameOfCommentAttributeInSession()`: the user still needs to be told the name that was assigned.

### 1.4 Chat actions

- Remove `attributeName` from `AddComment` and `ReplaceComment`.
- Leave `ActionComment.attributeName` in place: it is still needed by
  `RenameAttribute`, `AssignDerivedValue`, `RemoveDerivedValue`,
  `ReplaceDerivedValue` and `EditDerivedAttributeDefinition`. Reflection binds by parameter name, so an `attributeName`
  emitted by an old prompt is simply ignored; no error path is needed.
- Keep `ChatResponse.withCommentName` and `commentNamedMessage`.

### 1.5 Instructions

In `server/src/main/resources/chat/instructions/19_naming_and_renaming.md`, delete the whole "Proposing a name for a
comment" section — heading, rules, example table and JSON example. Replace it with a paragraph saying that every comment
is named by the system (`C1`, `C2`, …), that the system tells the user the name, and that the model must never invent or
state one. Retain the renaming section verbatim.

Then check the other instruction files for `attributeName` in a comment context: `3_defining_the_report_change.md` and
`4_comment_variables.md`. The
`attributeName` of `4_comment_variables.md` names a *variable's* attribute and stays.

### 1.6 Tests

All of these shrink.

- `AttributeManagerTest`: delete the proposed-name cases; keep and extend the
  `C1`, `C2`, smallest-unused and case-insensitive cases.
- `kb/chat/action/AddCommentTest`, `ReplaceCommentTest`: drop the proposed-name assertions and mockk argument matchers.
- `RuleSessionManagerCommentAssignmentTest`,
  `RuleSessionManagerPendingChangeTest`: signature updates only.
- Cucumber `requirements/chat/Naming and renaming.feature`: remove any scenario asserting a semantically proposed name;
  keep "a new comment is named C1 and the panel shows it" and the renaming scenarios; add a scenario in which two
  comments added in one session are named `C1` then `C2`.

### 1.7 Design doc

`repeat_inferencing.md` records the implemented decision: names are predictable and do not consume model attention or
latency; renaming covers the semantic case.

**Risk**: low. It is a pure deletion. The only behaviour lost is semantic auto-naming, which the rename action already
provides.

---

# Change 2 — persisted, user-controlled ordering

## Design

**One global attribute order, for attributes of every kind, in the existing
`attribute_indexes` table.**

- The three panels are disjoint by kind — the case table shows `EXTERNAL`
  (`CaseTableBody`), the Derived attributes panel `DERIVED`, the Comments panel
  `COMMENT` — so a single index space needs no partitioning: each panel sorts its own members by the shared index.
- `CaseViewManager` already accumulates comment and derived attributes, because they materialise onto the case and
  `getViewableCase` calls
  `inOrder(case.attributes)`, whose `getOrCreate` appends unknown entities. The ordering already exists in memory; it is
  only never persisted and never used to sort the two panels.
- No new table, so **no one-off migration SQL to run**. A separate order store per kind would need DDL.

`viewProperties.attributes`, shipped in every `ViewableCase`, is exactly that ordered list and already contains the
derived and comment attributes, so the client needs no new payload in order to sort — only attribute ids on the rows, so
that it can ask for a move.

## 2A — Make the order durable (server)

- **`OrderStore`**: add `fun store(idToIndex: Map<Int, Int>)`, for a whole-order write in one transaction, and redefine
  `store(id, index)` as an upsert.
- **`PostgresAttributeOrderStore.store`** does `PGAttributeIndex.new(id)`, which throws on an existing primary key — it
  can only ever have worked against a virgin store. Change it to
  `findById(id)?.apply { attributeIndex = index } ?: new(id) { … }`, and implement the bulk overload within a single
  `transaction`. The schema is unchanged.
- **`InMemoryOrderStore`**: trivial bulk overload.
- **`OrderedEntityManager`**: keep `orderStore` as a property; add an
  `idOf: (T) -> Int` constructor parameter (`CaseViewManager` passes
  `Attribute::id`); call a new `persist()`, which writes the whole
  `entityToIndex` map, at the end of `set`, `move`, `moveJustAbove`,
  `moveJustBelow`, `insert` and `create`. A full rewrite per move is O (N) for N in the hundreds; do it in one
  transaction and do not optimise.
    - Worth considering while there: `OrderedEntityManager<T>` is now only ever instantiated for `Attribute`, since
      `InterpretationViewManager` stopped extending it in commit `7a97207e`. Collapsing the generic into
      `CaseViewManager` would remove the `EntityProvider` indirection and the
      `idOf` lambda. Optional, and a separate commit if done.
- **Register a KB-assigned attribute when it is created**, so that it has a durable position before the case is
  re-fetched, and so that the position is deterministically last. `KB` owns both managers, so the wiring that avoids a
  cycle is a new `KB.registerNewKbAssignedAttribute(attribute)` doing
  `caseViewManager.insert(listOf(attribute))`, called from
  `RuleSessionManager.commentAttributeFor` and from the derived-attribute creation path. Lazy insertion via `inOrder`
  remains as the fallback.
- **Tests**: `OrderedEntityManagerTest` / `CaseViewManagerTest` gain "a move is written through to the store" and "a
  manager rebuilt from the store has the moved order"; `InMemoryOrderStoreTest` and `PostgresAttributeOrderStoreTest`
  gain upsert and bulk cases; `PostgresKBTest` gains a reload-after-move case (needs a local Postgres).

## 2B — Sort the two panels by the persisted order

- **Comments**: `InterpretationViewManager.commentAssignments` sorts by
  `attribute.id`. Give the class an `attributeOrder: () -> List<Attribute>`, wired in `KB` to
  `caseViewManager::allInOrder`, and sort by index in that list, unknown attributes last, with id as the tie-break.
  Update the class KDoc: ordering is significant again.
- **Derived values**: `ViewableCase.derivedValues()` sorts by name. Sort by
  `viewProperties.attributes.indexOf(assignment.attribute)` instead. This is server-supplied order, so there is no
  protocol change.
- **Pending previews**: `CommentRows.kt` documents that an addition is appended
  "because a new comment attribute has the highest id". That remains true under the new rule, since a new comment
  attribute is registered last, but the comment should say "because a new comment attribute is registered last in the
  attribute order".

## 2C — Carry attribute ids to the client

To issue `api.moveAttribute(movedId, targetId)` the rows need ids.

- Add `attributeId: Int = 0` to `RenderedComment`, populated by
  `InterpretationViewManager`, and to `DerivedValueInfo`, populated by
  `ViewableCase.derivedValues()`. Both are defaulted, so serialization stays backwards compatible.
- A row previewing a pending addition has no committed attribute, so it keeps id `0`, and `0` means "not draggable".

## 2D — Drag and drop in the two panels (UI)

- **Extract the reorder wiring** that is inline in `CaseTableBody` into a reusable composable,
  `ui/src/main/kotlin/io/rippledown/dragdrop/ReorderableColumn.kt`, parameterised by the items, a key, an
  `onMoved(moved, target)` callback and a
  `row(item, displacementOffset, boundsModifier)` slot. `DragDropState` itself needs no change. Refactor `CaseTableBody`
  onto it first, with its existing tests green: that de-risks the rest.
- **Comments panel**: `ReadonlyInterpretationView` draws
  `rows.forEach { CommentRow(…) }`. Wrap that in `ReorderableColumn`, disabled when `idPrefix` is the cornerstone's (a
  cornerstone view must not reorder)
  and for any row whose `attributeId` is `0`. `InterpretationViewHandler` gains
  `moveAttribute(movedId: Int, targetId: Int)`.
- **Derived panel**: the same treatment for the
  `rows.forEach { DerivedValueRow(…) }` loop of `DerivedValuesPanel`.
- **Plumbing**: `CaseInspection` already passes the handler to both panels, and
  `OpenRDRUI.swapAttributes` is `api.moveAttribute` followed by an
  `api.getCase` refetch, so it can be reused verbatim for both panels. The refetch is what makes the new order appear.
- **UI tests**: extend `ReadonlyInterpretationViewTest` and
  `DerivedValuesPanelTest` with "rows are drawn in the order supplied" and
  "moving row 1 below row 2 calls the handler with the two attribute ids", mirroring the existing case-table drag
  testsF.

## 2E — Export, import and docs

- `CaseViewExporter` already writes `caseViewManager.allInOrder()`, which will now include comment and derived
  attributes, and `KBImporter` resolves ids against the full attributes file, so it should work as it stands. Add a
  round-trip test asserting that a comment attribute's position survives export and import.
- Update `repeat_inferencing.md` to record that comment and derived-attribute ordering is user-controlled and persisted,
  and extend the Comments panel description to mention drag and drop.
- No SQL to run, given that `attribute_indexes` is reused.

---

# Commit sequence

1. `OrderStore` upsert and bulk write, both implementations, store tests. No behaviour change.
2. `OrderedEntityManager` persists on mutation; KB-assigned attributes are registered at creation; manager and KB tests.
3. Comments and derived values sorted by the persisted order; server and common tests; the design-doc decision reversal.
4. `attributeId` on `RenderedComment` and `DerivedValueInfo`.
5. Extract `ReorderableColumn`; refactor `CaseTableBody` onto it, with no functional change.
6. Drag and drop in the Comments panel; UI tests.
7. Drag and drop in the Derived attributes panel; UI tests.
8. Cucumber: reorder a comment, restart the server, assert the order held; the same for a derived attribute.
9. Change 1, in full. It is independent of steps 1–8 and can be done first.
