# Case management
RippleDown is a case-based knowledge-acquisition in which users build rules to correct the interpretations of
cases that are presented to them.

## Case lists
Current and future versions of OpenRDR should support the following lists of cases within each Knowledge Base:
1. Cornerstone Cases: The cases that have been used to build rules are retained in the KB and are used to test the effect of new rules.
2. Archived Cases: When cases are sent to a KB for interpretation, the cases are kept so that users can review their interpretations and build rules as necessary.
3. Favourite Cases*: Especially interesting cases can be copied from the Archived Cases list for easy access.
4. Search Results*: To store the results of condition- or conclusion-based case searches.

 \* Not yet implemented.

## Case editing
It is useful to edit cases so that the effects of different values on case interpreation can be assessed. The following restrictions apply:
- It is not possible to edit cases that are in the Favourite Cases list.
- It is not possible to copy cases from the other case lists to the Favourites list.
- When editing a Favourites case, it is possible to save the changed case with a new name and keep the original version of the case.
- It is possible to rename a Favourites case.

## Case deletion
To de-clutter a KB it is sometimes necessary to delete cases.
- It is not possible to delete Cornerstone Cases
- If a case has been copied from one case list to another, then either the original or the copy can be deleted without affecting the other case (except that deleting Cornerstone Cases is not possible).

## Cornerstone cases
Because Cornerstone Cases are used to test the effects of changes to a Knowledge Base, no two Cornerstone Cases should have the
same data. A special instance of this is that if a case is used to build more than one rule, only one copy of it should be
stored as a Cornerstone.




