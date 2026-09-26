# Conditions

Requirements: [conditions.md](../requirements/conditions.md). This doc is the evaluation model; how a user's phrase
becomes a condition is in [condition_translation.md](condition_translation.md).

## Three classes

`Condition` is a sealed hierarchy with three subclasses, one per shape of clinical assertion. Each is a small tree of
typed parts whose `holds(case)` is the interpreter. The grammar is fixed and domain-specific on purpose: it gives a
meaningful `sameAs` for de-duplication, stable serialisation, natural-language rendering, and per-shape editors in the
suggestion UI. A general boolean algebra was considered and rejected; a free-form `ExpressionCondition` could be added
*beside* these if ever needed (see [suggested_conditions.md](suggested_conditions.md)).

### Episodic

`EpisodicCondition(attribute, predicate, signature)`. Evaluation:

1. take the attribute's results across the episodes, in date order;
2. apply the `TestResultPredicate` to each, giving a sequence of booleans;
3. apply the `Signature` to the sequence.

| Signature    | True when                     |
|--------------|-------------------------------|
| `Current`    | the last value is true        |
| `Previous`   | the second-last value is true |
| `All`        | every value is true           |
| `Some`       | at least one is true          |
| `No`         | none is true                  |
| `AtLeast(n)` | n or more are true            |
| `AtMost(n)`  | n or fewer are true           |

For the case

|                 | 2023-03-11 | 2023-05-01 | 2023-08-16 |
|-----------------|------------|------------|------------|
| TSH (0.5 - 4.0) | 0.03       | 0.09       | 1.2        |
| FT3 (3.0 - 5.5) | 6.1        | 4.3        | 5.5        |

`all TSH are normal` → `(0.03, 0.09, 1.2)` → `(F, F, T)` → `All` → false; `no FT3 is low` → `(F, F, F)` → `No` → true.

Rendering agrees the verb with the signature: `(TSH, normal, All)` is `all TSH are normal`, `(TSH, normal, No)` is
`no TSH is normal`, and `Current` is unspoken: `TSH is normal`.

### Series

`SeriesCondition(attribute, predicate)` applies a `SeriesPredicate` (`Increasing`, `Decreasing`, `maximum < x`, …) to
the whole sequence of the attribute's values.

### Case structure

`CaseStructureCondition(predicate)` applies a `CaseStructurePredicate` to the case: `IsPresentInCase(attribute)`,
`IsAbsentFromCase(attribute)`, `IsSingleEpisodeCase`.

## Identity and sharing

A condition has an integer id and is stored once (`ConditionManager.getOrCreate`, by `sameAs`). Rules hold condition
ids. On load, a condition's attributes are re-pointed at the KB's instances (`alignAttributes`) so that the
single-instance
invariant holds and a rename is seen everywhere. The user's phrase for a condition is stored on it; see
[comments.md](comments.md).

## Real numbers

Values are `Double`. `BigDecimal` would be preferable for exactness but is not available to `common`, which must stay
pure Kotlin so that the same condition code runs in the client. The domain tolerates this: pathology measurements are
imprecise. What must not happen is a pathologist being confused by floating-point noise, e.g. a result of `0.95` against
a limit computed as 95 % of `1.0`. Numeric predicates therefore treat values that are extremely close as equal; a user
is far less likely to notice `0.9499999` being treated as `0.95` than to be baffled by a condition that is false on a
value sitting exactly on its limit.

## Not built

- **Restriction clauses**, e.g. `all Glucose are normal, where collection type is "fasting"`: an episode filter applied
  before the three-step evaluation. The model leaves room for it.
- **Time between episodes**, needed for a TSH rule about a pattern over six months.
- **Non-episodic domains** (a single text blob; sets of related results such as allergen panels; multi-valued
  hierarchies such as microbiology sensitivities). These need different case structures, not new conditions.
