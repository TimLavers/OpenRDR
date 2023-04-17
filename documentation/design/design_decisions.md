# Design Decisions

This document discusses design choices that have been made and which
we might want to revisit at some point.

## Not using `BigDecimal`

The Java `BigDecimal` class would have been our preferred option
for implementing `Value`s and `ReferenceRange`s. However, this
class is not available in the common realm.
See  [Treatment of real numbers](treatment_of_real_numbers.md).

## Calculating rule actions from changes that a user has made to the interpretative report

When a user changes the text of an interpretation, and wants to add a rule to justify this change, the backend needs to
know what rule action, or actions, this corresponds to.
See [calculating the rule actions from the changed report](calculating_rule_actions.md).
