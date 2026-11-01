# Architecture

## Modules

| Module      | Role                                                                                                                                                                                                                                   |
|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `common`    | The model shared by server and client: attributes, cases, conditions, rules, value expressions, view types, wire types and constants. Built as Kotlin/JVM and shared by the server and Compose Desktop client; it is not currently a multiplatform module. |
| `llm`       | The Gemini client and the one-shot primitives built on it: text generation, structured Boolean classification and audio transcription.                                                                                                                                    |
| `hints`     | Translation of a natural-language expression into a `Condition` (`ConditionGenerator`, `ConditionChatService`).                                                                                                                        |
| `chat`      | The multi-turn conversation loop with function calling, retries and timeouts (`Conversation`, `ChatService`).                                                                                                                          |
| `server`    | Ktor server: knowledge bases, persistence, inference, rule sessions, condition suggestion, the operator chat and its actions, the report service, REST and web-socket routes.                                                          |
| `ui`        | Compose Desktop client: case list, case view, comment and derived-attribute panels, cornerstone view, report, chat panel, voice input.                                                                                                 |
| `cucumber`  | Acceptance tests driving the real client and server; see [testing.md](testing.md).                                                                                                                                                     |
| `packaging` | The self-contained demo distribution and its smoke test.                                                                                                                                                                               |

## Server and client

All knowledge lives on the server. The client is a thin presenter: it fetches `ViewableCase`s, renders them, and sends
the user's chat messages. Authoring, condition translation, AI classification and report generation run on the
server and validate model output there. Voice transcription is the exception: the desktop client calls Gemini
through the shared `llm` module and needs the API key locally. See [chat_architecture.md](chat_architecture.md).

The server has one `ServerApplication` holding a `KBManager` (the stored knowledge bases) and one `ChatCoordinator`
with a conversation and open-KB context per user. Each loaded KB has a `KBSession` containing its shared `KB`, a lock,
and a `RuleSessionManager` per user. REST routes reach that session through `KBEndpoint`; chat rule actions use
`LockedRuleService`. Users can build rules concurrently, with each KB access serialised and each commit revalidated
against the current knowledge. See [concurrent_users.md](concurrent_users.md).

## How the client learns of server changes

The client connects to a web socket on startup and the server pushes:

| Message                | When                                                                                                                    |
|------------------------|-------------------------------------------------------------------------------------------------------------------------|
| cases info             | the processed case list changes, e.g. a case arrives through the REST interpreter                                       |
| cornerstone status     | a rule session starts, a condition is added or removed, a cornerstone is allowed, a comment is renamed during a session |
| rule session completed | a rule is committed or cancelled                                                                                        |
| KB info / KB closed    | a knowledge base is opened, created, copied, renamed or closed through the chat                                         |
| chat context           | the user's conversation starts with a KB and case context; their other windows adopt it                                 |
| presence               | a connection, open-KB context, or rule-session state changes                                                            |
| KB changed             | a logged edit supplies its author and summary and tells windows on that KB to refresh                                   |

Pushing was chosen over polling because the cornerstone review is conversational: the panel must change the moment the
chat says it has, and polling forces a trade-off between that latency and load.

The client also re-fetches the current case after every chat message, since most chat actions change the interpretation
of the case on screen. Change pushes refresh other windows on the KB, including the author's other windows, while
deferring case refresh during an active rule session. Only other users see the attributed notice. Socket delivery
uses an ordered queue outside the KB lock; after a disconnect the client reconnects and recovers missed changes
from the persisted log.

## Consequences

- **Ids, not names.** Attributes, conditions and rules are referenced by id everywhere, so renaming is a label change
  and import/export can re-align objects. See [rule_tree_and_inference.md](rule_tree_and_inference.md).
- **One instance per object within a KB.** A loaded KB is one object graph in which equal objects are the same instance,
  so a change made through one manager is seen by every rule and condition that refers to it. Conditions restored from
  the store are re-pointed at the KB's attribute instances (`alignAttributes`).
- **User-scoped conversations and rule sessions.** Each user has one conversation and at most one active rule
  session per KB. A user's windows share that identity and context. Shared knowledge is protected by the per-KB
  lock, commit-time revalidation, and undo confirmation bound to the reviewed rule session.
