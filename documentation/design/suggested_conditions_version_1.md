# Suggested conditions - version 1
As discussed in [Building conditions from user hints](building_conditions_from_user_hints.md) 
we would like the user experience of building
rules to include accurate suggestions for conditions based on the change being made to the case,
the values in the case, and the values in the cornerstone case, if there is one.

## Basic approach
The first approach we will take will work by:
 - enumerating all possible conditions that include the attributes present in the session case
 - excluding those that are not true for the session case
 - excluding those that are true for trivial reasons or that are otherwise irrelevant
 - sorting the remaining conditions

### Enumeration of all conditions
As explained in our [conditions design](conditions.md) document, there are three classes
of conditions in Open RippleDown. For each of these, it's a fairly simple matter to
enumerate all of the possible conditions that use the attributes available in a case.

#### Episodic conditions
An episodic condition consists of an attribute, a predicate and a signature.
We can build episodic conditions by enumerating across the attributes
 in the session case, the known predicates and the known signatures.
We can restrict the list of signatures to match the number of episodes in the case.
For example, if the session case has just one episode, then it makes sense to restrict
the signatures to `current` alone.

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

## Which conditions should be suggested?
In a rule session, the user adds conditions that describe the
kinds of cases for which the rule is to apply.
These conditions need to be true for the case for which the rule is being built.
Some suggested conditions cannot be changed by the user, for example, `TSH is high`.
These should be provided as suggestions if and only if they are true 
for the case.
Other conditions can be edited, for example `Notes is "_"`.
We should provide these as suggestions if they can be edited
in a way that makes them true for the session case.

There are some potential suggestions that, though editable, could never
result in a condition that is true for the case.
If the session case has a current `TSH` value that is not a number,
then `TSH ≥ _` should not be suggested. 

Therefore, our algorithm for deciding whether a condition should be suggested is:
- if it is not editable, suggest it if it applies to the session case
- if it is editable, suggest it unless we know that there is no way of editing it so that it applies to the session case.

## Editable suggested conditions
For a condition like `TSH ≥ _`, it only makes sense for one version of that
condition to be used in a rule. By contrast, we might add both
`Clinical Notes contains "Emergency Room"` and 
`Clinical Notes contains "mania"` in a rule.
Editable suggestions get treated differently depending
on this property.