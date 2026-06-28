# Conclusion Requirements
`Conclusion`s are textual comments added to interpretations by rules.


| Requirement            | Description                                                                                                           | Validation |
|------------------------|-----------------------------------------------------------------------------------------------------------------------|------------|
| `Conclusion` ids       | Each `Conclusion` has an id, which is an integer.                                                                     | Conc-1     |
| Text not blank         | The text of a `Conclusion` cannot be blank.                                                                           | Conc-2     |
| Maximum length of text | There can be at most 2,048 characters in the text of a `Conclusion`.                                                  | Conc-3     |
| Conclusion alignment   | The `Conclusion`s  in `Rule`s are aligned with those in the manager.                                                  | Conc-4     |
| Comment variables      | A `Conclusion`'s text may contain variables that are replaced with case attribute values when the report is rendered. | Conc-5     |

## Inserting variables into a comment

A comment can include the current value of a case attribute by inserting that attribute's name delimited with
braces, e.g. `{TSH}`. When the interpretive report is rendered for a case, each such variable is replaced with the
attribute's latest value in that case.

For example, the comment text:

> Your patient has a TSH of `{TSH}` mIU/L.

renders, for a case whose latest `TSH` value is `8.2`, as:

> Your patient has a TSH of 8.2 mIU/L.

| Requirement             | Description                                                                                                | Validation |
|-------------------------|------------------------------------------------------------------------------------------------------------|------------|
| Variable syntax         | A variable is written in a comment as an attribute name delimited with braces, e.g. `{TSH}`.               | Conc-6     |
| Variable binding        | Each variable is bound to a case attribute; binding is by order of appearance in the comment text.         | Conc-7     |
| Value substitution      | When rendering a report, each variable is replaced with the latest value of its attribute for that case.   | Conc-8     |
| Missing value marker    | If the attribute has no value (or no value for the case), a visible marker is shown in place of the value. | Conc-9     |
| Plain comment unchanged | A comment with no variables is rendered exactly as written.                                                | Conc-10    |
