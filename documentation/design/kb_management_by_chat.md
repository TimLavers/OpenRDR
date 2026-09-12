# Managing knowledge bases through the chat

## Goals

- From the chat, with or without a knowledge base open: **list** the knowledge bases, **create** one by name (which
  opens it), and **open** one by name.
- With a knowledge base open: **close** it, **delete** it (or another, by name) after confirmation, **rename** it, and
  **show** or **replace** its description.
- The GUI reflects each change without the user touching the anchor menu: the name in the bar, the case list, the chat
  context.
- The chat is usable when the knowledge base has no cases, and when no knowledge base is open at all. An empty knowledge
  base offers a demonstration case.
- Everything that matters is deterministic and unit-tested on the server without a model: name resolution, ambiguity,
  confirmation, refusal during a rule session.

Non-goals: import and export (both need a file path), incremental editing of the description,
and more than one client at a time.

## How it works

### One conversation, owned by the application

The conversation used to belong to a case: a `ChatManager` per case selection, inside the `KBSession` of one knowledge
base. Knowledge base management must work *above* a knowledge base, so the conversation moved up a level.
`ChatCoordinator`, owned by `ServerApplication`, holds the one `ChatManager` and the `ChatContext` it was built for:

- `NoKnowledgeBase`
- `KnowledgeBaseOnly(endpoint)`
- `CaseInKnowledgeBase(endpoint, viewableCase)`

The context decides the system prompt sections, the functions the model may call (none without a case), the opening
message, and which actions make sense. Without a case the greeting is fixed server text rather than a model reply: with
no knowledge bases it offers to create one or open a demonstration; with some it lists them and the demonstrations and
asks whether to open one or create another; with
an empty knowledge base it explains that cases normally come from an external information system and offers a
demonstration case (Einstein, the same patient the demo and the acceptance tests use).

The client starts a conversation whenever its context changes: a knowledge base opened or closed, a case selected. The
server never starts one. A user message that arrives while the client is still starting the next conversation waits on a
mutex in the coordinator, so two turns never run against the same model chat at once.

### Actions

Knowledge base management is a set of extra actions in the *same* system prompt as rule building. `Action` is a sealed
interface with two kinds: the existing `ChatAction`, which works on the open knowledge base and its case through a
`RuleService`, and `KbManagementAction`, which works on the set of knowledge bases through a `KnowledgeBaseService`
(`ApplicationKbService`, which delegates to `ServerApplication` and pushes the result over the web socket).
`ChatManager` dispatches on the kind; a `ChatAction` with no knowledge base open is refused with a fixed message.

| Action                                         | Behaviour                                                                                            |
|------------------------------------------------|------------------------------------------------------------------------------------------------------|
| `ListKnowledgeBases`                           | One vertical list with stored/demo headings and clickable names; the open KB is marked and inactive. |
| `OpenKnowledgeBase(kbName)`                    | Opens an exact stored match, confirms a partial stored match; a demonstration asks for a copy name.  |
| `CopyDemonstrationKnowledgeBase(sample, name)` | Server-only action: validates the name, builds the sample and opens the stored copy.                 |
| `CreateKnowledgeBase(kbName)`                  | Refuses clashes and reserved demonstration titles; confirms near-duplicates.                         |
| `CloseKnowledgeBase`                           | Tells the client to close; nothing changes on the server.                                            |
| `DeleteKnowledgeBase(kbName?)`                 | Refuses demonstrations; confirms deletion of a stored KB, defaulting to the open one.                |
| `AddDemonstrationCase`                         | Adds Einstein to the open knowledge base.                                                            |
| `RenameKnowledgeBase(newName)`                 | Renames the open knowledge base, refusing reserved demonstration titles; keeps the id.               |
| `ShowKnowledgeBaseDescription`                 | Reads the description from the server; the model never answers from memory.                          |
| `SetKnowledgeBaseDescription`                  | Replaces the whole description with the user's words, transcribed not composed.                      |

Actions that change what the chat is about (open, create, copy, close, delete) are refused while a rule is being built.
Rename, describe, list and the demonstration case are not.

### The server holds every confirmation

An action that needs the user's say-so returns `KbManagementOutcome.Ask(question, thenDo)` instead of a response. The
question goes to the user and `thenDo`, a lambda that has already captured the resolved `KBInfo`, is held by
`ChatManager` for exactly one turn. A plain acceptance runs it without consulting the model; anything else drops it and
goes to the model as usual. The model is told never to ask for confirmation itself, because the server asks when it
needs
to, and a lambda rather than an action class means there is nothing the model could name to skip the question.

### Creating a knowledge base or demonstration copy: shared naming workflow

When no knowledge base exists the server owns the offer to create one and the request for its name, as explicit stages
(`OFFER_CREATION`, `AWAITING_NAME`). A plain acceptance ("yes", "ok") is answered by the server at either stage, as
for every other server question, and moves to or stays at naming. Any other reply is sent to the model with the pending
question and stage, and the model returns an intent (`CONFIRM`, `DENY`, `CONFIRM_WITH_NAME`, `UNCLEAR`,
`OTHER_REQUEST`) plus, where given, the name exactly as the user wrote it, in their language. The server chooses the
transition, validates the name, and executes. Malformed or off-contract model output executes nothing and keeps the
stage. `PendingKbCreation` holds an action factory: `CreateKnowledgeBase` for an empty KB or
`CopyDemonstrationKnowledgeBase` for a demonstration copy. `KbManagementOutcome.AskForName` starts the latter directly
at `AWAITING_NAME`, preserving its question on a plain confirmation. Both actions use the same blank, duplicate,
reserved-title and near-duplicate checks. The server decides, the model reads.

### Name resolution

`resolveKbName` searches in order: exact stored name, exact demonstration title, partial stored names, partial
demonstration titles. Matching ignores case. Stored matches return `Exact` or `Partial`; a demonstration returns
`Demonstration(sample)`. Multiple partial matches are `Ambiguous`; a miss returns `NotFound` with stored and
demonstration names. No edit distance is used. Partial stored matches ask before opening or deleting; a demonstration
asks for a copy name when opened and is refused when deleted. An existing stored name wins an exact title collision,
although new demonstration-title collisions are refused on create, copy and rename.

### The GUI follows the server

`WebSocketManager` pushes `KbInfo:` (open, create, copy, rename) and `KbClosed`, exactly as it pushes cases and
cornerstones.
The client sets `Api.currentKB` before the UI sees the event, then its existing cascade (`kbInfo` → cases → first case →
`startConversation`) does the rest. Chat requests carry the ids they are given. The lazy default knowledge base endpoint
has been removed; `Api.kbInfo()` requires an open knowledge base. Clickable list rows send ordinary `Open <name>`
messages
through the same chat pipeline as typed text.

## Decisions

- **One model, one prompt — not a router.** An earlier branch put a second "router" model in front of the knowledge base
  chat. It cost two model calls per turn, lost context at the hand-off and duplicated the dispatch machinery.
- **Server-owned workflow, model-interpreted language.** The server holds the question and the state and chooses the
  next step; the model only says what the user meant. Chosen over an English acceptance word list alone, which cannot
  handle "oui" or "yes, call it Thyroid". The word list still runs first: a plain yes to a server question is never
  put to a model that did not ask it, which had it answering "yes" with a clarification. The older one-turn
  confirmations use the word list only, until a second workflow shows what a shared abstraction should look like.
- **The client starts conversations; the server never does.** After open, create or close the server only pushes state.
  If it also restarted the conversation, the client's own cascade would start a second one and the two greetings would
  race.
- **Generous matching, careful acting.** Partial names are accepted for open and delete but always ask first; delete
  asks
  even on an exact match; create warns on a near-duplicate. One extra turn in the doubtful cases, no silent surprises.
- **Fixed greetings without a case.** They exist to tell the user exactly what they can do next; that wording should not
  vary from run to run.
- **A demonstration case for an empty knowledge base.** Einstein is still offered when an empty knowledge base is open.
  The separate demonstration list offers complete sample knowledge bases as named copies, including their cases and
  rules.
- **`ruleService` is nullable in `ChatManager`, not a null object.** A null object would make `AddComment` "succeed"
  with
  nothing happening; a null makes the refusal explicit and testable.
- **`KbManagementAction` is a sibling of `ChatAction`, not a change to `ChatAction.doIt`.** A single `doIt(context)` for
  every action is cleaner in the abstract but touches every existing action and its tests for no behavioural gain.
- **"Close" is a client state.** The server has no current knowledge base; requests carry `kbId`. The only server-side
  effect of closing is that the next `startConversation` arrives with no ids.
- **Rename keeps the id.** The id was only seeded from the name and is the Postgres database name; re-deriving it would
  mean copying a database. A renamed `KBInfo` is equal to the old one (equality is by id), so the client's `kbInfo`
  state
  uses `neverEqualPolicy()` to recompose.
- **The description is replaced, not edited, by chat.** A conversation is a poor place to edit a multi-paragraph
  document; the GUI dialog remains for anything finer. Reading it back is a server action because the description is not
  in the prompt.
- **The model transcribes names; the server resolves them.** For open, create, delete and rename the model must pass the
  name exactly as the user gave it, even when it is obviously close to an existing one. If the model "corrected" it, the
  user would never be asked about a partial match.

## Acceptance tests

`cucumber/src/test/resources/requirements/kb/Knowledge Base management by chat.feature`, run with
`.\gradlew.bat :cucumber:kb`.
