# Milestone 2 Requirements

Milestone 2 builds on the basic interpretation and review system defined
by Milestone 1 to include:

- support for multiple `KB`s,
- basic rule building, and
- a REST interface for case interpretation.

## Multiple KBs

OpenRDR supports multiple `KB`s that are persisted using the Postgres database management system.
Only one of the `KB`s is in an active state at any time, and it is this `KB` that is used to
interpret cases.

## Basic rule building

A rule can be built in the active `KB`.

## Case storage and REST interface

Each `KB` shall have two systems of stored cases: _processed cases_ and
_cornerstone cases_. Processed cases are those that have been supplied for
interpretation using the REST interface. Cornerstone cases are those that
have been used to build rules. If a processed case is used to build a rule,
a copy of that processed case is stored as a cornerstone case.

**Attribute:**
A well-defined concept understood by experts and used by them to make classifications. For example, age, sex,
a clinical test such as TSH or LDL Cholesterol.
See [Attribute Requirements](attributes.md)
