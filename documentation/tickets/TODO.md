# Backlog

Open work only. Implemented behaviour belongs in requirements/design; completed plans and transient run notes
are retained in Git history.

## Attributes and case lists

- Create external attributes and edit case values through chat for hypothetical scenarios.
- Rename external attributes with a persisted alias mapping; show that mapping.
- Delete derived attributes and list all derived attributes independently of the selected case.
- Show the impact of a global definition edit, such as affected cornerstones.
- Make significant figures configurable and support derived values for earlier episodes.
- Edit or rename an existing user-list case, and rename a list. See
  [case-list requirements](../requirements/user_defined_case_lists.md).

- Fix cancellation of an ordinary literal/formula assignment leaving an unused derived attribute whose name
  is then refused by chat. AI assignment names already remain provisional until commit.

## Attribute ordering

- Persist external-attribute reorder operations. `OrderedEntityManager` currently loads stored order but its
  mutations only update memory; export captures the current order.
- Add persisted user-controlled ordering to comments and derived-value panels. Comments currently sort by
  attribute ID and derived values by name.
- Reuse the shared attribute order where appropriate, with bulk upsert persistence, stable IDs for draggable rows,
  and restart/export round-trip coverage. Pending rows and cornerstone views must not be reorderable.

## AI-derived attributes

The [core feature is implemented](../design/ai_derived_attributes.md). Outstanding review and coverage items
carried forward from its implementation plan are:

- Audit mutation persistence failures and large cornerstone reviews; expose progress for lengthy evaluations.
- Extend live authoring coverage for ambiguous/incomplete requests and long cornerstone conversations.
- Review disagreements from the opt-in synthetic model-quality evaluation before adopting a profile. A mocked
  classifier or binding-only dry run cannot establish model accuracy.
- Record current desktop, Postgres and live-model validation when scheduled; old plan/run notes are not evidence
  that the current revision has passed those checks.

## Comments and rule building

- List all comments, enforce the desired nonblank/maximum-length text constraint, and distinguish ancestor rules
  from the leaf rule in condition tooltips.
- Group comments (for example GP versus specialist advice), allowing a rule to suppress a whole group.
- Help for rule building and individual topics.
- Acceptance coverage for cancelling when no rule is active.
- Inline cornerstone Allow/Don't allow choices, within [UI guidelines](../design/chat_ui_guidelines.md).
- Restriction clauses and time-between-episodes conditions; see [conditions](../design/conditions.md).
- Consider whether each rule should start a new conversation.

## Reports, chat and awareness

- Send comment names as well as texts to report generation; let users choose which attributes are sent.
- Show/hide the Report panel through chat.
- Revisit report-cache invalidation when primary data changes without a comment-text change.
- Stream model replies and support live voice transcription.
- Remember chat-panel width and improve its focus indication.
- Supply updated case/change context on the next model turn after a remote edit without restarting the conversation.
- Make mutation and change-log persistence atomic; currently a logging failure leaves the mutation applied.
- Coalesce burst change notifications if repeated case refresh becomes costly.
- Convert remaining chat-based acceptance setup to deterministic backdoor setup where setup is not the behaviour
  being tested; retain real chat for the action under test.

## Infrastructure

- Run the appropriate test suites in CI on each push, accounting for desktop, database and model dependencies.
- Convert to multiplatform.
- Allow downloading a client application from the server.
