# Milestone 1 Requirements
Milestone 1 will be a system for providing interpretations for Thyroid cases in a 
Clinical Pathology setting, using the rules 

## Basic concepts
The basic data structures needed for Milestone 1 are given below.  
**Ticket:** [ORD1](../tickets/work_tickets.md)

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

**TestResult**
Combination of a `Value`, a `Reference Range` (optional) and a `Unit` (optional)
associated with a particular `Attribute` in a data set. 

**RDRCase**
A collection of `Attribute`s and corresponding `TestResult`s for a particular set of related data. 
A typical example is the set of test results for a patient on a particular date.

## User interface
**CaseView:**
An `RDRCase` is presented to the user as a table with three columns. 
Each `Attribute` and its values in the case in a single row with the `Attribute`
in the first column, the `Value` and `Unit`s in the second column, and the
`ReferenceRange` in the third column.  
**Ticket:** [ORD2](../tickets/work_tickets.md)

## Data processing
The Clinical Decision Support System is backed by a Knowledge Base (KB).
It has this workflow:
- Thyroid cases are written into an 'inputs' directory.
- A clinician can view the cases together with their interpretations from the KB.
- The clinician can edit the interpretation provided by the KB.
- Interpretations (modified or not) that are approved by a Clinician are written to an 'outputs' directory.
- Cases that have been approved are deleted from the 'inputs' directory.

**Ticket:** [ORD3](../tickets/work_tickets.md)

## Example cases
The cases described in the TSH paper by Vasikaran and Loh will be available
for review.
