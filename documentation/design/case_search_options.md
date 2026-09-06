# Case search design

## Copies or references?
Should cases in the case search list be copies of cases from other lists or just references to them?

Here are some things to consider:
1. Cases are not editable, and there is no intention of making them so. This means that the case data can't change in a 
way that invalidates its inclusion in a list of search results.
2. Rules can change, and this may result in a case on a search list no longer satisfying the search conditions that
caused it to be put there.
3. The list to which a case belongs is its `CaseType` in `CaseId`. This is currently a single value. If we did
want to use references rather than copies, then making this multi-valued might be a way of achieving this.
4. Users are likely to search for just a few examples of the cases of interest. It's unlikely that case search
results would cause a big blowout in database size or performance.
5. Copying is a simpler option given the current design.
6. Favourites and Processed cases can be deleted. What would happen to a case on the search list if the 'backing' copy were deleted?

## Session state
As mentioned in the requirements, there is a fair bit of state information that needs to be gathered before
we can start a case search. Also, we don't want to do more than one search at a time or to run a search
while a rule is being built. So maybe a search needs a state interface like the current rule session design.
Also, maybe this needs to be a peer of the rule session. So, for a KB, there is either a rule session
in progress, or a search in progress, but not both.