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

### Evaluation
Condition evaluation for a case has three steps:
1. the sequence of test results for the attribute is extracted from the case
2. the predicate is applied to each element of the test results sequence
3. the positional quantifier is applied to the sequence of booleans calculated in 

For example, consider this case:

|                 | 2023-03-11 | 2023-05-01 | 2023-08-16 |
|-----------------|------------|------------|------------|
| TSH (0.5 - 4.0) | 0.03       | 0.09       | 1.2        |
| FT3 (3.0 - 5.5) | 6.1        | 4.3        | 5.5        |
| FT4 (10 - 20)   | 18.0       | 18.0       | 15.3       |
| Sex             |            |            | M          |

`all TSH are normal` evaluates as:

`Case ==[TSH]==> (0.03, 0.09, 1.2) ==[normal]==> (false, false, true) ==[all]==> false`

`Sex is "M"` evaluates as:

`Case ==[Sex]==> (, , M) ==[is "M"]==> (false, false, true) ==[current]==> true`

`no FT3 is low` evaluates as:

`Case ==[FT3]==> ( 6.1, 4.3, 5.5) ==[low]==> (false, false, false) ==[no]==> true`

### Presentation of conditions to users
We can turn condition objects into natural language expressions
by expressing the predicate in a form indicated by the positional quantifier. 
For example, the `normal` predicate is written as `is normal` when combined
with `current` but as `are normal` when combined with `all`. 
So `(TSH, normal, all)` is written as `all TSH are normal` 
whereas `(TSH, normal, none)` is written as `no TSH is normal`. 
The predicate `current` is left unexpressed,
so `(TSH, normal, current)` is written as `TSH is normal`.

## Conditions that cannot be expressed in this format
It's possible to think of assertions that might involve more than one
attribute. For example, `mass/(height * height) > 28`.
The conditions described above would not be able to express these kinds
of calculations. However, some kind of case pre-processor could put the
calculated values as a single attribute into cases, and this attribute
could then be used in simple conditions.

Assertions about the time between episodes in a case cannot be easily
expressed either. One of the rules for the TSH KB adds a comment for
cases where a pattern of nearly normal results occurs over 6 months.
Some thought is needed here....

## Restriction clauses
Some assertions concern only certain episodes in a case, for example
Glucose values where the patient is fasting. ...