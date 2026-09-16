# Chat UI guidelines

## The chat is the general-purpose surface

The chat exists to replace standing GUI controls, not to grow new ones. Open-ended input — typed or spoken — is the
default way to do anything, and the model interprets it. Persistent controls are not reintroduced beside the chat.

All KB management uses chat, including the clickable lists of stored and demonstration knowledge bases. The
application bar displays the current KB name as a read-only label, or "No KB selected"; there is no KB menu.

An outlined info icon beside the open KB name shows its description on hover or keyboard focus. The tooltip uses
the case panels' dark surface and 12 sp text, wrapping at 400 dp. An empty description says "No description set.
You can add one through chat." No icon is shown without an open KB. Descriptions are fetched for the displayed KB
on opening/import and after chat operations, so editing the description is reflected without reopening the KB.
Loading and fetch failures have explicit messages. This is passive information, not a KB-management control.

Import and export requests immediately open native file dialogs through FileKit.
This is a deliberate exception to text-only open-ended
input: choosing a local file is better served by the operating system's file browser. It is a temporary system dialog,
launched by a server-validated structured request, with no standing control or extra "Choose file" chat button.
The client handles the chosen file directly and reports the result; paths are not ordinary messages sent to the model.
See [the import/export design](demonstration_knowledge_bases.md#import-and-export-through-chat).

## Capability summary

The server-owned capability catalogue includes ZIP import/export, demonstration KB copies and cases,
reading and changing KB descriptions, editing derived definitions, managing rule reasons, and favourite cases,
alongside report comments and layout operations. Without a current case, the summary offers KB operations only;
it explains that import needs no open KB and export does.

Implemented: a separate help card within the chat, with its own vertical scrollbar and a height capped at 380 dp or
85% of the available message area, whichever is smaller. Headings use the case panels' 13 sp semibold dark-grey
style. The first bullet is 2 dp below its heading; groups are separated by 10 dp. The chat input remains outside the
card and visible while the user scrolls it.

The model emits `ListCapabilities`; the server supplies explicit `CapabilitySection` headings and items through
`ChatResponse.capabilities`. The prompt no longer maintains a duplicate catalogue. The response retains a plain-text
equivalent for accessibility and transcript consumers; the client renders the catalogue once as a card. A new section
list remains visible even if its accompanying text matches a previous reply.

## Inline affordances: the test

An ephemeral control inside the chat (a chip, a button under a message) may be added only when **both** hold:

1. **The answer set is closed and tiny.** The user has nothing to say beyond picking one of a handful of options that
   the server already knows.
2. **The control vanishes after one use.** It answers the question it was attached to and is gone; it never becomes a
   standing control.

If either fails, the answer belongs in the text field.

## How an affordance is built

- **The server attaches it deterministically.** The action that produces the message also produces the choices (as the
  suggested-condition chips do through `SuggestionsBuffer`). The model is never asked to classify its own reply.
- **A click sends ordinary text.** The chip's text goes through the same pipeline as if typed, so the control is a
  shortcut, not a second code path, and everything the typed form is subject to (resolution, confirmation, refusal
  during a rule session) applies.
- **No destructive one-click.** Anything irreversible is confirmed in words.

## Judgements so far

| Affordance                                    | Verdict    | Why                                                                          |
|-----------------------------------------------|------------|------------------------------------------------------------------------------|
| Suggested-condition chips                     | Yes, built | Closed list from the suggester; consumed by the rule being built.            |
| Knowledge base list rows (open on click)      | Yes, built | One grouped vertical list; opening restarts the conversation and clears it.  |
| Cornerstone "Allow" / "Don't allow"           | Yes        | Binary, one shot. Not yet built.                                             |
| "Provide more reasons?" buttons               | No         | The real answer is usually a reason, in words; a button invites a worse one. |
| Delete knowledge base chip                    | No         | Irreversible; confirmation is the point.                                     |
| Anything that needs a value or an explanation | No         | Not a closed set.                                                            |

KB list rows use compact spacing, blue underlined names and hover/focus highlighting. The current KB is inactive
and marked "(current)" immediately after its name. Muted section headings have info icons with hover instructions,
including the naming and copy behaviour for demonstrations. Hovering a row shows a one-line summary of that KB's
description; a tooltip is passive disclosure, not a control, so it does not need the affordance test. The full
description is asked for in the chat.
