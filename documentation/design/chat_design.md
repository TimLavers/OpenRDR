## What do we want to achieve?

When viewing a case and its report, if the report is not appropriate for the case, the actions available to the user
are:

- Add another comment (new or existing)
- Remove a comment
- Replace a comment by another one (new or existing)
- Undo the last rule

The user then has to provide the justification for an Add, Remove or Replace action which is one or more boolean
expressions involving the
case attributes and values which must evaluate as true for this case.

The user then has to review the cornerstone cases and decide whether the changed report is appropriate for them. If not,
the user can add a further condition to the current rule which will be false for the cornerstone case and so exclude
that case from the rule action. Once this is done, the interpretive report for that cornerstone case will remain
as it was, that is, unaffected by the new rule.

GUI controls have been provided to do all this. However, the application could be enhanced by providing a more
user-friendly interface which is a chat window. In that window the user can simply say how they want the report to
change, and the justification for this change. The user's request will then be given to an LLM which may ask the user
further questions to clarify exactly what is meant, and will then provide a structured output to the backend of the
application which will then automatically add the rule that effects the report change requested by the user.

This avoids the user having to learn how to use the GUI controls, which may be particularly helpful for new or
infrequent users. It also allows the user to enter their request in their own words, or own language, which may be more
natural and
intuitive than using a formal rule-building workflow.

## Design options

### Multiple LLM types?

We are already using the Gemini LLM to perform the task to translate a user-entered justification into a condition class
used in the rule that is being added.

The options for the chat LLM are:

1. Use Gemini again, or
2. Use a different LLM which may be better suited for the chat task, e.g. Claude or even a non-cloud implementation of
   Llama.

We have chosen the simplest option which is to stay with Gemini as the API is already in place for the translation of
user expressions into the corresponding condition syntax.
See [Google Generative AI](https://github.com/PatilShreyas/generative-ai-kmp/).

### Combining the rule addition chat task with the condition translation task?

We have chosen to keep the two tasks separate. The chat LLM will focus on gathering the rule information and structuring
it, while the existing LLM instance will handle the translation of the justification into a formal condition.

There may be some advantage for the translation task if the chat LLM is also used for the translation task, as it may be
able to use the context of the conversation to improve its understanding of the user's intent. However, this would
require additional complexity in the design of the system. We can consider combining the two tasks if we find
that the current approach needs to be improved.

### Interfacing the LLM with the backend or frontend?

We have chosen to interface the chat LLM with the backend, as was done with the LLM used for the translation task.
This allows us to use the structured output from the chat LLM directly in the backend to add the rule to the knowledge
base. This avoids complicating the frontend with additional logic and API calls to instruct the backend on how to add
the rule.

See [ChaKt-KMP]("https://github.com/PatilShreyas/ChaKt-KMP") for an example of a chat LLM interfaced directly with the
frontend. A nice feature of ChatKt - KMP implementation is that it uses a streaming response (i.e. Flow) from the model
which updates the UI as the words or phrases are generated.

## Prompt design for a chat facility

### Information to be passed to the chat LLM

The chat LLM will need to be provided with the following minimum information:

- the user's request
- the conversation history
- the current report comments (if any). This could be achieved by passing JSON string representing the current case and
  its interpretive report - a ViewableCase object.
- all the available comments in the Knowledge Base that could be used in the report

### Conversation design

The chat LLM will operate in a multi-turn chat environment following the sequence below:

1. the user selects a case
2. the conversation service starts a chat with the case data as part of the system instruction
3. the model asks the initial question to the user (e.g. "what change do you want to make to the report?")
4. the user enters their request
5. the model asks for confirmation of the intended report action, or else asks to clarify that action
6. the user confirms or clarifies the action
7. the model asks for the reason for the action (i.e. the conditions that must be true for the case)
8. the user enters the reason
9. the model confirms the reason, or else responds with a question to clarify that reason
10. once both the action and reason are confirmed, the model outputs a structured JSON response to the backend
11. the backend processes the JSON and adds the rule to the knowledge base

## UI design

The UI design will be a simple chat panel with the following features:

- a text entry box for the user to enter their request
- a display of the conversation history, with the model responses on the left and the user responses on the right

The chat panel will be hidden by default, but can be shown by the user by clicking an icon on the application bar.
Showing the chat panel will automatically start the chat session with the model.

