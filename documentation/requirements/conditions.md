# Condition Requirements
`Conditions`s are objects that evaluate as `true` or `false` for a case.

Typically, a `Condition` contains one or more `Attribute`s which are used as
keys to extract a value from the case being evaluated.

When a `Condition` is created or restored from a persistent store,
the `Attribute`s within it are aligned with those in the `AttributeManager`,
to ensure the single-instance requirement for objects in the KB object tree.


| Requirement           | Description                                                                                      | Validation |
|-----------------------|--------------------------------------------------------------------------------------------------|------------|
| `Condition` ids       | Each `Condition` has an id, which is an integer.                                                 | Cond-1     |
| `Attribute` alignment | The `Attribute`s within a `Condition` are set to be the same as those in the `AttributeManager`. | Cond-2     |
