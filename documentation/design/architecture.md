# Architecture

## Modules

| Module      | Role                                                                                                                                                                                                                                   |
|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `common`    | The model shared by server and client: attributes, cases, conditions, rules, value expressions, view types, wire types and constants. Kotlin only, no JVM-specific libraries, so that the same condition code evaluates on both sides. |
| `llm`       | The Gemini client and the one-shot primitives built on it: text generation and audio transcription.                                                                                                                                    |
| `hints`     | Translation of a natural-language expression into a `Condition` (`ConditionGenerator`, `ConditionChatService`).                                                                                                                        |
| `chat`      | The multi-turn conversation loop with function calling, retries and timeouts (`Conversation`, `ChatService`).                                                                                                                          |
| `server`    | Ktor server: knowledge bases, persistence, inference, rule sessions, condition suggestion, the operator chat and its actions, the report service, REST and web-socket routes.                                                          |
| `ui`        | Compose Desktop client: case list, case view, comment and derived-attribute panels, cornerstone view, report, chat panel, voice input.                                                                                                 |
| `cucumber`  | Acceptance tests driving the real client and server; see [testing.md](testing.md).                                                                                                                                                     |
| `packaging` | The self-contained demo distribution and its smoke test.                                                                                                                                                                               |

## Server and client

All knowledge lives on the server. The client is a thin presenter: it fetches `ViewableCase`s, renders them, and sends
the user's chat messages. It never talks to a language model; the Gemini key and every prompt are server-side, so the
model's output can be validated before it touches a knowledge base. See [chat_architecture.md](chat_architecture.md).

The server has one `ServerApplication` holding a `KBManager` (the stored knowledge bases) and one `ChatCoordinator` (the
current conversation). Each open KB has a `KBEndpoint`, through which the REST routes reach the `KB`: its managers for
attributes, conditions, rules, definitions, cases and attribute order, and its `RuleSessionManager`, which owns the one
rule session that may be in progress.

## How the client learns of server changes

The client connects to a web socket on startup and the server pushes:

| Message                | When                                                                                                                    |
|------------------------|-------------------------------------------------------------------------------------------------------------------------|
| cases info             | the processed case list changes, e.g. a case arrives through the REST interpreter                                       |
| cornerstone status     | a rule session starts, a condition is added or removed, a cornerstone is allowed, a comment is renamed during a session |
| rule session completed | a rule is committed or cancelled                                                                                        |
| KB info / KB closed    | a knowledge base is opened, created, copied, renamed or closed through the chat                                         |

Pushing was chosen over polling because the cornerstone review is conversational: the panel must change the moment the
chat says it has, and polling forces a trade-off between that latency and load.

The client also re-fetches the current case after every chat message, since most chat actions change the interpretation
of the case on screen.

## Consequences

- **Ids, not names.** Attributes, conditions and rules are referenced by id everywhere, so renaming is a label change
  and import/export can re-align objects. See [rule_tree_and_inference.md](rule_tree_and_inference.md).
- **One instance per object within a KB.** A loaded KB is one object graph in which equal objects are the same instance,
  so a change made through one manager is seen by every rule and condition that refers to it. Conditions restored from
  the store are re-pointed at the KB's attribute instances (`alignAttributes`).
- **One user at a time.** There is one conversation per server and one rule session per KB. Concurrent users are a
  known non-goal; see [../reviews/design_review_2026-08.md](../reviews/design_review_2026-08.md).
