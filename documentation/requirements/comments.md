# Comments

A comment is a sentence the rules add to a case's interpretation. The set of comments given to a case is what the
expert reviews and corrects, and it is the input to the [AI report](ai_report_generation.md).

A comment is an attribute of kind `COMMENT` whose value is the comment's text. Modelling comments as attributes lets a
rule's conditions refer to earlier comments ("the report says the patient is diabetic"), which is what makes
[repeat inferencing](derived_attributes.md) possible without a second mechanism. It also gives every comment a name,
which the Comments panel shows beside it.

| Requirement                              | Description                                                                                                                                                                                                                                    | Validation                                           |
|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| Name                                     | A new comment is named `C1`, `C2`, … by the system, the smallest unused number. The user is told the name and can rename it at any time. Predictable names cost the model nothing and avoid inventing a semantic name the user did not choose. | `chat/Naming and renaming.feature`                   |
| One attribute a text                     | A comment text has one attribute: asking for a comment already in the knowledge base reuses it rather than duplicating it.                                                                                                                     | `rulebuilding/Build rule to add comment.feature`     |
| Text is fixed                            | A comment's text never changes. Changing what a comment says means giving a different comment in its place, so a name stays attached to one wording.                                                                                           | `chat/Replace comment.feature`                       |
| Give                                     | A rule gives a comment to the cases that satisfy its conditions.                                                                                                                                                                               | `rulebuilding/Build rule to add comment.feature`     |
| Remove                                   | A refinement rule retracts a comment for the cases that satisfy its conditions.                                                                                                                                                                | `rulebuilding/Build rule to remove comment.feature`  |
| Replace                                  | A refinement rule gives the replacement and retracts the original; the leaf-most satisfied rule wins.                                                                                                                                          | `rulebuilding/Build rule to replace comment.feature` |
| Order                                    | Comments are shown in a deterministic order (attribute id). **User-controlled, persisted ordering is not implemented**; see `tickets/attribute_ordering_plan.md`.                                                                              | `kb/Comment ordering.feature`                        |
| Text not blank, at most 2,048 characters | **Not enforced.** Recorded as wanted.                                                                                                                                                                                                          |                                                      |

## Variables in a comment

A comment can include the current value of a case attribute, so that the rendered comment reflects the case's data.

| Requirement       | Description                                                                                                                                                 | Validation                                                        |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| Syntax            | The user writes the attribute name in braces, e.g. `Your patient has a TSH of {TSH} mIU/L.`                                                                 | `comments/Add, replace or remove comments with variables.feature` |
| Substitution      | When the comment is shown for a case, each variable is replaced by the attribute's latest value in that case.                                               | same                                                              |
| Missing value     | If the attribute has no value for the case, `{TSH: no value}` is shown in its place and highlighted, so that an incomplete comment is never silently blank. | same                                                              |
| Attribute renamed | The variable follows the attribute, so renaming the attribute changes how the comment reads without the comment being edited.                               | `CommentTemplateTest`                                             |
| Discoverability   | The facility is in the chat's capability list, and a one-line tip is shown once per conversation the first time a comment is added.                         | `comments/Help for comments with variables.feature`               |
