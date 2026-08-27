# Case management
RippleDown is a case-based knowledge-acquisition in which users build rules to correct the interpretations of
cases that are presented to them.

## Case lists
Current and future versions of OpenRDR should support the following lists of cases within each Knowledge Base:
1. Cornerstone Cases: The cases that have been used to build rules are retained in the KB and are used to test the effect of new rules.
2. Archived Cases: When cases are sent to a KB for interpretation, the cases are kept so that users can review their interpretations and build rules as necessary.
3. Favourite Cases: Especially interesting cases can be copied for easy access.
   See [favourite_cases.md](favourite_cases.md).
4. Search Results*: To store the results of condition- or comment-based case searches.

 \* Not yet implemented.

## Case editing

*Not yet implemented.*

It is useful to edit cases so that the effects of different values on case interpreation can be assessed. Editing is to
be confined to the Favourite Cases list, which the user curates: the archived cases are the record of what the external
system sent, and the cornerstones are the record of what the rules were built on, so neither may be edited. The intended
behaviour is:

- a case in the Favourite Cases list can be edited, through the chat interface;
- the changed case can be saved under a new name, keeping the original;
- a case in the Favourite Cases list can be renamed.

Copying the current case to the Favourite Cases list **is** implemented; see
[favourite_cases.md](favourite_cases.md), which is the current specification of that list.

## Case deletion
To de-clutter a KB it is sometimes necessary to delete cases.
- It is not possible to delete Cornerstone Cases
- If a case has been copied from one case list to another, then either the original or the copy can be deleted without affecting the other case (except that deleting Cornerstone Cases is not possible).

## Cornerstone cases
Because Cornerstone Cases are used to test the effects of changes to a Knowledge Base, no two Cornerstone Cases should have the
same data. A special instance of this is that if a case is used to build more than one rule, only one copy of it should be
stored as a Cornerstone.




