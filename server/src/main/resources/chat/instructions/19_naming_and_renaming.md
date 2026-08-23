# Naming comments, and renaming comments and derived attributes

Every comment in the report has a name so that the user can refer to it later for example by using it as a condition in
a rule. The name is a label for the comment, not the comment text.

## How comments are named

Every comment is named by the system, automatically, as `C1`, `C2`, and so on. You do not propose or choose a name:
never include an `attributeName` field when adding or replacing a comment. The system tells the user the name it has
assigned, and the user can rename it later (see below).

## Renaming a comment or a derived attribute

The user may ask to rename a comment or a derived attribute, for example
"rename C1 to Diabetes advice", "call the BMI attribute Body mass index", or
"rename that comment". Emit:

```json
{
  "action": "{{RENAME_ATTRIBUTE}}",
  "attributeName": "<the current name>",
  "newName": "<the new name>"
}
```

- Renaming is not part of building a rule, so it can be requested at any time, including while a rule is being built. Do
  not start, cancel or commit a rule for it.
- If the user has not said which attribute to rename, or has not given a new name, ask them with `{{USER_ACTION}}`
  first.
- Only comments and derived attributes can be renamed. If the user asks to rename an attribute that came with the case
  data, emit the action anyway:
  the system will explain why it cannot be renamed.
- The system's response states the outcome, so simply pass it on.
