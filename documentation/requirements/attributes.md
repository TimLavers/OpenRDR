# Attribute Requirements

`Attribute`s name clinical tests and other significant clinical data items, and also the things a knowledge base works
out for itself: derived values and comments.

An `Attribute` is identified by its id, not its name, so changing a KB-assigned attribute's name does not disturb the
rules or conditions that refer to it. External attributes cannot currently be renamed because their names come from the
external system.

| Requirement                   | Description                                                                                                                                                  | Validation |
|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| `Attribute` names             | Each `Attribute` has a name.                                                                                                                                 | Attr-1     |
| Name not empty                | An `Attribute` name must contain at least one character. The constructor currently permits whitespace-only names; renaming a KB-assigned attribute does not. | Attr-2     |
| Maximum length of name        | There can be at most 255 characters in an `Attribute` name.                                                                                                  | Attr-3     |
| Unique exact names            | No two `Attribute`s in a `Knowledge Base` can have exactly the same name.                                                                                    | Attr-4     |
| External names case sensitive | External attribute names are matched case-sensitively, so names that differ only by case remain distinct.                                                    | Attr-5     |
| KB-assigned names ignore case | Creating or renaming a derived or comment attribute refuses a name used by any attribute ignoring case; changing only the case of its own name is allowed.   | Attr-6     |

## Kinds of attribute

Every attribute is of one of three kinds, which decides where its values come from and where they are shown.

| Kind       | Values come from                        | Shown in                                           |
|------------|-----------------------------------------|----------------------------------------------------|
| `EXTERNAL` | the external system that sends the case | the case data table                                |
| `DERIVED`  | the rules, as a value or a formula      | the Derived attributes panel                       |
| `COMMENT`  | the rules, as a text                    | the Comments panel; see [comments.md](comments.md) |

| Requirement         | Description                                                                                            | Validation |
|---------------------|--------------------------------------------------------------------------------------------------------|------------|
| Attribute kind      | Each `Attribute` is external, derived or a comment.                                                    |            |
| Panel by kind       | Derived and comment attributes are shown in their own panels, not as rows of the case data table.      |            |
| External name clash | External data for a name a derived attribute owns is given a distinct `"<name> (external)"` attribute. |            |

## Renaming

| Requirement            | Description                                                                                                     | Validation |
|------------------------|-----------------------------------------------------------------------------------------------------------------|------------|
| Rename                 | A derived or comment attribute can be renamed at any time, through the chat.                                    |            |
| Rename is a label only | Renaming changes the name and nothing else: the rules, conditions and values that refer to it are unaffected.   |            |
| Name in use refused    | A rename to a name already in use is refused.                                                                   |            |
| External not renamable | An attribute that comes with the case data cannot be renamed, its name being the one the external system sends. |            |
