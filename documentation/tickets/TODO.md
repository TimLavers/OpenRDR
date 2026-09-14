**External (primary) attributes**

- Provide the facility for the user to create and set the value of an external attribute (for the purposes of creating a
  case scenario on which to build rules)
- Allow the user to rename an external attribute
- Allow the user to see the external name -> name mapping
- Allow the user to see the list of all external attributes

**Derived attributes**

- Allow the user to edit the formula or value of a derived attribute, as distinct from adding/removing/replacing it with
  a rule
- Allow the user to delete a derived attribute
- Allow the user to rename a derived attribute (id-referenced so mechanically safe; needs name-in-use refusal and a chat
  action)
- Show a lightweight impact summary (e.g. count of affected cornerstones) when the definition of a derived attribute is
  edited globally
- Allow the user to see the list of all derived attributes, whether or not they are given for the case
- Allow the user to set the number of significant digits for a derived attribute for the project, rather than
  hard-coding it to 4

**Rule building**

- Provide the facility for the user to ask for help with building a rule
- Write a cuke for the scenario where the user tries to cancel a rule when there is none in progress
- Don't ask for confirmation of a condition if it is exactly the same expression that the user entered
- Allow the user to set the rule action and add a condition at the same time, e.g. Add the comment "go to the beach"
  because the sun is hot

**Comments**

- The user should be able to get the model to list all comments
- The user should be able to edit a comment
- Show ancestor rules differently to the leaf rule
- Consider grouping comments (e.g. GP comments, Specialist comments) so that a single rule can remove a whole group.
  Motivating example: many prescriptive comments drive a detailed report to a GP, but a rule detecting that the
  referring doctor is a specialist should suppress all of them in favour of a single "Specialist management noted"
  comment. Today this needs one removal rule per comment. Design after repeat inferencing Phase 2 lands — options
  include a group tag on COMMENT attributes plus a bulk-remove rule action, or a rule-given "report scope" signal
  honoured by the AI report generator.

**Voice**

- stream the model's response to the user
- stream the user's voice input to the model

**Chat panel**

- Flash the chat panel when the user sets focus on it
- Remember the user-set width of the chat panel
- Investigate whether it's a good idea to start a new conversation for each rule
- For confirmation responses required by the user, add two buttons: one to confirm, one to cancel, so the user does not
  have to type the confirmation.
- Similarly for "allow" and "do not allow" when reviewing cornerstone cases.

**Testing**

- Run all tests in github actions for each push

**Help**

- Provide the user with the ability to get help with a specific aspect of the system
- Is this done via the system prompt or some mcp interface to a Help document?

**Miscellaneous**

- convert to multiplatform

**Bugs**

- Start to build a rule for a derived attribute BMI, then cancel. You cannot re-start the rule builder for that
  attribute as it says "BMI is already in use"

