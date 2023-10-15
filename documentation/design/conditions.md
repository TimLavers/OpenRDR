# Condition Syntax

Most of the conditions required for the Thyroids Knowledge Base (KB) that we are implementing
as part of our first milestone are simple assertions about a single test result. For example:
- `TSH is high`
- `Clinical Notes contains "very tired"`
- `Age > 70`

However, some rules apply to cases with a series of values. The conditions for these rules
are assertions about the sequence of boolean values obtained by applying a predicate to 
each of the test values for an attribute. For example:

`all TSH values are within 10% of the upper reference value`

From knowledge of commercial KBs in Thyroids and other clinical domains, we know that multi-episode
conditions are very common and take a variety of forms, such as:
- `all Glucose are normal`
- `no DiabetesStatus is true`
- `at least 3 Glucose are high`
- `at most 2 PSA are high`
- `previous FT3 is normal`

Conditions such as these will be called _episodic conditions_. As we will see below, 
they include single episode conditions like those in the first set of examples above.

A further kind of condition is one that applies a predicate to the entire sequence
of test values for an attribute. For example:
- `TSH is increasing`
- `maximum BMI < 18.0`

These kinds of conditions will be called _series conditions_.

This document describes a software design that handles both these kinds of condition.

*The way in which an end user might build or select a condition 
while building a rule is not in the scope of this document.*

## Episodic conditions
### Test Result Predicates and Signatures
Episodic conditions like those shown above have two conceptual components:
- a *test result predicate* which identifies test results meeting some criterion
- a *signature* which takes a sequence of `true` and `false` values
   and picks out those of interest.

For example, in `all TSH are normal`, the predicate identifies TSH test results
which are in their normal range. This list of TSH test results:

`|TSH (0.5-4.0)|  3.1 | 2.8 | 4.1 |`

would produce the sequence 

`| true | true | false |`

The signature, `all`, would evaluate this list of booleans as `false`.

For a condition that looks at the most recent test result for an attribute,
the signature is `current`, which evaluates a sequence of booleans as
`true` if the last item in the sequence is `true`.

Here are the main signatures:

| Syntax     | Description                              | `true` example   | `false` example |
|------------|------------------------------------------|------------------|-----------------|
| Current    | Looks just at the last value             | F F T            | T F F           |
| Previous   | Looks just at second last value          | F F T            | T F F           |
| All        | True if and only if all are true         | T T T            | T F T           |
| Some       | True if and only if at least one is true | F F F T          | F F             |
| No         | True if and only if all are false        | F F F T          | F F             |
| At least n | True if and only if n or more are true   | F T F T  (n = 2) | F F F T (n = 2) |
| At most n  | True if and only if n or fewer are true  | F F F T  (n = 2) | F F T T (n = 1) |

### Episodic Condition objects
An episodic condition has three components:
- an attribute (eg `TSH`)
- a test result predicate (eg `is normal`)
- a signature (eg `all`)

### Evaluation
Condition evaluation for a case has three steps:
1. the sequence of test results for the attribute is extracted from the case
2. the test result predicate is applied to each element of the test results sequence from step 1
3. the signature is applied to the sequence of booleans from step 2

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

### Presentation of episodic conditions to users
We can turn episodic condition objects into natural language expressions
by expressing the predicate in a form that takes into account the 
plurality indicated by the signature. 
For example, the `normal` predicate is written as `is normal` when combined
with `current` but as `are normal` when combined with `all`. 
So `(TSH, normal, all)` is written as `all TSH are normal` 
whereas `(TSH, normal, none)` is written as `no TSH is normal`. 
The predicate `current` is left unexpressed,
so `(TSH, normal, current)` is written as `TSH is normal`.

### Restriction clauses
Some assertions concern only certain episodes in a case, for example
those where the patient is fasting, or those within the last few years.
A "restriction clause" concept could be introduced to allow the expression
of such conditions. Such a restriction clause would be a predicate
`Episode => Boolean`
that would be used to filter out those Episodes in a case 
that did not satisfy the predicate.  In the condition 
`all glucose are normal, where collection_type is "fasting"`
the restriction clause would represent the part after the comma. 
The evaluation of a condition with a restriction clause would
be just like the evaluation of a regular condition, but with the 
preliminary step of producing a "cut-down" version of the input
case.

In the TSH case above, the condition

`all TSH are low, where FT4 > 16.0`

would be evaluated as follows:

`Case ==[FT > 16.0]==> Case with just first 2 episodes ==[TSH]==> ( 0.03,0.09) ==[low]==> (true, true) ==[all]==> true`

## Series Conditions
A series condition will contain two sub-objects:
- an `Attribute` of interest,
- a `SeriesPredicate` that makes an assertion about the values in a case of the `Attribute`.

The evaluation of a `SeriesCondition` has the following steps:
1. the sequence of test results for the attribute is extracted from the case
2. the series predicate is applied to the test results sequence from step 1
For example, the condition

`TSH is increasing`

would be evaluated as follows:
`Case ==[TSH]==> (0.03, 0.09, 1.2) ==[Increasing]==> true`

A restriction clause could be applied in the evaluation of series conditions
if required.

## Case predicates that cannot be expressed as episodic or series conditions
It's possible to think of assertions that might involve more than one
attribute. For example, `mass/(height * height) > 28`.
These kinds of calculations won't be expressible as conditions like
those we've defined above. However, some kind of case pre-processor could put the
calculated values as a single attribute into cases, and this attribute
could then be used in simple conditions.

Assertions about the time between episodes in a case cannot be easily
expressed either. One of the rules for the TSH KB adds a comment for
cases where a pattern of nearly normal results occurs over 6 months.
Some thought is needed here.

### Cases that don't involve episodes
Certain knowledge domains have cases that don't require the concept of an episode.
These include:

#### Text analysis
The case consists of a single text blob that needs to be interpreted. 
One could perhaps use machine learning to extract features (i.e. attributes), 
and then RD to build rules on these features. 
But it may be appropriate to use RD 
also for the feature extraction, i.e. on the raw data

#### Sets of related test results 
The case consists of sets of related attributes, e.g. IGE values 
testing for various type of allergies. 
The conditions need to consider these attributes as sets.

#### Multi-valued attributes
The case consists of multi-valued attributes. A good example is microbiology, 
where there is a hierarchy of attributes. For example, 
there may be three skin samples taken. For each sample, 
there may be several organisms tested for (e-coli, streptococcus), 
and for each organism there may be three antibiotics used, 
for example, penicillin. Each antibiotic has several properties 
(i.e. attributes) such as name, sensitivity, method of testing etc.). 
So each case would have multiple sensitivity values, 
even for the same antibiotic as it may be used many times in the case.

None of these 3 domains need the concept of “episode”.