# Condition Syntax

Most of the conditions required for the Thyroids Knowledge Base (KB) that we are implementing
as part of our first milestone are simple assertions about a single test result. For example:
- `TSH is high`
- `Clinical Notes contains "very tired"`
- `Age > 70`

However, some rules apply to cases with a series of values and the conditions for these rules
are assertions about all of the test values for an attribute. For example:

`all TSH values are within 10% of the upper reference value`

From knowledge of commercial KBs in Thyroids and other clinical domains, we know that multi-episode
conditions are very common and take a variety of forms, such as:
- `all Glucose are normal`
- `no DiabetesStatus is true`
- `at least 3 Glucose are high`
- `at most 2 PSA are high`
- `previous FT3 is normal`

This document describes a design that allows conditions such as these to be implemented.

*The way in which an end user might build or select a condition 
while building a rule is not in the scope of this document.*

## Predicates and positional quantifiers
Multi-value conditions like those shown above have two conceptual components:
- a *predicate* which identifies test results meeting some criterion
- a *positional quantifier* which takes a sequence of `true` and `false` values
   and picks out those of interest.

For example, in `all TSH are normal`, the predicate identifies TSG test results
which are in their normal range. This list of TSH test results:

|TSH (0.5-4.0)|  3.1 | 2.8 | 4.1 |

would produce the sequence 

| true | true | false |

The positional quantifier, `all`, would evaluate this list of booleans as `false`.

For a condition that looks at the most recent test result for an attribute,
the positional quantifier is `current`, which evaluates a sequence of booleans as
`true` if the last item in the sequence is `true`.

Here are the main positional quantifiers:

| Syntax     | Description                              | `true` example   | `false` example |
|------------|------------------------------------------|------------------|-----------------|
| Current    | Looks just at the last value             | F F T            | T F F           |
| Previous   | Looks just at second last value          | F F T            | T F F           |
| All        | True if and only if all are true         | T T T            | T F T           |
| Some       | True if and only if at least one is true | F F F T          | F F             |
| No         | True if and only if all are false        | F F F T          | F F             |
| At least n | True if and only if n or more are true   | F T F T  (n = 2) | F F F T (n = 2) |
| At most n  | True if and only if n or fewer are true  | F F F T  (n = 2) | F F T T (n = 1) |


Note that Positional Quantifier is a really bad name. It is trying to capture the
fact that its evaluation depends on either the number of or the position of the
true values in a sequence. Some alternatives: trace, signature, ...

## Condition objects
A condition has three components:
- an attribute (eg `TSH`)
- a predicate (eg `is normal`)
- a positional quantifier (eg `all`)


Evaluation

Presentation to user
- in the presence of 'all' 'is normal' writes itself as 'are normal'
- Current writes itself as blank