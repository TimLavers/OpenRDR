# Case View
The `CaseView` displays the data and interpretation for a case to the user.

## Case data
The data is shown in a table in which:
- there is a row for each attribute having data in the case
- there is a column for each date at which one or more test results were given.

Only the data that came with the case is shown in this table. What the knowledge base worked out for the case — its
derived values and its comments — is shown in the panels below it, each attribute with its name. See
[attributes.md](attributes.md).

| Requirement     | Description                                                                                    | Validation |
|-----------------|------------------------------------------------------------------------------------------------|------------|
| Case name       | The name of the case is shown as a heading above the case table.                               |            |
| Case rows       | There is a row for each external attribute that has data in the case.                          |            |
| Units           | The units for a test result are shown alongside the value.                                     |            |
| Reference range | If there is a reference range for the most recent test result, it is shown in the last column. |            |

## Panels below the case data

Under the case table are collapsible panels, each showing what the rules gave this case. They line their name column up
with the attribute column of the case table, so that the case reads as one thing.

| Panel              | Shows                                                            |
|--------------------|------------------------------------------------------------------|
| Derived attributes | a row for each derived value: its attribute's name and the value |
| Comments           | a row for each comment: its attribute's name and the comment     |
| Report             | the report for the case                                          |

| Requirement         | Description                                                                                                                                                            | Validation |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| Always shown        | The Derived attributes panel is shown even when the case has none, so that the user knows of the facility.                                                             |            |
| Empty state         | A panel with nothing to show says so.                                                                                                                                  |            |
| Conditions on hover | Hovering over a row shows the conditions of the rule that gave it.                                                                                                     |            |
| Pending changes     | While a rule is being built, the panel previews what it will do. See [previewing pending changes](../design/previewing_pending_changes_when_a_rule_is_being_built.md). |            |

## Order of attributes
There is an ordering of attributes, which is used to order the rows of the case table.
The attributes can be ordered by dragging them within the case table. The initial ordering
is the order in which the attributes are first encountered in the cases presented; new
attributes are added relative to existing ones so as to preserve that order where possible.
