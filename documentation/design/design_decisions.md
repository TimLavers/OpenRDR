# Design Decisions
This document discusses design choices that have been made and which
we might want to revisit at some point.

## Not using `BigDecimal`
The Java `BigDecimal` class would have been our preferred option
for implementing `Value`s and `ReferenceRange`s. However, this 
class is not available in the common realm.
See  [Treatment of real numbers](treatment_of_real_numbers.md).

## KB Export
To be completed

## Persistence
To be completed

## Single instance of each object within a KB
On the server, a KB is a single object in memory, with references to various sub-objects, which
of course themselves refer to further sub-objects, and so on.
In this object structure, any two objects that are equal, will in fact be
the same instance. 

For example, suppose, that a `rule A` has a condition `glucose is high` and
`rule B` has a condition `glucose is normal`. Both of these conditions
shall refer to the same `glucose` object. If a new rule is added,
that has the condition `glucose is high`, then this too shall refer to the
same instance in memory. 

The purpose of this design is to ensure that if an object is changed, then this change
is reflected consistently throughout the object hierarchy.
