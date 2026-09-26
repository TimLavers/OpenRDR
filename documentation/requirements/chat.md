# The chat

The chat is the user interface for everything that changes the knowledge base. It exists to replace GUI controls, not
to sit beside them: a new or infrequent user can say what they want in their own words and language, and the model
works out what they mean. The GUI panels (case list, case view, cornerstone, comments, derived attributes, report) show
state; the chat carries intent. Rules for when an inline control is acceptable are in
[design/chat_ui_guidelines.md](../design/chat_ui_guidelines.md).

## Capabilities

The chat's own catalogue, returned when the user asks what it can do, is the authoritative list
(`chat/List chat capabilities.feature`). It is grouped as:

| Group              | Operations                                                                                                                                                                                            | Requirements                                                     |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| Knowledge bases    | list, open, create, close, delete, rename; show or change the description; open a named copy of a demonstration; import from and export to a ZIP; add a demonstration case to an empty knowledge base | [knowledge_bases.md](knowledge_bases.md)                         |
| Report comments    | add, remove or replace a comment with a rule; insert a case value into a comment with `{name}`                                                                                                        | [comments.md](comments.md), [rule_building.md](rule_building.md) |
| Derived attributes | assign, remove or replace a derived value with a rule; edit a definition everywhere                                                                                                                   | [derived_attributes.md](derived_attributes.md)                   |
| Building a rule    | suggested reasons; add, list or remove reasons; review cornerstones; cancel; undo the last rule                                                                                                       | [rule_building.md](rule_building.md)                             |
| Names and layout   | rename a comment or derived attribute; rename a condition's phrase; reorder the attributes                                                                                                            | [cases_and_attributes.md](cases_and_attributes.md)               |
| Favourite cases    | copy the current case to favourites, optionally renamed; delete it from favourites                                                                                                                    | [cases_and_attributes.md](cases_and_attributes.md)               |

Without an open case only the knowledge-base group is offered, since nothing else has a subject.

## Behaviour

| Requirement              | Description                                                                                                                                                                                                                                                             | Validation                                             |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| Conversation per context | A conversation starts when a knowledge base is opened or closed or a case is selected, and the greeting says what can be done next. With no knowledge base the greeting offers to create one or open a demonstration; with an empty one it offers a demonstration case. | `kb/Knowledge Base Management.feature`                 |
| Any language             | The user may write in any language the model understands; names and comment texts are taken as written.                                                                                                                                                                 | `chat/Build rules using non-English languages.feature` |
| Server decides           | Every change is validated by the server; the model never rewrites a name, a comment, an expression or a condition, and anything irreversible is confirmed in words.                                                                                                     | `chat/*.feature`                                       |
| Refusals explain         | A request that cannot be carried out (a clashing name, an unparseable reason, a cycle) is refused with the cause, and the state is unchanged.                                                                                                                           | `chat/*.feature`, `inferencing/*.feature`              |
| Voice                    | A microphone button records an utterance and transcribes it into the input field for the user to review and send. Transcription is one-shot: text appears after the recording stops.                                                                                    | `voice/voice.feature`                                  |
| Suggestion chips         | Suggested conditions are offered as chips under the message; clicking one sends its text as an ordinary message.                                                                                                                                                        | `conditions/Suggested Conditions.feature`              |
| Knowledge base list      | The list of knowledge bases is rendered as clickable rows; clicking sends `Open <name>`.                                                                                                                                                                                | `kb/Knowledge Base Management.feature`                 |
| Capability card          | The capability list is rendered as a scrollable card with headings.                                                                                                                                                                                                     | `chat/List chat capabilities.feature`                  |
| Comment variable tip     | A one-line tip about `{name}` variables is shown once per conversation, the first time a comment is added.                                                                                                                                                              | `comments/Help for comments with variables.feature`    |

**Not implemented:** streaming of the model's reply; live transcription while speaking; remembering the chat panel's
width; help on a specific topic. See `tickets/TODO.md`.
