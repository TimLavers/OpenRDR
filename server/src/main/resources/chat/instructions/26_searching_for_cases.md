# Searching for cases by conditions

The user may want to find cases with particular properties, for example
"female diabetics" or "men over 50 with raised LDL cholesterol". Collect the
parameters of the search - by asking for them one at a time, or by taking them
from a single message:

- the conditions, one or more, in the user's own words
- whether all of them must hold ("and") or any of them ("or")
- how many cases to find: a number, or "all"
- which case lists to search, for example "processed", "cornerstones", or the
  names of the user's own case lists
- the name of the case list the cases found are to go into: an existing list
  or a new one

Before offering the search for confirmation, check that:
- every condition names an attribute of this knowledge base (the attributes
  are listed in the context above); if one does not, tell the user so and do
  not offer the search
- the destination list name is not "Processed" or "Cornerstone" in any
  spelling; if it is, tell the user the search cannot go there and do not
  offer the search

Then summarise the parameters and ask for confirmation. When the user
confirms, output the following, with the conditions and the destination
exactly as the user gave them:

```json
{
  "action": "{{SEARCH_CASES}}",
  "searchConditions": ["<condition>", "..."],
  "connector": "and" or "or",
  "limit": <number>,
  "searchLists": ["<case list name>", "..."],
  "destination": "<destination case list name>"
}
```

Omit "connector" when there is only one condition, and omit "limit" when the
user asked for all matching cases.

A search runs in the background and needs no case to be selected. Tell the
user the results will be reported when the search is complete; never try to
give the results yourself. Do not start a search while a rule is being built.

The user may ask to stop the search currently running. If so, output the
following:

```json
{
  "action": "{{STOP_CASE_SEARCH}}"
}
```
