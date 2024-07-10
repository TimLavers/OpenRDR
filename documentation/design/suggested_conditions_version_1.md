# Suggested conditions - version 1
As discussed in [Building conditions from user hints](building_conditions_from_user_hints.md) 
we would like the user experience of building
rules to include accurate suggestions for conditions based on the change being made to the case,
the values in the case, and the values in the cornerstone case, if there is one.

A first step towards this is a system that generates conditions based on what is in the
case for which the rule is being written and the cornerstone.

## Basic approach
The first approach we will take will work by:
 - enumerating all possible conditions that include the attributes present in the session case
 - excluding those that are not true for the session case
 - sorting the remaining conditions

### Enumeration of all conditions
As explained in our [conditions design](conditions.md) document, there are three classes
of conditions in Open RippleDown. For each of these, it's a fairly simple matter to
enumerate all of the possible conditions that use the attributes available in a case.

#### Episodic conditions
An episodic condition consists of an attribute, a predicate and a signature.
We can build episodic conditions by enumerating across the attributes
 in the session case, the known predicates and the known signatures.
If the session case has just one episode, then it makes sense to restrict
our enumeration of signatures to `current` alone.

#### Series conditions
A series condition consists of an attribute and a series predicate.
We can build series conditions by enumerating across the attributes in the 
session case and the known predicates.
If the session case has just one episode, we can leave series conditions
out when creating suggestions.

#### Case structure conditions
These consist of just a predicate and will be easy to enumerate.

### Ordering of suggestions
The user is most likely to pick suggested conditions that:
- are true for the session case
- are false for the cornerstone case, if there is one
- are false for as many other cornerstone cases as possible
- have previously been used in a rule
- use an attribute that has previously been used in a condition
- are suggested by the rule action
- previously used in a rule with a similar action (high priority for this)
- in a rule building session some conflicting cornerstones get exempted (the user has indicated that the change is good 
for the case). We should give precedence to conditions that are true for these.