# User-defined case lists
A Knowledge Base (KB) has two built-in lists of cases: `Processed Cases` and `Cornerstone Cases`.
In addition, the user can create lists of cases as a way of organising the cases that are of interest to them.
For example, in a diabetes KB, a user might want to copy one case to a "Borderline" case list, and another
case to a "Gestational Diabetes" list.

The user will create these lists "on demand": a list will be created when a case is copied to it.

Copying the currently selected case will be done via the chat interface. The user will need to
provide the name of the case list and, optionally, a new case name for the copy.
Only the currently selected case can be copied.

The reason we only allow copying the current case is that there might be more than one case with
a given name, so in general there's no way for the user to specify a case other than that which
is currently selected.

## Operations available on user-defined lists
It shall be possible to:
- delete 
- rename
- edit (to be done)
any case in a user-defined case list through.

When a Knowledge Base is exported to a zip file, the user-defined case lists are included.

