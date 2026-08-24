---
trigger: always_on
---

# Working together on OpenRDR

- The user commits, not you. When a piece of work is finished, suggest a concise commit message.
- Design docs live in `documentation/design/`. Update the relevant doc when a decision is resolved or a step is
  finished, and re-read the file immediately before editing it — the user reformats docs, so your copy goes stale.
- Give direct, terse answers. When something looks redundant or over-engineered, say so and explain the trade-off rather
  than quietly implementing it.
- Ask before making a structural change whose blast radius is wider than the request.
- Verify with the automated tests you can run yourself, and give the user copy-pastable commands for the ones you cannot
  (any real cucumber run).
- Do not create documentation or progress `.md` files for your own benefit unless the user asks for them.
