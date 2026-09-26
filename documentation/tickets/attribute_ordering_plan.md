# Plan: persisted, user-controlled ordering of comments and derived attributes

**Status: not implemented.** Comments are sorted by attribute id, derived values by name, and neither panel offers
reordering.

## A finding the plan depends on

External attribute ordering is not persisted either. `OrderedEntityManager` reads `orderStore.idToIndex()` in its `init`
and never calls `store()`; `OrderStore.store` has no production caller except `KBImporter.load`. A drag-and-drop reorder
survives only until the server restarts, so this work must add persistence, not extend it.

## Design

One global attribute order for attributes of every kind, in the existing `attribute_indexes` table. The three panels are
disjoint by kind, so a single index space needs no partitioning, and reusing the table means no migration SQL.
`CaseViewManager` already accumulates comment and derived attributes (they materialise onto the case), and
`viewProperties.attributes` already carries the order to the client; what is missing is persistence and using the order
to sort the two panels.

## Steps

1. `OrderStore`: bulk `store(Map<Int, Int>)` in one transaction; `store(id, index)` becomes an upsert
   (`PostgresAttributeOrderStore.store` currently `new`s and would throw on an existing key).
2. `OrderedEntityManager` persists the whole map after every mutation. Register a KB-assigned attribute in the order
   when
   it is created, so its position is durable and deterministically last. Consider collapsing the generic into
   `CaseViewManager`, its only instantiation.
3. Sort comments (`InterpretationViewManager`) and derived values (`ViewableCase.derivedValues()`) by the persisted
   order, unknown attributes last, id as tie-break.
4. Add `attributeId` (default 0, meaning "pending row, not draggable") to `RenderedComment` and `DerivedValueInfo`.
5. Extract the drag-and-drop wiring in `CaseTableBody` into a reusable `ReorderableColumn`; refactor the case table onto
   it first with its tests green.
6. Drag and drop in the Comments panel (disabled for the cornerstone view and pending rows), then the Derived attributes
   panel, both calling the existing `moveAttribute` API and refetching the case.
7. Export/import: `CaseViewExporter` writes `allInOrder()`, which will now include the new kinds; add a round-trip test.
8. Cucumber: reorder a comment, restart the server, assert the order held; likewise a derived attribute.
9. Update `design/rule_tree_and_inference.md` and `requirements/comments.md`.
