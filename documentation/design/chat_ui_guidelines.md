# Chat UI guidelines

## The chat is the general-purpose surface

The chat exists to replace standing GUI controls, not to grow new ones. Open-ended input, typed or spoken, is the
default way to do anything, and the model interprets it. No persistent control is added beside the chat. The
application bar shows the open KB's name as a read-only label with an info icon whose tooltip is the KB description;
that is passive disclosure, not a control.

Two deliberate exceptions to text-only input:

- **Native file dialogs** for import and export. Choosing a local file is what the operating system's browser is for.
  The dialog is a one-use system dialog opened by a server-validated request; the client handles the file and reports
  the result in the chat; paths never go to the model.
- **The inline affordances below.**

## The test for an inline affordance

A control inside the chat (a chip, a row, a button under a message) is allowed only when **both** hold:

1. **The answer set is closed and tiny.** The user has nothing to say beyond picking one of a handful of options the
   server already knows.
2. **The control vanishes after one use.** It answers the message it is attached to and never becomes standing.

If either fails, the answer belongs in the text field.

## How an affordance is built

- **The server attaches it deterministically.** The action that produces the message produces the choices; the model
  is never asked to classify its own reply.
- **A click sends ordinary text.** The chip's text goes through the same pipeline as if typed, so the control is a
  shortcut, not a second code path, and every check the typed form gets (resolution, confirmation, refusal during a
  rule session) applies.
- **Nothing irreversible is one click.** Deletion is confirmed in words.

## Judgements

| Affordance                                 | Verdict            | Why                                                                               |
|--------------------------------------------|--------------------|-----------------------------------------------------------------------------------|
| Suggested-condition chips                  | Built              | Closed list from the suggester, consumed by the rule being built.                 |
| Knowledge base list rows                   | Built              | One grouped list; a click sends `Open <name>`; the conversation restarts on open. |
| Capability card                            | Built (display)    | Scrollable card with headings; no controls in it.                                 |
| Cornerstone "Allow" / "Don't allow"        | Allowed, not built | Binary, one shot.                                                                 |
| "Provide more reasons?" buttons            | No                 | The real answer is usually a reason, in words; a button invites a worse one.      |
| Delete knowledge base chip                 | No                 | Irreversible; the confirmation is the point.                                      |
| Anything needing a value or an explanation | No                 | Not a closed set.                                                                 |

Hover tooltips (a KB's one-line description on its row, help on a heading) are passive disclosure and need not pass the
test.
