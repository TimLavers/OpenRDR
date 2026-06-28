# Rule Building Requirements
When an expert is reviewing the interpretative report for a case, if they judge that for this case, parts of the report are inappropriate, they
can add, remove or replace sentences in that report. In the chat-based UI, the expert does this by asking the chatbot to
add, remove or replace a comment;
the proposed change to the report is shown, and the user can then decide to add a rule to the knowledge base for that
change. In this way, they teach the system
how to generate the approved report for this case and other cases like this one.
The addition, removal or replacement of a sentence is called the `rule action`, namely, the change that the rule will make to the report when it is applied to a case.

When creating a rule, the user reviews the `cornerstone cases` affected by the rule. These are the cases on which
previous rules
were based and which will now also have a changed report once the current rule is added. Put another way, the cornerstone cases are those which satisfy 
the conditions of the new rule and so the rule action will also apply to them.

The changed report may still be appropriate for that cornerstone case, and if so,
the user can then review the next cornerstone case. However, if the changed report is not appropriate for that
cornerstone case, the user can then add a further
condition to the current rule which will be false for the cornerstone case and so exclude that case from the rule action. Once this is done, the interpretive report
for that cornerstone case will remain unchanged.

