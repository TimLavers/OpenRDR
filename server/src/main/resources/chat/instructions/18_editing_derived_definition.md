# Editing the definition of a derived attribute

A derived attribute that is calculated or assigned by the knowledge base has a stored *definition* (its formula or
value). The user can edit that definition in place — for example to fix a typo in the BMI formula — so that the
correction applies everywhere the attribute is given by its definition. This does **not** build a rule and there is no
cornerstone review.

## Distinguishing an edit from a replacement rule

This is the crucial routing decision:

- **Edit the definition** — the user wants a *global* correction, with no condition or reason attached:
    - "Fix the BMI formula to weight/ (height\*height)"
    - "BMI should be defined as weight divided by height squared"
    - "Edit the definition of Risk score"
    - "The BMI formula is wrong, it should be ..."

  → Emit `{{EDIT_DERIVED_DEFINITION}}` (see below).

- **Replace with a rule** — the change is *case- or condition-specific*:
    - "For amputees, BMI should be calculated as ..."
    - "When Height is missing, BMI should be ..."
    - "For this patient, Risk score should be 9"

  → Follow "Assigning derived values" and emit `{{REPLACE_DERIVED_VALUE}}`, which starts a rule session with the normal
  reasons and cornerstone flow.

- **Ambiguous** — ask a one-line clarifying question, e.g. "Should the definition change everywhere, or only under a
  condition?"

## Emitting the action

The value expression follows the same quoting rule as for assigning derived values: literal text in double quotes,
numbers and formulas unquoted.

```json
{
  "action": "{{EDIT_DERIVED_DEFINITION}}",
  "attributeName": "Pulse pressure",
  "valueExpression": "systolic - diastolic"
}
```

The expression is transcribed exactly as the user gave it, as in "Assigning derived values", step 3.

The server replies with a summary of the change (e.g. *Changed the definition of "Pulse pressure" from systolic to
systolic - diastolic.*). Relay it to the user. There is no rule session, no reasons to collect, and no
cornerstones to review.

## Handling server refusals

The server may refuse the request. Relay its message verbatim and ask the user to correct the request. Reasons:

- The attribute does not exist, or is not a derived attribute.
- The new definition would create a dependency cycle (e.g. defining BMI as `BMI * 2`).
- A rule session is in progress — the user must finish or cancel it first.
