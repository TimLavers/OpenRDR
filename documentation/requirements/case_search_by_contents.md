# Case search
In OpenRDR a user can search for cases that meet particular criteria of interest.
These criteria are expressed as the sorts of conditions used in building rules:
statements that are either true or false for a case. However, whereas a rule contains
conditions that must all be satisfied for a rule to apply, case search can be
either by conjunction or disjunction of individual conditions.

## Case search list
From the user's point of view, when a case search is run, the cases that are found
are copied into a 'Search results' case list. 

__There are two design options here: either to literally copy the cases or to
keep references to them and populate the search result list using the references.__

## Case search options
The following options will be available:
- the number of cases to be found (some number or all)
- where to search (processed cases, cornerstones, favourites)
- whether to clear the existing search cases
- what conditions to search by
- whether the search conditions are to be 'and-ed' or 'or-ed'.

Note that we don't allow the option to search the search case list itself because this could be
confusing, especially when combined with the option of deleting the existing cases on the search
list. This could be allowed so that the user can refine the results of an existing search
if that were of value. To do so would require that any removal of existing cases occurred after the
search had completed.

__Should it be possible to search the search results?__

## Session state
Two case searches should not run at the same time. Nor should it be possible for 
a case search to run at the same time that a rule is being built. Moreover, the
system needs a complete list of the options for a search before starting the search.
The case search tool will be run through the chat interface, so there may need to be
some back-and-forward between the chatbot and the user to gather all of this information.


