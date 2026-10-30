# User-defined case lists

A knowledge base has built-in Processed and Cornerstone lists. Users can also organise copies of cases into named
lists such as "Borderline". A list is created with its first copy and disappears when its last case is deleted;
there is no separate list registry.

| Requirement | Behaviour | Validation |
|---|---|---|
| Copy | Copy the currently selected case through chat, supplying a list name and optionally a new name for the copy. The original is unchanged. | `cases/User defined case lists.feature` |
| Current case only | Names are not unique across lists, so chat copies the selected case rather than guessing from a name. | `CopyCaseToListTest` |
| List identity | Names are trimmed and compared without case sensitivity; an existing list retains its spelling. | `CaseListTypeTest`, `KBTest` |
| Reserved names | Blank names and built-in names, including variants such as "Cornerstone Cases", are refused. | `cases/User defined case lists.feature`, `KBTest` |
| Ordering | Lists follow the ID of their first case; cases within a list follow their IDs, preserving copy order. | `CaseManagerTest` |
| Delete | Delete the selected copy from its user-defined list. Other copies and the original remain. | `cases/User defined case lists.feature` |
| Export/import | User-defined cases round-trip with their list names. | `KBImporterTest`, `cases/User defined case lists.feature` |

All user-defined cases are exported into one `UserDefinedCases` directory. Each case's serialised `CaseId.type`
contains the list name, so list names do not become directory names and need no filesystem encoding.

**Not implemented:** editing a stored copy's values, renaming it after copying, and explicit list rename operations.
A different case name can be supplied when making a new copy.

See [cases and attributes](cases_and_attributes.md) for navigation and the built-in lists.
