# Managing the rule-building workflow

The user steps and software components involved in building a rule are as follows;

1. Rule building starts when the user requests a change to the interpretive report of the case currently showing in the `InterpretationView` component
2. When the user confirms the change, a `RuleBuildingSession` is created on the server
3. The user provides natural language "reasons" which the model converts to conditions.
4. At the same time, the first Cornerstone case is shown (if there is one). This gives the opportunity for the user to add further conditions that will not be true for the cornerstone case and so its interpretation will not be affected by the new rule
5. Whenever a condition is added or removed, the cornerstone cases are re-evaluated and the next relevant cornerstone case is shown
6. The user also has the opportunity to just skip a cornerstone case if they approve of the change to its interpretation that will result from the new rule
7. Once all the required conditions have been added, the model user commits the rule

