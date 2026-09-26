# Knowledge bases

A knowledge base (KB) holds attributes, conditions, rules, definitions and cases. An OpenRDR server manages many; the
client has at most one open at a time, and all management is done through the chat.

## Identity

| Requirement     | Description                                                                                                                                                                                                       | Validation                             |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| Name and id     | A KB has a name (non-blank, at most 127 characters, no newline) and an id. Equality is by id.                                                                                                                     | `KBInfoTest`                           |
| Id format       | The id is the lower-cased alphanumeric name, an underscore and random digits; at most 127 characters of lower-case letters, digits and underscores, so that it can be a Postgres database name and a URL segment. | `KBInfoTest`                           |
| Rename keeps id | A KB can be renamed without changing its id, so nothing persistent moves.                                                                                                                                         | `kb/Knowledge Base Management.feature` |
| Unique          | No two KBs have the same id; no two have the same name, ignoring case.                                                                                                                                            | `KBManagerTest`                        |
| Reserved names  | The demonstration titles cannot be used for a stored KB, on create, rename or import, so that a demonstration is never hidden behind a copy of the same name.                                                     | `kb/Knowledge Base Management.feature` |

## Management by chat

| Requirement        | Description                                                                                                                                                          | Validation                             |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| List               | Stored KBs and demonstrations are listed under separate headings, the open one marked; the list is clickable.                                                        | `kb/Knowledge Base Management.feature` |
| Open               | An exact name opens at once; a unique partial match asks first; several matches are listed.                                                                          | same                                   |
| Create             | A new, empty KB is created by name and opened. A near-duplicate of an existing name asks first.                                                                      | same                                   |
| Close              | Closing leaves no KB open; the chat continues without one.                                                                                                           | same                                   |
| Delete             | Deletion of the open or a named KB is always confirmed first, even on an exact match, because it is irreversible.                                                    | same                                   |
| Rename             | The open KB can be renamed.                                                                                                                                          | same                                   |
| Description        | A KB has a Markdown description that can be read or replaced (not edited piecewise) through the chat, and is shown on hover over the KB name in the application bar. | same                                   |
| Demonstration case | An empty KB offers a demonstration case (Einstein) so that rule building can be tried without an external system.                                                    | same                                   |
| During a rule      | Open, create, copy, close, delete, import and export are refused while a rule is being built. Rename, describe and list are not.                                     | same                                   |
| Startup            | The client opens the first stored KB; with none, the greeting offers to create one or open a demonstration. No KB is created implicitly.                             | same                                   |

## Demonstration knowledge bases

Four demonstrations ship with the application, so that a new user can practise rule building on realistic data:

| Title                       | Source                                                                                                                      |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Thyroid Stimulating Hormone | Vasikaran and Loh, *Interpretative commenting in clinical chemistry with worked examples for thyroid function test reports* |
| Contact Lens Prescription   | UNSW machine learning course notes on RDR                                                                                   |
| Zoo Animals                 | Compton and Kang, *Ripple-Down Rules*, chapter 5                                                                            |
| Pathology                   | The cases the packaged demo script uses                                                                                     |

| Requirement     | Description                                                                                                                                      | Validation                             |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| Copy on open    | Opening a demonstration asks for a name and gives the user their own copy under it. The copy is an ordinary KB; the demonstration never changes. | `kb/Knowledge Base Management.feature` |
| Cannot delete   | A demonstration cannot be deleted; the user's copies can.                                                                                        | same                                   |
| Cases and rules | Each demonstration contains its cases and the rules that interpret them (`samples/*.feature` build them and check the interpretations).          | `samples/*.feature`                    |

## Import and export

| Requirement  | Description                                                                                                                                                             | Validation                             |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------|
| Export       | "Export this KB" opens the operating system's Save dialog with a suggested file name; the open KB is written as a ZIP including its cases, cornerstones and favourites. | `kb/Knowledge Base Management.feature` |
| Import       | "Import a KB" opens the Open dialog filtered to ZIP; the KB is imported with a fresh id and opened. No KB needs to be open.                                             | same                                   |
| Name in use  | An archive whose KB name is in use is refused before anything is stored; restoring a backup means deleting or renaming the stored one first.                            | same                                   |
| Cancellation | Cancelling either dialog changes nothing and says so in the chat.                                                                                                       | same                                   |
| Privacy      | File paths and contents never go to the model; the client does the transfer and reports the result.                                                                     | same                                   |

## Interpretation service

| Requirement    | Description                                                                                                             | Validation                                |
|----------------|-------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| REST interface | An external system posts a case to the open KB and receives its interpretation; the case appears in the processed list. | `interpreter/Interpreter Service.feature` |

Design: [design/kb_management.md](../design/kb_management.md).
