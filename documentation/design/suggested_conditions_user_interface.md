# Suggested conditions - user interface
When a user is building a rule, they need to add conditions to the rule
that are true for the session case but false for cases that should not be
affected by the rule. Selecting or building conditions can be difficult for
users because it involves them translating their knowledge into mathematical
formulas. 

In [Building conditions from user hints](building_conditions_from_user_hints.md)
we discuss graphical- and text-based approaches to condition editing. 
In [Suggested conditions - version 1](suggested_conditions_version_1.md) we
look at ways of generating lists of candidate conditions.
The present document describes a software design for a system that presents
good suggestions to the user and allows the editing of those that require adjustments.

## Editable suggestions
Some suggested conditions should not be changed by the user. For example
`TSH is high` cannot meaningfully be altered because changing `high` to
`normal` or `low` would result in a condition that is not true for the 
session case, and there is no point in changing `TSH` to another attribute
because all possible conditions for all attributes are presented as
suggestions.

On the other hand, a suggested condition such as `TSH ≥ 0.67` almost
certainly requires editing.

## `SuggestedCondition` class
The class `SuggestedCondition` handles these situations.

| Method             | Return value         | Notes                                                                 |
|--------------------|----------------------|-----------------------------------------------------------------------|
| `initialCondition` | `Condition`          | This is the suggestion shown to the user.                             |
| `editable`         | `Boolean`            | Can adjustments be made to the suggestion.                            |
| `editableCondition`| `EditableCondition?` | Used to construct an editor popup, if not null. Null if not editable. |

## `EditableCondition` class
As shown above, a `SuggestedCondition` for which `editable` returns `true` can provide
an `EditableCondition` which is used to construct a user interface for adjusting the suggestion.
At the time of writing, all of the conditions that can be edited are of the form
`fixed text part1 ___ fixed text part2` where `___` is a variable and `fixed text part2`
may be blank. 

The kind of condition being edited imposes constraints on the types that the variable can represent.
For conditions like `TSH ≥ ___` it must be a double.
For `contains`, it must be text.
For `FT4 is normal or high by at most ___%`, it must be an integer.

Here's a first-cut for `EditableCondition`:

| Method             | Return type    | Notes                                                            |
|--------------------|----------------|------------------------------------------------------------------|
| `fixedText`        | `String`       | the uneditable part of the condition                             |
| `variablePosition` | `Int`          | Where the parameter fits into the fixed text                     |
| `parameterType`    | Class or enum. | What kind of value the variable must be                          |
| `editedCondition`  | `Condition`    | The condition corresponding to the current value of the parameter|

## Which 

