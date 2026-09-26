# Backlog

Open work only. Items that are done are removed, not ticked; the requirements docs record what exists.

**External attributes**

- Let the user create an external attribute and set its value, to build a case scenario to build rules on.
- Let the user rename an external attribute (needs an alias table; see `design/rule_tree_and_inference.md`).
- Show the external name → name mapping and the list of all external attributes.

**Derived attributes**

- Delete a derived attribute.
- List all derived attributes, whether or not the case has them.
- Impact summary (e.g. count of affected cornerstones) when a definition is edited globally.
- Configurable significant figures, instead of the hard-coded 4.
- Derived values for episodes other than the latest.

**Comments**

- List all comments.
- Persisted, user-controlled ordering of comments and derived values: `attribute_ordering_plan.md`.
- Show ancestor rules differently from the leaf rule in the conditions tooltip.
- Grouping comments (e.g. GP vs specialist) so that one rule can remove a whole group. Motivating example: many
  prescriptive comments drive a detailed GP report, but a referring specialist should get a single "Specialist
  management noted" comment; today that is one removal rule per comment. Options: a group tag on `COMMENT` attributes
  with a bulk-remove action, or a rule-given "report scope" honoured by the report generator.

**Rule building**

- Help with building a rule, and help on a specific topic generally (system prompt vs a help document behind a tool).
- Cuke for cancelling when no rule is in progress.
- Inline "Allow" / "Don't allow" for a cornerstone (passes the test in `design/chat_ui_guidelines.md`).
- Restriction clauses and time-between-episodes conditions (`design/conditions.md`).
- Consider whether each rule should start a new conversation.

**Report**

- Send the comments' names as well as their texts to the model.
- Show or hide the report panel from the chat.

**Chat panel and voice**

- Stream the model's reply.
- Live transcription while speaking (Speech-to-Text with phrase hints; `design/voice_input.md`).
- Remember the user-set width of the chat panel.
- Flash the chat panel when it receives focus.

**Infrastructure**

- Run all tests in GitHub Actions on each push.
- Convert to multiplatform.

**Bugs**

- Start to build a rule assigning derived attribute BMI, then cancel: the next attempt is refused with "BMI is already
  in use". The cancelled session should not leave the attribute behind.
