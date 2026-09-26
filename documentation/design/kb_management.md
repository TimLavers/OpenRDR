# Knowledge base management

Requirements: [knowledge_bases.md](../requirements/knowledge_bases.md). The conversation machinery (contexts,
confirmations, the naming workflow, intent interpretation) is in [chat_architecture.md](chat_architecture.md); this doc
covers what is specific to knowledge bases.

## Actions

KB management is a set of `KbManagementAction`s in the same prompt as rule building, executed through
`KnowledgeBaseService`. A second "router" model in front of the KB chat was tried and dropped: two model calls per
turn, context lost at the hand-off, duplicated dispatch.

| Action                                       | Behaviour                                                                                                   |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `ListKnowledgeBases`                         | Stored and demonstration sections, the open KB marked; the client renders `ChatResponse.kbListing` as rows. |
| `OpenKnowledgeBase(name)`                    | Exact stored match opens; partial match asks; a demonstration asks for a copy name.                         |
| `CreateKnowledgeBase(name)`                  | Refuses a clash or a reserved title; asks on a near-duplicate.                                              |
| `CopyDemonstrationKnowledgeBase`             | Server-only: validates the name like create, builds the sample, opens the copy.                             |
| `CloseKnowledgeBase`                         | Tells the client to close; the server has no "current KB", requests carry the id.                           |
| `DeleteKnowledgeBase(name?)`                 | Refuses a demonstration; always confirms, defaulting to the open KB.                                        |
| `RenameKnowledgeBase(newName)`               | Keeps the id, refuses reserved titles.                                                                      |
| `ShowKnowledgeBaseDescription(name?)`        | Reads the description; a server action because the description is not in the prompt.                        |
| `SetKnowledgeBaseDescription(text)`          | Replaces the whole description; a chat is a poor place to edit a multi-paragraph document piecewise.        |
| `AddDemonstrationCase`                       | Adds Einstein to an empty KB.                                                                               |
| `ImportKnowledgeBase`, `ExportKnowledgeBase` | Return a one-use file-dialog request; the client does the transfer.                                         |

Actions that change what the chat is about (open, create, copy, close, delete, import, export) are refused during a
rule session.

## Name resolution

`resolveKbName` searches in order: exact stored name, exact demonstration title, partial stored names, partial
demonstration titles, ignoring case and with no edit distance. One partial match asks before acting; several are listed
as ambiguous; a miss lists what exists. **Generous matching, careful acting**: one extra turn in the doubtful cases and
no silent surprises. The model must pass the name exactly as the user gave it, or the user would never be asked about a
partial match.

Demonstration titles are reserved for create, rename, copy and import, enforced in `KBManager` and `KBImporter` so that
REST and import paths obey it too, with the chat adding conversational refusals. Reservation was chosen over precedence
rules because a stored KB with a demonstration's title would hide the demonstration; refusing costs one message and
keeps resolution a single ordered lookup. Legacy stored KBs that already carry a title remain readable and win an exact
match.

## Demonstrations are recipes

A demonstration is never stored. `SampleKB` names four recipes built by the sample builders that the acceptance tests
also use; "opening" one is `createKBFromSample(newName, sample)`. There is therefore nothing to guard against deletion,
no flag to persist, and no startup logic to skip demonstrations. A stored, flagged copy would have needed a schema
change, a deletion guard, a copy-with-new-id and a startup filter.

Startup opens the first stored KB. No KB is created implicitly: the old lazy "Thyroids" default was removed so that the
acceptance tests load nothing a feature does not name, and the packaged demo starts empty and opens the Pathology
demonstration through the chat, which is the feature being demonstrated.

## Import and export

The model identifies the intent; the server validates (a KB is open for export; no rule session) and returns a
`KbFileDialogRequest` with a fresh request id and, for export, the open KB's identity. The client opens the native
dialog (FileKit, chosen because Compose Desktop has no file chooser and it covers Windows and macOS with the one API),
performs
the HTTP transfer, and puts the outcome — success, cancellation, error — into the transcript directly without a model
call. The request id prevents recomposition from opening a second dialog; capturing the KB identity means a context
change cannot export a different KB; chat input is disabled while a transfer is pending.

An import gets a fresh id and can only clash by name, which is refused before anything is stored. The imported KB is
registered with `KBManager` like any other. The client treats an import as a context change and restarts the
conversation, keeping the completion message.

The native dialogs' own overwrite prompts are relied on; no second confirmation is added. Only the *suggested* file name
is sanitised for Windows; the KB name and the chosen path are untouched.

## The GUI follows the server

Open, create, copy and rename push KB info over the web socket and close pushes a closed event; the client's cascade
(KB → cases → first case → start conversation) does the rest. A renamed `KBInfo` is equal to the old one (equality is
by id), so the client's KB state uses a never-equal policy to recompose.
