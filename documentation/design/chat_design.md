## What do we want to achieve?

When viewing a case and its report, if the report is not appropriate for the case, the actions available to the user
are:

- Add another comment (new or existing)
- Remove a comment
- Replace a comment by another one (new or existing)

The user then has to provide the justification for that action which is one or more boolean expressions involving the
case attributes and values which must evaluate as true for this case.

The user then has to review the cornerstone cases and decide whether the changed report is appropriate for them. If not,
the user can add a further condition to the current rule which will be false for the cornerstone case and so exclude
that case from the rule action. Once this is done, the interpretive report for that cornerstone case will remain
unchanged.

GUI controls have been provided to do all this. However, I want to enhance this application by providing a more
user-friendly interface which is a chat window. In that window the user can simply say how they want the report to
change, and the justification for this change. The user's request will then be given to an LLM which may ask the user
further questions to clarify exactly what is meant, and will then provide a structured output to the backend of the
application which will then automatically add the rule that effects the report change requested by the user.

This avoids the user having to learn how to use the GUI controls, which may be particularly helpful for new or
infrequent users. It also allows the user to enter their request in their own words, which may be more natural and
intuitive than using a formal rule-building workflow.

## Design options

### Multiple LLM types?

We are already using the Gemini LLM to perform the task to translate a user-entered justification into a condition class
used in the rule that is being added.

The options for the chat LLM are:

1. Use Gemini again, or
2. Use a different LLM which may be better suited for the chat task

We have chosen the simplest option which is to stay with Gemini as the API is already in place.
See [Google Generative AI](https://github.com/PatilShreyas/generative-ai-kmp/).

If there are problems with this approach, we can experiment with a different LLM at that time.

### Combining the rule addition chat task with the condition translation task?

We have chosen to keep the two tasks separate. The chat LLM will focus on gathering the rule information and structuring
it, while the existing LLM instance will handle the translation of the justification into a formal condition.

There may be some advantage for the translation task if the chat LLM is also used for the translation task, as it may be
able to use the context of the conversation to improve its understanding of the user's intent. However, this would
require additional complexity in the design of the system. Again, we can consider combining the two tasks if we find
that the current approach is not working well.

### Interfacing the LLM with the backend or frontend?

We have chosen to interface the chat LLM with the backend, as was done with the LLM used for the translation task.
This allows us to use the structured output from the chat LLM directly in the backend to add the rule to the knowledge
base. This avoids complicating the frontend with additional logic and API calls to instruct the backend on how to add
the rule.

See [ChaKt-KMP]("https://github.com/PatilShreyas/ChaKt-KMP") for an example of a chat LLM interfaced directly with the
frontend.

## Prompt design for a chat facility

### Information to be passed to the chat LLM

The chat LLM will need to be provided with the following minimum information:

- the user's request
- the conversation history
- the current report comments
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

### Prompt design suggestions from an LLM

--- THE FOLLOWING TO BE REMOVED ONCE THE INITIAL PROMPT DESIGN HAS BEEN FINALISED---

The following design suggestions for the chat LLM prompt were provided by Grok in response to the task below:

#### Task for Grok

I have developed an application that allows a user to build their own knowledge base, where each knowledge base
generates a report consisting of a set of comments appropriate for a given case.

A case is a set of attribute/value pairs organised into episodes where an episode is the set of values for a specific
date.

When viewing a case and its report, if the report is not appropriate for the case, the actions available to the user are
to add another comment (new or existing), remove a comment, or replace a comment by another one. A replace action is
treated by the backend as a remove followed by an add.

After the user chooses the action, they must provide the justification for that action which is one or more boolean
expressions involving the case attributes and values which must evaluate as true for this case.

Rules are always added to the knowledge base, never amended, as per the rippledown rule building methodology.

Attached is a screen shot of the main screen of the application for a TSH knowledge base showing a comment generated by
rules for this case. Knowledge bases for other domains have a similar UI.

The tool tip over the comment shows the boolean conditions which are the reasons why this comment has been given for
this case.
In the example shown, there are 2 conditions: "TSH is normal" and "Free T4 is low by at most 20%"

In the application, there is already functionality to use an LLM (Gemini) to translate a user-entered justification (in
any language) into a json format which is then used to create the condition classes used in the rule that is being
added.

However, rather than requiring the user to learn the GUI controls for specifying the rule action (add, remove or replace
a comment) and the formal rule-building workflow, I want to enhance this application by providing a more user-friendly
interface which is a chat window.

In that window the user can simply say how they want the report to change, and the justification for this change.

The user's request will then be given to an LLM (either the same LLM or different to the one used to translation the
user entered justification into formal conditions) which may ask the user further questions to clarify exactly what is
meant, and will then provide a structured output to the backend of the application which will then automatically add the
rule that effects the report change requested by the user.

So my question is, how do I engineer a set of prompts for the LLM that will do all this?

#### Grok's response

To enhance your TSH knowledge base application with a chat-based interface that allows users to modify report comments
using natural language, you need to engineer a set of prompts for a Large Language Model (LLM) that can interpret user
requests, clarify ambiguities, and generate structured outputs for your application's backend. Below is a comprehensive
guide to designing these prompts, based on your application’s requirements and the screenshot provided.

#### Understanding the Goal

Your application generates reports with comments (e.g., "A mildly reduced F4 with a normal TSH may be due to
non-thyroidal illness or pituitary hypothyroidism") based on case attributes (e.g., TSH = 0.74 mU/L, Free T4 = 8 pmol/L)
and existing rules. Users want to modify these reports by:

- Adding a new comment.
- Removing an existing comment.
- Replacing an existing comment (treated as a remove followed by an add).

Each action requires a justification in the form of boolean expressions (e.g., "TSH is normal," "Free T4 is low by at
most 20%") that must be true for the case. The LLM should:

- Interpret the user’s intent and identify the action and comment(s) involved.
- Collect the justification in natural language.
- Ask clarifying questions if the request is ambiguous.
- Output a structured JSON response for the backend to process.

Since your system already uses an LLM (Gemini) to translate justifications into formal conditions, the conversational
LLM can focus on gathering information and structuring it, leaving the translation to the existing functionality.

#### Prompt Design strategy

The LLM will operate in a multi-turn chat environment, where it must:

- Maintain context across the conversation.
- Leverage the current report’s comments (provided as input) to resolve references like "the current comment."
- Produce a JSON output once all required information is gathered.

To achieve this, I’ll design a single, flexible prompt template that includes:

- Instructions for interpreting user requests.
- Guidelines for asking clarification questions.
- A format for the structured output.
- Context about the current case and report.

The system will update the conversation history with each user message and LLM response, allowing the LLM to track
progress and decide when to output the JSON.

#### The Prompt Template

```plaintext
You are an assistant helping a user modify a medical knowledge base report through a chat interface. The user is currently viewing a case and its generated report, which consists of one or more comments. The user wants to change the report by adding, removing, or replacing comments, and they must provide a justification for this change in natural language.

Your task is to interpret the user's requests, ask for clarifications if needed, and provide a structured output for the backend to add a rule to the knowledge base.

### Possible Actions
- **add**: Add a new comment to the report.
- **remove**: Remove an existing comment from the report.
- **replace**: Replace an existing comment with a new comment (treated as removing one comment and adding another).

### Justification
The justification is a set of conditions based on the case attributes (e.g., TSH, Free T4, Age) that must be true for the current case. The user will provide this in natural language (e.g., "TSH is normal" or "the patient is young"). Another system component will translate it into formal boolean expressions, so you should collect it as provided without formalizing it.

### Conversation History
Below is the conversation history so far:

[Conversation history]

### Current Report Comments
The current report contains the following comments:
[List of current comments in the report]

### Your Role
Based on the conversation history and the current report comments:
1. **Identify the Action**: Determine if the user wants to add, remove, or replace a comment. If unclear, ask, "Do you want to add, remove, or replace a comment?"
2. **Determine the Comment(s)**:
   - For **add**, identify the comment to be added. If not specified, ask, "Which comment do you want to add?"
   - For **remove**, identify the comment to be removed. If ambiguous (e.g., "the comment" when there are multiple), ask, "Which comment do you want to remove? Please specify the comment text or describe it."
   - For **replace**, identify both the comment to be removed and the new comment to be added. If not specified, ask, "Which comment do you want to replace, and what should it be replaced with?"
3. **Collect the Justification**: Extract the justification from the user’s input. If missing or vague, ask, "What is the justification for this change?"
4. **Clarify Ambiguities**: If any part of the request is unclear, ask a specific question to gather the missing information.
5. **Output Structured Response**: Once you have the action, comment(s), and justification, provide the output in JSON format with these fields:
   - **action**: "add", "remove", or "replace"
   - **comment_to_add**: The comment to add (for "add" or "replace"), or null if not applicable
   - **comment_to_remove**: The comment to remove (for "remove" or "replace"), or null if not applicable
   - **justification**: The justification in natural language

### Guidelines
- Use the current report comments to resolve references (e.g., "the current comment" refers to an existing comment).
- If the user specifies a comment not in the report for removal, assume they mean a different action (e.g., "remove X" might imply "add" if X isn’t present) and clarify accordingly.
- Keep questions concise and focused on missing information.
- Output the JSON only when all required fields are confidently filled.

### Response
Based on the conversation so far, respond with either:
- A question to clarify the user’s request, or
- The structured JSON output if all information is complete.
```

#### Example 1: Adding a Comment

- User: "I want to add another comment because the patient is young."
- Prompt Input:

```markdown
Conversation History:
User: I want to add another comment because the patient is young.

Current Report Comments:

- "A mildly reduced F4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism."
```

- LLM Response: "Which comment do you want to add?"
- User: "Consider repeating the test in 6 weeks."
- Updated Prompt Input:

```markdown
Conversation History:
User: I want to add another comment because the patient is young.
Assistant: Which comment do you want to add?
User: Consider repeating the test in 6 weeks.

Current Report Comments:

- "A mildly reduced F4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism."
```

- LLM Response

```json
{
  "action": "add",
  "comment_to_add": "Consider repeating the test in 6 weeks.",
  "comment_to_remove": null,
  "justification": "the patient is young"
}
```

#### Example 2: Replacing a Comment

-User: "Replace the current comment with 'Check for secondary hypothyroidism' because TSH is normal but Free T4 is low."

- Prompt Input

```markdown
Conversation History:
User: Replace the current comment with 'Check for secondary hypothyroidism' because TSH is normal but Free T4 is low.

Current Report Comments:

- "A mildly reduced F4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism."
```

- LLM Response (since there’s only one comment, "the current comment" is clear)

 ```markdown
{
"action": "replace",
"comment_to_add": "Check for secondary hypothyroidism",
"comment_to_remove": "A mildly reduced F4 with a normal TSH may be due to non-thyroidal illness or pituitary
hypothyroidism.",
"justification": "TSH is normal but Free T4 is low"
}
```

#### Example 3: Ambiguous Request

- User: "Change the report."
- Prompt Input:

```markdown
Conversation History:
User: Change the report.

Current Report Comments:

- "A mildly reduced F4 with a normal TSH may be due to non-thyroidal illness or pituitary hypothyroidism."
```

- LLM Response: "Do you want to add, remove, or replace a comment?"

#### Handling Edge Cases

- Multiple Comments: If the report has multiple comments (unlike the screenshot’s single comment), the LLM asks for
  clarification when the user says "the comment" (e.g., "Which comment do you want to remove?").
- Vague Justifications: If the user says "because of the results," the LLM asks, "What is the justification for this
  change?"
- Incorrect References: If the user tries to remove a comment not in the report, the LLM might ask, "Did you mean to add
  this comment instead?"

#### Integration with Your System

- Input: The backend provides the current report comments and updates the conversation history.
- Output: The LLM’s JSON is sent to the backend, where the justification field is passed to the existing Gemini LLM for
  translation into formal conditions.
- State Management: The chat system maintains the conversation history, ensuring context persists across turns.

#### Why This Works

- Clarity: The prompt explicitly defines actions, fields, and clarification steps.
- Flexibility: It handles single- and multi-turn conversations.
- Context-Aware: Including report comments resolves references naturally.
- Separation of Concerns: The conversational LLM focuses on intent and data collection, leveraging your existing
  justification translator.

You can test this prompt with your LLM, adjusting the wording or adding examples (e.g., few-shot learning) if it
struggles with complex requests. This design should streamline your users’ experience, replacing GUI complexity with a
natural, chat-based workflow.