# Suggested conditions - user interface

When a user is building a rule, they need to add conditions to the rule
that are true for the session case but false for cases that should not be
affected by the rule. Selecting or building conditions can be difficult for
users because it involves them translating their knowledge into mathematical
formulas. 

In [Building conditions from user hints](building_conditions_from_user_hints.md)
we discuss graphical- and text-based approaches to condition editing.
In [Suggested conditions — non-targeted generation](non_targeted_suggested_conditions.md)
we look at ways of generating lists of candidate conditions.
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

## `SuggestedCondition` interface

The sealed interface `SuggestedCondition` handles these situations. Two
implementations exist: `NonEditableSuggestedCondition` for fixed
suggestions and `EditableSuggestedCondition` wrapping an
`EditableCondition`.

| Method                           | Return value         | Notes                                                                      |
|----------------------------------|----------------------|----------------------------------------------------------------------------|
| `initialSuggestion()`            | `Condition`          | The suggestion shown to the user.                                          |
| `isEditable()`                   | `Boolean`            | Whether adjustments can be made to the suggestion.                         |
| `editableCondition()`            | `EditableCondition?` | Used to construct an editor popup, if not null. Null if not editable.      |
| `shouldBeSuggestedForCase(case)` | `Boolean`            | Whether the suggestion is offered for the given case (filters falsehoods). |

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

The actual `EditableCondition` interface (in
`common/src/main/kotlin/io/rippledown/model/condition/edit/EditableCondition.kt`):

| Method                                     | Return type     | Notes                                                                          |
|--------------------------------------------|-----------------|--------------------------------------------------------------------------------|
| `fixedTextPart1()`                         | `String`        | The uneditable text shown before the editable value.                           |
| `fixedTextPart2()`                         | `String`        | The uneditable text shown after the editable value (often blank).              |
| `editableValue()`                          | `EditableValue` | The current value plus its `Type` (`Text`, `Integer`, `Real`).                 |
| `condition(value)`                         | `Condition`     | The condition corresponding to a given value of the parameter.                 |
| `prerequisite()`                           | `Condition`     | A gate that must hold before the suggestion is offered (defaults to `True`).   |
| `shouldBeUsedAtMostOncePerRule()`          | `Boolean`       | Whether the editor allows adding multiple instances to one rule.               |
| `initialValueRepresentsHoldingCondition()` | `Boolean`       | When false, the auto-populated value is treated as a placeholder to be edited. |

