---
trigger: always_on
description: 
globs: 
---

# Chat UI

- The chat is the general-purpose surface. Do not propose or add standing GUI controls beside it.
- An inline affordance (chip, button under a message) is allowed only when both hold: the answer set is closed and tiny,
  and the control vanishes after one use. Otherwise the answer belongs in the text field.
- The server attaches such affordances deterministically; the model never classifies its own reply. A click sends
  ordinary text through the normal pipeline. Nothing irreversible is a one-click action.
- Judgements so far and the reasoning are in `documentation/design/chat_ui_guidelines.md`; add new ones there.
