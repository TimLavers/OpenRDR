# Milestone 1 Requirements

## General requirements
### Basic concepts
The basic data structures needed for Milestone 1 are given below.
[ORD1](../tickets/work_tickets.md)

**Attribute:**
A well-defined concept understood by experts and used by them to make classifications. For example, age, sex,
a clinical test such as TSH or LDL Cholesterol.

**Value:**
The value for an `Attribute` within a data set. Could be numerical, textual, or boolean.

**Reference Range:**
A numerical range within which a `Value` is considered normal. Typically, the
normal range depends on factors such as the age and sex of the patient. 

**Unit**
Measurement units for a `Value`.

**Result**
Combination of a `Value`, a `Reference Range` (optional) and a `Unit` (optional)
associated with a particular `Attribute` in a data set. 

**Case**
A collection of `Attribute`s and corresponding `Result`s for a particular set of related data. 
A typical example is the set of test results for a patient on a particular date.
