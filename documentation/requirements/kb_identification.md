# KB Identification
`KB`s are identified by a `KBInfo` object that contains a name and an id.
Two `KBInfo`s are equal if they have equal ids.

| Requirement                      | Description                                                                | Validation |
|----------------------------------|----------------------------------------------------------------------------|------------|
| `KBInfo`                         | Each `KB` has a `KBInfo` that has a name and id, both of which are `String`s. | KBId-1     |
| `KBInfo` name not blank.         | A `KBInfo` name cannot be blank.                                           | KBId-2     |
| Maximum length of name.          | There can be at most 127 characters in a `KBInfo` name.                    | KBId-3     |
| No newlines in name.             | A `KBInfo` name cannot contain a newline character.                        | KBId-8     |
| `KBInfo` id blank allowed.       | A `KBInfo` id can be blank.                                                | KBId-4     |
| `KBInfo` id maximum length.      | A `KBInfo` id can be at most 127 characters.                               | KBId-5     |
| No newlines in id.               | A `KBInfo` id cannot contain a newline character.                          | KBId-9     |
| `KBInfo` id determines identity. | Two `KBInfo`s are equal if and only if they have equal ids.                | KBId-6     |
| `KB` identity.                   | In an OpenRDR system, no two `KB`s can have equal `KBInfo`s.               | KBId-7     |                                                          |

