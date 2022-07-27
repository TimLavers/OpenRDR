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

**Conclusion**
A textual comment applicable to a given case.

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

## Condition syntax
To express the justifications for the comments given in the TSH paper,
we need the following concepts:  
**1.4.1**
__Normal values__ according to the reference range.  

**1.4.2**
__Test not done__ (no Free T4 in this case).

**1.4.3**
__Value slightly low__ Is the fraction constant or needed as a parameter?.

**1.4.4**
__Value slightly high__ Again, how do we specify what "slightly" means?

**1.4.5**
No new concepts.

**1.4.6**
__Value greater than some constant__ This specifies what is meant by "elderly".

**1.4.7**
No new concepts.

**1.4.8**
__Value equal to specific text__ In this case we are detecting "Obstetric clinic" or perhaps "/40".

**1.4.9**
__Text analysis__ In this case, the clinical notes "Trying for a baby"
are key to the conclusions drawn. How should we handle this?

**1.4.10**
__Value low__ according to the reference range.

**1.4.11**
__Value high__ (FT3 in this case).

**1.4.12**
__Severely elevated value__ Is this compared to the reference range? If so, by how much? Or is
it compared to some other range?
__Handle values such as "<5"__ These are understood to mean "some value that could not be
accurately measured, but is less than 5". Such values need to be low according to the 
reference range. Another approach would be to compare to the value "<5" textually,
as this is likely a standard cutoff for Free T4 measurements.

**1.4.13**
__Textual analyses__ Need to understand "started T4 replacement 1 week ago".
This could also be considered a multi-episode case, in conjunction with
the previous scenario.

**1.4.14**
No new concepts.

**1.4.15**
No new concepts.

**1.4.16**
No new concepts.

**1.4.17**
No new concepts.

**1.4.18**
__PREVIOUS RESULTS__ This is in fact a multi-episode case.

**1.4.19**
No new concepts.

**1.4.20**
No new concepts.

**1.4.21**
Another multi-episode scenario.

**1.4.22**
No new concepts.

**1.4.23**
No new concepts.

**1.4.24**
No new concepts.

**1.4.25**
No new concepts.

**1.4.26**
No new concepts.

**1.4.27**
No new concepts.

**1.4.28**
No new concepts.

**1.4.29**
No new concepts.

**1.4.30**
No new concepts.

**1.4.30**
This, in conjunction with 1.4.29, is a multi-episode case.

**1.4.31**
No new concepts.

**1.4.32**
No new concepts.

**1.4.33**
No new concepts.

**1.4.34**
No new concepts.

**1.4.35**
No new concepts.


