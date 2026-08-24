---
trigger: always_on
---

# Production Kotlin

- Never use the non-null assertion operator `!!` in production code. Use `requireNotNull` / `checkNotNull` with a
  message, a safe call, or `error("...")`. Test code may use `!!`.
- Do not add defensive code for a state that cannot arise. Fix the guard so the state is impossible instead of handling
  it downstream. Design by contract: a guard that no test can trigger is misleading.
- Do not add or delete comments or documentation unless asked. When a comment is warranted, say *why*, not *what*.
- Imports go at the top of the file, never inline.
- Prefer the minimal upstream fix over a downstream workaround. Identify the root cause before implementing; a one-line
  change is better than a refactor when it suffices.
- Never change production code merely to make a test pass. Stop and explain first.
