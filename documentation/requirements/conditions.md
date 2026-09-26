# Conditions

A condition is a predicate on a case: it holds or it does not. The conditions of a rule are the expert's justification
for its action, and every condition of a rule must hold for the case the rule was built on.

## Kinds of condition

Clinical knowledge bases need three shapes of assertion, and OpenRDR has one class of condition for each.

| Kind           | Asserts something about                               | Examples                                                                                     |
|----------------|-------------------------------------------------------|----------------------------------------------------------------------------------------------|
| Episodic       | the test results of one attribute, episode by episode | `TSH is high`, `all Glucose are normal`, `at least 3 PSA are high`, `previous FT3 is normal` |
| Series         | the whole sequence of an attribute's values           | `TSH is increasing`, `maximum BMI < 18`                                                      |
| Case structure | the case itself                                       | `TSH is not in case`, `case has a single episode`                                            |

An episodic condition combines a **predicate** on a single result (`high`, `normal`, `> 70`, `contains "fasting"`)
with a **signature** saying which episodes must satisfy it (`current`, `previous`, `all`, `some`, `no`, `at least n`,
`at most n`). The everyday single-result condition `TSH is high` is the `current` signature, left unspoken.
Multi-episode
conditions are common in commercial thyroid and other knowledge bases, which is why the signature is a first-class part
of the model rather than a later extension.

| Requirement                | Description                                                                                                                                                                | Validation                                                                                                   |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Own words                  | The user states a condition in their own words and language; the system translates it and shows the formal form alongside. See [rule_building.md](rule_building.md).       | `rulebuilding/Conditions in the users words.feature`, `chat/Build rules using non-English languages.feature` |
| Must hold                  | A condition is added to a rule only if it holds for the case the rule is being built on.                                                                                   | `chat/Add comments with conditions.feature`                                                                  |
| Reference-range predicates | `high`, `low` and `normal` are judged against the result's own reference range, so the same rule applies to patients with different ranges.                                | `common` condition tests                                                                                     |
| Derived attributes         | Conditions on derived attributes and comments use the same syntax; presence (`is in case`) is the natural condition on a comment.                                          | `inferencing/Repeat inferencing.feature`                                                                     |
| Identity                   | A condition is stored once, has an integer id, and is shared by every rule that uses it, so the user's phrase for it is the same everywhere.                               | `ConditionManagerTest`                                                                                       |
| Attribute alignment        | The attributes inside a condition are the knowledge base's own instances, so a rename is seen everywhere at once.                                                          | `ConditionManagerTest`                                                                                       |
| Real numbers               | Comparisons treat values that differ only by floating-point error as equal, so a result exactly on a computed limit is not judged by rounding noise the expert cannot see. | `common` predicate tests                                                                                     |

**Not implemented:** restriction clauses (`all Glucose are normal, where collection type is "fasting"`); conditions on
the time between episodes; conjunctions and disjunctions within one condition (a rule's conditions are implicitly
conjoined, and disjunction is expressed by separate rules, as RDR intends).

Design: [design/conditions.md](../design/conditions.md).
