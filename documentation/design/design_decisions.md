# Design Decisions
This document discusses design choices that have been made and which
we might want to revisit at some point.

## Not using `BigDecimal`
The Java `BigDecimal` class would have been our preferred option
for implementing `Value`s and `ReferenceRange`s. However, this 
class is not available in the common realm.
See  [Treatment of real numbers](treatment_of_real_numbers.md).