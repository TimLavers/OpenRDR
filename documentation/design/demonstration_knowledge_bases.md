# Demonstration knowledge bases

## Goals

- Four demonstration knowledge bases the user can open and play with: Thyroid Stimulating Hormone, Contact Lens
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
builders that `ServerApplication.createKBFromSample` runs; the chat and the `samples` cucumber fixtures use the same
builders. So a demonstration knowledge base is never persisted. "Opening" one is `createKBFromSample(newName,
sample)`; the copy is stored, the demonstration is not, and there is nothing to guard against deletion, no flag to
persist and no schema change.

`SampleKB.demonstrations()` returns the four; the `_CASES` variants remain available through the sample API for
acceptance-test fixtures.

### Listing

`ListKnowledgeBases` prints two sections:

```
Your knowledge bases:
Thyroids (open)

Demonstration knowledge bases (open one to get your own copy):
Contact Lens Prescription
Pathology
Thyroid Stimulating Hormone
Zoo Animals
```

The demonstration section is always present; the first section says there are none when there are none. The system
prompt gains `{{DEMONSTRATION_KB_NAMES}}` beside `{{KB_NAMES}}`, so the model knows the names it is transcribing. The
greeting with no knowledge bases mentions that a demonstration can be opened as well as a new one created.

#### The list is clickable

The bot renders one compact vertical list with muted, accessible headings for "Your knowledge bases" and
"Demonstration knowledge bases". Each name appears once, in a full-width clickable row with underlined blue text,
a hand pointer, and hover and keyboard-focus highlighting. The open KB has an inline "(current)" marker and is inactive.
An info icon beside each heading explains the action on hover: "Click a knowledge base to open it" or "Click a
demonstration to open it. You will get your own copy and be asked to give it a name."
The UI titles, help text and current-KB marker are defined in `constants/chat/Constants.kt`; the transcript headings
reuse the same titles with their existing punctuation and explanation.
Empty stored lists still show their heading and an empty-state message.
The list uses the chat's vertical scrolling; it has no separate horizontal chip row.

`ListKnowledgeBases` supplies `ChatResponse.kbListing`, a `KnowledgeBaseListing` containing stored names,
demonstration names, the open name and a description per name. The UI renders this structure rather than parsing the
response prose. The response text remains available for history and acceptance-test observations, but is not displayed
a second time.

#### Descriptions on hover

Hovering a row shows a one-line summary of that knowledge base's description in the same tooltip style as the heading
help. For a stored KB the summary is `summaryOf(description)`: the first non-blank line with any Markdown heading marks
stripped, cut to `SUMMARY_MAX_LENGTH` (120) characters with an ellipsis. A blank description gives no tooltip. Each
demonstration has a fixed one-line `SampleKB.description()`. The tooltip is deliberately short: descriptions are
Markdown and can run to several lines, and a scrolling tooltip disappears when the pointer moves to it. The full text
is a chat request away: `ShowKnowledgeBaseDescription` takes an optional `kbName`, resolved like any other, so "What is
the description of Zoo Animals?" answers with `Description of "Zoo Animals":` and the text; a partial stored match is
described without asking, since reading changes nothing.

Clicking a row or activating it with the keyboard sends "Open <name>" as an ordinary user message. Name resolution,
the naming prompt for a demonstration, and refusal during a rule session follow the typed-message path. Rows are
disabled while chat is busy. Opening a KB changes context and starts a new conversation, clearing the old list.

### Name resolution

`resolveKbName` returns `Demonstration(sample)` for an exact or unique partial demonstration title. The lookup order is
exact stored name, exact demonstration title, partial stored names, then partial demonstration titles. An exact stored
name wins a legacy title collision; an exact demonstration title wins over a partial stored match. One stored partial
match wins even when demonstrations also match. Several stored partial matches are ambiguous and include matching
demonstrations as candidates; with no stored match, several demonstration partial matches are ambiguous.
A miss lists stored names and demonstrations.

For example, after copying Zoo Animals as "Zoo2", "open Zoo" asks whether to open Zoo2. "Open Zoo Animals" still
selects the demonstration; "open Animals" also does so if no stored name contains "Animals". Near-duplicate warnings
compare stored names only, so "Zoo Animal" is allowed without a warning when no stored name resembles it. Such a
name also takes precedence for "open Zoo". These are intentional consequences of the lookup order and exact-title
reservation.

Demonstration titles are reserved, ignoring case and surrounding whitespace. Chat actions give the refusal
"… is the name of a demonstration knowledge base; please choose another". `KBManager.createKB` (even with `force=true`)
and `renameKB` enforce the same reservation for direct and REST calls. Import creates persistence through `KBImporter`,
so it validates the archive name there before writing any data. These paths share `requireUnreservedKbName`.
Existing stored KBs with legacy title collisions remain readable and can be renamed; no automatic migration changes
their names. Exact stored-name precedence remains for those legacy collisions.

### Opening a demonstration

`OpenKnowledgeBase` is unchanged for the model: it still transcribes the name. On `Demonstration(sample)` the server
asks for the copy's name, reusing the server-owned naming workflow that already exists for the first knowledge base
(`KnowledgeBaseConversation.State.Creating`, stage `AWAITING_NAME`, replies read by `KbCreationReplyInterpreter`).
`AskForName` supplies the
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

### Import and export through chat

Import and export are available through chat. An explicit request immediately opens a native file dialog; there is
no additional "Choose file" chat button and no request to type a path. The remaining KB menu items stay until Step 14
migrates their acceptance coverage. After their removal, the KB name remains visible as a read-only application-bar
label.

| Request          | Dialog and result                                                                                                  |
|------------------|--------------------------------------------------------------------------------------------------------------------|
| "Import a KB"    | Open-file dialog filtered to `.zip`; import the selected archive, open its KB and confirm its name in chat.        |
| "Export this KB" | Save As dialog with a suggested filename such as `Thyroids.zip`; confirm the destination after the write succeeds. |

Windows and macOS (Intel and Apple Silicon) are required platforms. Use
[FileKit](https://github.com/vinceglb/FileKit) for Windows system dialogs and macOS `NSOpenPanel` / `NSSavePanel`.
Material 3 provides no built-in desktop file chooser. FileKit 0.14.1 replaces the unused `mpfilepicker`
dependency; its published Kotlin 2.3.21 and coroutine 1.10.2 dependencies are compatible with this project.
`KbFileDialogs` returns `FileSelection.Selected` or `Cancelled`. `FileKitKbFileDialogs` prepares ZIP choices and
safe filename suggestions, and delegates native calls to `FileKitDialogLauncher` through an injectable
`FileDialogLauncher`. It supplies the application's AWT window and runs dialog calls on `Dispatchers.IO`.
Windows uses that window as the owner. FileKit's macOS implementation dispatches AppKit work to the main thread
and uses application-modal panels; it does not attach a sheet to the supplied AWT window. Tests use a fake launcher
or stub FileKit's entry points, without opening native dialogs.

Only the suggested filename is sanitised: Windows-invalid characters become underscores, trailing dots and spaces
are removed, reserved device names are prefixed with an underscore, and an empty result becomes `knowledge-base`.
The KB name and the chosen destination remain unchanged. FileKit's Windows `IFileSaveDialog` retains the native
[default overwrite prompt](https://learn.microsoft.com/en-us/windows/win32/shell/common-file-dialog).
On macOS, `NSSavePanel`
likewise [asks before replacing an existing file](https://developer.apple.com/documentation/appkit/nsopensavepaneldelegate/panel(_:userenteredfilename:confirmed:)).
No second application confirmation is added. The packaged runtime includes FileKit and JNA; the JNA archive
contains Windows DLLs and macOS native libraries for both Intel and Apple Silicon. It also includes
`jdk.security.auth` for FileKit's Linux support. Build the native application on its target OS and architecture.
Interactive verification of open, save, cancel and overwrite confirmation remains required on both Windows and
macOS; inspecting the libraries in a Windows package does not verify execution on a Mac.

The model identifies import or export intent. The server validates the action and supplies a structured, one-use
file-dialog request in `ChatResponse`; the client does not infer an operation from response prose. Import works
without an open KB. Export requires an open stored KB and captures its identity before showing the chooser. Both
operations remain unavailable during rule building, matching the current menu restriction. Refusals produce chat
text without launching a dialog. Exporting a built-in demonstration requires first opening a named copy.

The client owns local file selection and filesystem access, reuses the import/export HTTP endpoints, and reports
the actual result through deterministic chat messages. Paths and file contents are not sent to the model. Export
uses the captured KB id, so a later context change cannot silently export a different KB. The Save As flow confirms
replacement of an existing file before writing. Import retains the server's archive validation and reserved-title
guard; choosing a filename does not rename the KB inside the archive.

A small client controller owns explicit `Idle`, `ChoosingFile` and `Transferring` states. A request id prevents
recomposition or duplicate response delivery from opening a second chooser or repeating a transfer. Chat submission
is disabled while the operation is pending. Cancellation returns to idle, changes no KB or file, and says "Import
cancelled." or "Export cancelled." Errors are shown in chat and release the busy state; success is reported only
after completion. An imported KB follows the existing client context-change sequence, with its completion message
retained in the new chat rather than lost when the conversation restarts. Export leaves the current KB and case alone.

`KbFileTransferController` owns the operation state and consumed request ids. `ChatState` keeps transcript rendering
and text deduplication separate from request delivery: a new request id still opens a dialog when its prose repeats.
Local completion and failure messages enter the transcript directly, without a model call. The application supplies
the native dialog adapter with its window; tests inject fake selections. Import invalidates the loaded case context
even when the archive replaces the currently open KB with identical KB and case ids. The existing context effects
then fetch its cases and restart the conversation. The transcript persists across that restart.

`Api` performs file reads and writes on `Dispatchers.IO` and checks HTTP status before accepting an import or
writing export bytes. Export takes the KB identity captured in the request. Failed HTTP exports leave an existing
destination untouched. Import validation and damaged-ZIP errors return readable HTTP 400 responses, which are shown
in chat. Chat auto-scrolling requests the next layout pass rather than forcing layout during composition updates.

The implementation sequence is Steps 11–15 in `demonstration_knowledge_bases_implementation_plan.md`. The current
menus stay until the chat flows and replacement acceptance coverage work. Step 11 is implemented: the server
emits `ChatResponse.kbFileDialogRequest` with a fresh request id and, for export, the open `KBInfo`. Serialization,
action dispatch, prompt wiring and refusals are tested. Steps 12 and 13 are implemented: chat launches the native
dialog adapter and performs transfers. The existing menus still perform import and export. Packaged Windows and
macOS native-dialog checks remain scheduled work under Step 15.

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
- Cucumber REST helpers reuse the API instance whose KB was explicitly created or selected. Case deletion must use
  that instance too: a fresh `Api()` has no open KB and no longer falls back to an implicit Thyroids.

## Decisions

- **Recipe, not stored copy.** A stored, flagged demonstration would need a `demo` column in `kb_info` (a migration for
  every existing database), a deletion guard, a copy through export/import with a new id, and a startup that skips
  demonstrations. Rebuilding from the sample needs none of that and the machinery exists.
- **Reserved titles over precedence rules.** Refusing a demonstration title on create and rename costs one message and
  keeps `resolveKbName` a single ordered lookup; letting user names shadow demonstrations would hide a demonstration
  behind a copy with the same name.
- **Reuse the naming workflow.** `kb_management_by_chat.md` said the first-knowledge-base workflow was the pattern to
  extend; both workflows share `KnowledgeBaseConversation.State.Creating` and its action factory.
- **Reverses a non-goal.** `kb_management_by_chat.md` listed creating from a sample as a non-goal; it is now in scope
  through the demonstration list. The duplicate GUI dialog has been removed; sample scenarios create their fixtures
  through the API before starting the client.
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
refusal; reserved titles on create and rename; `ListKnowledgeBases` text and structured `kbListing` (including the open
name); `ChatManager` name-awaiting flow for a demonstration (confirm with name, deny, other request). UI: one grouped
vertical list, an inactive open row, busy-state disabling, and mouse/keyboard activation through "Open <name>".

Cukes, in `kb/Knowledge Base Management.feature`: the list shows both sections; opening a demonstration asks for
a name and the copy opens with the expected case count; clicking a stored knowledge base's row opens it; clicking a
demonstration's row asks for a name; deleting a demonstration is refused; creating with a
demonstration title is refused; the no-knowledge-base greeting mentions demonstrations. Run with `.\gradlew.bat
:cucumber:kb`.

Implementation review corrected alphabetical ordering and separation in no-KB greetings, horizontal chip scrolling,
outdated list expectations and UTF-8 corruption introduced by the bulk default-KB feature edit. Demonstration listing
is covered by the existing listing and deletion scenarios; the duplicate "The list shows demonstrations" scenario
has been removed. Compilation, mock-based helper tests and the full cucumber dry run pass. The real
`:cucumber:kb` run still needs the live server, model and GUI and must be scheduled by the user.
