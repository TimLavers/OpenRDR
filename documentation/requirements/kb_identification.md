# KB Identification
`KB`s are identified by a `KBInfo` object that contains a name and an id.
Two `KBInfo`s are equal if they have equal ids.
If a `KB` is to be persisted as a database, the intention is to use the id as the database name,
so that the `KB` name can be changed without problems. To keep things simple, the allowed characters
for an id will be such as to make the id url-safe. In practice, ids will usually be a lower-case
alphanumeric version of the KB name, followed by an underscore and a sequence of random digits.

| Requirement                      | Description                                                  | Validation |
|----------------------------------|--------------------------------------------------------------|------------|
| `KBInfo`                         | Each `KB` has a `KBInfo`, which has a name and id.           | KBId-1     |
| `KBInfo` name not blank.         | A `KBInfo` name cannot be blank.                             | KBId-2     |
| Maximum length of name.          | There can be at most 127 characters in a `KBInfo` name.      | KBId-3     |
| No newlines in name.             | A `KBInfo` name cannot contain a newline character.          | KBId-8     |
| `KBInfo` not blank.              | A `KBInfo` id cannot be blank.                               | KBId-4     |
| `KBInfo` id maximum length.      | A `KBInfo` id can be at most 127 characters.                 | KBId-5     |
| Valid format for id.             | A `KBInfo` id contains only letters, numbers, and hyphens.   | KBId-9     |
| `KBInfo` id determines identity. | Two `KBInfo`s are equal if and only if they have equal ids.  | KBId-6     |
| `KB` identity.                   | In an OpenRDR system, no two `KB`s can have equal `KBInfo`s. | KBId-7     |                                                          |

