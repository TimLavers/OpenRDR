# Chat UI guidelines

## The chat is the general-purpose surface

The chat exists to replace standing GUI controls, not to grow new ones. Open-ended input — typed or spoken — is the
default way to do anything, and the model interprets it. Persistent controls are not reintroduced beside the chat.

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
| Knowledge base list chips (open on click)     | Yes        | Closed, tiny; opening restarts the conversation, so the chips go with it.    |
| Cornerstone "Allow" / "Don't allow"           | Yes        | Binary, one shot. Not yet built.                                             |
| "Provide more reasons?" buttons               | No         | The real answer is usually a reason, in words; a button invites a worse one. |
| Delete knowledge base chip                    | No         | Irreversible; confirmation is the point.                                     |
| Anything that needs a value or an explanation | No         | Not a closed set.                                                            |
