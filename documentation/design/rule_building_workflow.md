# Managing the rule-building workflow

The user steps and software components involved in building a rule are as follows;

1. Rule building starts when the user changes the interpretive report of the case currently showing in the `InterpretationView` component
2. The list of changes made to the report are stored in the `Interpretation` as a `DiffList` which is persisted by the server for this case
3. From the `DiffViewer` component, the user can see the list of `Diff` items (i.e. report changes) and selects one to build the rule on
4. When the user clicks the `Build Rule` button, a `RuleBuildingSession` is created on the server
5. The `ConditionSelector` component is then shown and the user can select the conditions associated with the rule
6. At the same time, the first Cornerstone case is shown (if there is one). This gives the opportunity for the user to add further conditions that will not be true for the cornerstone case and so its interpretation will not be affected by the new rule
7. Whenever a condition is added or removed, the cornerstone cases are re-evaluated and the next relevant cornerstone case is shown
8. The user also has the opportunity to just skip a cornerstone case if they approve of the change to its interpretation that will result from the new rule
7. Once all the required conditions have been added, the user commits the rule from the `ConditionSelector` screen

