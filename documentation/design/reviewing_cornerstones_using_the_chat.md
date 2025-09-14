# Reviewing cornerstones using the chat interface

## What do we want to achieve?

We want to allow the user to review the cornerstones of a case using the chat interface, rather than having to use the
UI components to do this review. The user actions for each cornerstone case are:

- allow the report of the cornerstone case to change once the new rule is added, or
- add further conditions to the rule to exclude the cornerstone case from the rule action, or
- cancel the review of the cornerstone cases

## Conversation design

1. The user defines the report change.
2. If they don’t want to add any conditions, the rule is built immediately.
3. If they do add conditions, then say they are all done, a rule session is started.
4. The system then gives the model information on how many cornerstones there are.
5. If there are no cornerstones, the model then sends an action message to the server to just build the rule using the
   session that was already started.
6. If there are cornerstones, the model leads the user through the review of each cornerstone.
7. Once there are no more cornerstones to review, the model sends an action message to build the rule.
