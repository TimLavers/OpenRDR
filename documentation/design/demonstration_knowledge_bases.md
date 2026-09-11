# Demonstration knowledge bases

## Goals

- Four demonstration knowledge bases the user can open and play with: Thyroid Stimulating Hormone, Contact Lense
  Prescription, Zoo Animals (each with cases and rules, as built by the `samples` cukes) and Pathology (the cases the
  packaged demo uses, `SampleKB.PATHOLOGY`).
- The chat lists them under a separate "demonstration" heading, so it is clear they cannot be deleted.
- Opening one asks for a new name and gives the user a copy under that name. The user's copy is an ordinary knowledge
  base: it can be changed, renamed and deleted; the demonstration it came from never changes.
- The cukes stop loading Thyroids implicitly. A `Background` step says so where a scenario needs it; without the step
  no knowledge base exists.

## How it works

### A demonstration knowledge base is a recipe, not a stored knowledge base

The four demonstrations already exist as code: `SampleKB.TSH`, `CONTACT_LENSES`, `ZOO` and `PATHOLOGY` (renamed from
`DEMO`, title "Pathology"), built by the sample
builders that `ServerApplication.createKBFromSample` runs (the "Create KB from sample" menu item and the `samples` cukes
use exactly this). So a demonstration knowledge base is never persisted. "Opening" one is `createKBFromSample(newName,
sample)`; the copy is stored, the demonstration is not, and there is nothing to guard against deletion, no flag to
persist and no schema change.

`SampleKB.demonstrations()` returns the four; the `_CASES` variants stay available to the GUI dialog only.

### Listing

`ListKnowledgeBases` prints two sections:

```
Your knowledge bases:
Thyroids (open)

Demonstration knowledge bases (open one to get your own copy):
Contact Lense Prescription
Pathology
Thyroid Stimulating Hormone
Zoo Animals
```

The demonstration section is always present; the first section says there are none when there are none. The system
prompt gains `{{DEMONSTRATION_KB_NAMES}}` beside `{{KB_NAMES}}`, so the model knows the names it is transcribing. The
greeting with no knowledge bases mentions that a demonstration can be opened as well as a new one created.

#### The list is clickable

The same names are shown as chips under the message, in one row, stored names first and then demonstrations, in the
manner of the suggested-condition chips
(`SuggestionListMessage` / `SuggestionListRow`), not the tip, which is display only. The server attaches them
deterministically: `ListKnowledgeBases` returns a `ChatResponse` carrying a `kbChoices` list, so the model is never
asked
to classify its own reply. The open knowledge base is shown in the text and excluded from the chips. The row scrolls
horizontally when its contents exceed the available width, including with fewer than five names.

Clicking a chip sends "Open <name>" as an ordinary user message. Name resolution, the naming prompt for a
demonstration, and the refusal during a rule session all happen exactly as if the user had typed it; the chips are a
shortcut, not a second code path. They vanish on their own: opening a knowledge base changes the chat context, which
starts a new conversation. There is no delete chip.

### Name resolution

`resolveKbName` returns `Demonstration(sample)` for an exact or unique partial demonstration title. The lookup order is
exact stored name, exact demonstration title, partial stored names, then partial demonstration titles. An exact stored
name wins a legacy title collision; an exact demonstration title wins over a partial stored match. Multiple partial
matches are ambiguous, and a miss lists stored names and demonstrations.

Demonstration titles are reserved: `CreateKnowledgeBase` and `RenameKnowledgeBase` refuse them (case-insensitive) with
"… is the name of a demonstration knowledge base; please choose another". This is the only new guard, and it is what
keeps resolution unambiguous.

### Opening a demonstration

`OpenKnowledgeBase` is unchanged for the model: it still transcribes the name. On `Demonstration(sample)` the server
asks for the copy's name, reusing the server-owned naming workflow that already exists for the first knowledge base
(`PendingKbCreation`, stage `AWAITING_NAME`, replies read by `KbCreationReplyInterpreter`). `AskForName` supplies the
question and an action factory. The pending state holds what to do with the name: `CreateKnowledgeBase(name)` for an
empty KB or `CopyDemonstrationKnowledgeBase(sample, name)` for a copy. The
interpreter already returns `CONFIRM_WITH_NAME`, `DENY` and `OTHER_REQUEST`, so "call it Zoo2", "no" and "actually,
open Thyroids" all work without new prompt machinery.

`CopyDemonstrationKnowledgeBase` validates the name exactly as `CreateKnowledgeBase` does (blank, clash, near-duplicate
asks first, reserved title), then `KnowledgeBaseService.createFromSample(name, sample)`, which is
`ServerApplication.createKBFromSample` plus `webSocketManager.sendKbInfo`. The client's existing cascade opens it.
Response: "Created \"Zoo2\" from the Zoo Animals demonstration and opened it."

Like open and create, it is refused while a rule is being built.

### Deleting

`DeleteKnowledgeBase` on a `Demonstration` resolution answers "Zoo Animals is a demonstration knowledge base and cannot
be deleted. Your own copies can be." No confirmation, nothing to do.

### Startup

Unchanged. The client opens the first stored knowledge base; demonstrations are not stored, so they are never opened by
accident. With no stored knowledge base the greeting offers to create one or open a demonstration.

`ServerApplication.getDefaultProject` and the `DEFAULT_KB` endpoint are deleted: they create Thyroids lazily, which is
exactly the implicit loading being removed. `Api.kbInfo()` with no knowledge base becomes a `checkNotNull` failure;
the chat and the UI already never reach it without one.

### Packaged demo

`OpenRDRServer` no longer seeds a knowledge base on start: the packaged demo instructions begin with "open Pathology"
and suggest naming the copy "Clinic". `ensureSampleKB`, `DEMO_KB_NAME`, `DEMO_ARG` and the launchers' `Demo` argument
are removed. `DemoZipSmokeTest` expects an empty `kbList` on both launches. The `demo` cucumber script instead opens
an explicit default KB and supplies its own Taylor case, as specified in implementation Step 8.

## Cukes: an explicit default knowledge base

- `StepsInfrastructure.startServerWithInMemoryDatabase` stops calling `createKBWithDefaultName()`.
- New step `Given a default KB is opened`: `restClient().createKBWithDefaultName()`. It runs in `Background`, so the
  knowledge base exists before `I start the client application`, and the client opens it as it does today. The step
  replaces `there is a knowledge base called Thyroids`.
- Every feature whose scenarios assume Thyroids gets the `Background`. In the chat KB-management feature the step is
  per scenario, allowing empty-server scenarios to omit it. The `samples` features create their own KBs. Scenarios that
  previously deleted the implicit Thyroids now start without creating it.
- `cucumberDryRun` checks binding; the folders then need a real run.

## Decisions

- **Recipe, not stored copy.** A stored, flagged demonstration would need a `demo` column in `kb_info` (a migration for
  every existing database), a deletion guard, a copy through export/import with a new id, and a startup that skips
  demonstrations. Rebuilding from the sample needs none of that and the machinery exists.
- **Reserved titles over precedence rules.** Refusing a demonstration title on create and rename costs one message and
  keeps `resolveKbName` a single ordered lookup; letting user names shadow demonstrations would hide a demonstration
  behind a copy with the same name.
- **Reuse the naming workflow.** `kb_management_by_chat.md` said the first-knowledge-base workflow was the pattern to
  extend; this is the second workflow, so `PendingKbCreation` becomes the shared abstraction rather than a new one.
- **Reverses a non-goal.** `kb_management_by_chat.md` listed creating from a sample as a non-goal; it is now in scope
  through the demonstration list. The GUI "Create KB from sample" dialog stays for now.
- **Nothing in the cukes is loaded that a feature does not name.** The lazy default is deleted rather than left unused.
- **Clickable list, no delete chip.** The list passes the test for an inline chat affordance: the answer set is closed
  and tiny, and the control vanishes after one use. Deletion is confirmed and irreversible, so a one-click chip for it
  fails the same test.
- **One name, "Pathology", and no seeding.** The demonstration was "Demo" and the packaged copy was also "Demo"; one
  "demo" too many. Renaming the demonstration to Pathology reserves that title, so a seeded copy could not share it,
  and a third name would reintroduce the confusion. The packaged demo therefore starts empty and opens the Pathology
  demonstration through the chat, which is the feature being demonstrated anyway.

## Tests

Server, no model: `resolveKbName` with demonstration titles (exact, partial, shadowed by a stored name);
`OpenKnowledgeBase` → awaiting a name; `CopyDemonstrationKnowledgeBase` validation and creation; `DeleteKnowledgeBase`
refusal; reserved titles on create and rename; `ListKnowledgeBases` format and `kbChoices` (open one excluded);
`ChatManager` name-awaiting flow for a demonstration (confirm with name, deny, other request). UI: the chip rows render
the two groups and a click sends "Open <name>".

Cukes, in `kb/Knowledge Base management by chat.feature`: the list shows both sections; opening a demonstration asks for
a name and the copy opens with the expected case count; clicking a stored knowledge base's chip opens it; clicking a
demonstration's chip asks for a name; deleting a demonstration is refused; creating with a
demonstration title is refused; the no-knowledge-base greeting mentions demonstrations. Run with `.\gradlew.bat
:cucumber:kb`.

Implementation review corrected alphabetical ordering and separation in no-KB greetings, horizontal chip scrolling,
outdated list expectations and UTF-8 corruption introduced by the bulk default-KB feature edit. The seven demonstration
scenarios are implemented; compilation, mock-based helper tests and the full cucumber dry run pass. The real
`:cucumber:kb` run still needs the live server, model and GUI and must be scheduled by the user.
