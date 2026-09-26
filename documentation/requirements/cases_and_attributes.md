# Cases and attributes

## Concepts

| Term            | Meaning                                                                                                    |
|-----------------|------------------------------------------------------------------------------------------------------------|
| Attribute       | A named data item an expert reasons about: a test such as TSH, a demographic such as Age, a clinical note. |
| Test result     | A value for an attribute, with optional units and reference range.                                         |
| Episode         | The test results for one date. A case has one or more episodes, ordered by date.                           |
| Case            | A named collection of episodes, plus the interpretation the rules gave it.                                 |
| Reference range | The interval within which a value is considered normal for that patient.                                   |

## Attributes

Attributes are identified by id, not name, so a knowledge base can rename what it owns without disturbing the rules and
conditions that refer to it.

Every attribute has a kind, which decides where its values come from and where the case view shows them:

| Kind       | Values come from                     | Shown in                                                                         |
|------------|--------------------------------------|----------------------------------------------------------------------------------|
| `EXTERNAL` | the system that sends the case       | the case data table                                                              |
| `DERIVED`  | the rules, as a literal or a formula | the Derived attributes panel; see [derived_attributes.md](derived_attributes.md) |
| `COMMENT`  | the rules, as a text                 | the Comments panel; see [comments.md](comments.md)                               |

| Requirement            | Description                                                                                                                                                                                                                 | Validation                         |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------|
| Name                   | An attribute has a non-blank name of at most 255 characters.                                                                                                                                                                | `AttributeTest`                    |
| Unique names           | No two attributes in a knowledge base have the same name. External names are compared exactly; a derived or comment name is refused if it matches any name ignoring case, because the user types these names into the chat. | `AttributeManagerTest`             |
| Rename                 | A derived or comment attribute can be renamed through the chat; the rules, conditions and values that refer to it are unaffected. A name in use is refused.                                                                 | `chat/Naming and renaming.feature` |
| External not renamable | An external attribute keeps the name the sending system gave it. **Renaming external attributes is not implemented.**                                                                                                       |                                    |
| External name clash    | External data arriving under the name of a derived attribute is stored under `<name> (external)`, never dropped and never written into the derived attribute.                                                               | `KBExternalCollisionTest`          |

## The case view

| Requirement          | Description                                                                                                                                                                                                                                                                                                                                                                               | Validation                                                                  |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| Case table           | One row per external attribute with data in the case, one column per episode date, units beside the value, the latest reference range in the last column.                                                                                                                                                                                                                                 | `cases/Case View.feature`                                                   |
| Out-of-range marking | A value outside its reference range is marked.                                                                                                                                                                                                                                                                                                                                            | `cases/Case View.feature`                                                   |
| Attribute order      | Rows follow the knowledge base's attribute order. The initial order is the order in which attributes were first seen; the user can reorder by dragging a row or by asking the chat.                                                                                                                                                                                                       | `attributes/Attribute ordering.feature`, `chat/Move attributes.feature`     |
| Attribute filter     | A filter field above the case panels narrows the rows to attributes whose name, values, units or reference range contain the text. One filter applies to the current case and the cornerstone shown beside it, so that the two can be compared row for row. Dragging is disabled while a filter is active, since reordering a subset is ambiguous. Escape or the clear button empties it. | `attributes/Attribute filtering.feature`                                    |
| Panels               | Below the table are collapsible **Derived attributes**, **Comments** and **Report** panels, name columns aligned with the table's attribute column so that the case reads as one thing. The Derived attributes panel is shown even when empty, so that the user learns the facility exists.                                                                                               | `inferencing/Derived attribute.feature`, `kb/Interpretation review.feature` |
| Conditions on hover  | Hovering a comment or derived value shows the conditions of the rule that gave it, in the user's words with the formal text beneath.                                                                                                                                                                                                                                                      | `rulebuilding/Show conditions for rule action.feature`                      |
| Pending changes      | While a rule is being built the panels preview what it will do. See [design/previewing_pending_changes.md](../design/previewing_pending_changes.md).                                                                                                                                                                                                                                      | `rulebuilding/*.feature`                                                    |

## Case lists

A knowledge base keeps three lists of cases, shown in sections of the case list panel.

| List         | Purpose                                                                                                                                                                                                                                                                                                               | Validation                               |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------|
| Processed    | Cases sent for interpretation, kept so that the user can review them and build rules.                                                                                                                                                                                                                                 | `cases/Case list.feature`                |
| Cornerstones | Copies of the cases rules were built on, used to test the effect of every later rule. A case used for several rules is stored once; cornerstones cannot be deleted.                                                                                                                                                   | `rulebuilding/Cornerstone cases.feature` |
| Favourites   | Cases the user has chosen to keep to hand. Only the *current* case can be copied to favourites, because case names are not unique and the current case is the only one the user can point at unambiguously. A favourite can be given a new name when copied and can be deleted. Favourites are included in an export. | `cases/Favourite cases.feature`          |

**Not implemented:** editing a favourite case through the chat (to assess how different values would be interpreted);
renaming a favourite after it has been copied; a search-results list.

| Requirement    | Description                                                                                    | Validation                                |
|----------------|------------------------------------------------------------------------------------------------|-------------------------------------------|
| Navigation     | The user selects a case by clicking it or with the arrow keys; the view follows the selection. | `cases/Navigate cases.feature`            |
| Live case list | A case arriving through the REST interface appears in the list without a refresh.              | `interpreter/Interpreter Service.feature` |
| Deletion       | A processed or favourite case can be deleted; deleting a copy leaves the original alone.       | `cases/Favourite cases.feature`           |
