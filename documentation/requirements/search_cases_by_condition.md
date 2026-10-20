# Case search by conditions
A user will sometimes want to find examples of cases that have particular properties.
For example, they may be interested in female diabetics, or men over 50 with raised
LDL cholesterol. The "search by conditions" facility allows this kind of data mining.

To search for cases by conditions, the user will need to specify:
- the search conditions
- whether these are "and-ed" or "or-ed"
- how many cases to find
- which case lists to search
- where to put the cases that are found

## More detailed requirements
### Search conditions
The search conditions can be assertions about raw case data,
the derived attributes, or the comments (so cases need to be interpreted
before the search conditions are checked). The user can provide one or more
such conditions. If the user provides more than one, either the conjunction
or disjunction of them is applied as the final determiner for each case.
The search conditions do not need to be true for the case currently selected.
Of course the conditions need to be parsable: they cannot reference attributes
not present in the KB, for example.

### Starting the search
When the user indicates that they would like to search for cases by conditions,
the system will build up an internal data structure that holds the information
listed above (conditions, lists, etc).

The user will be assisted in this by a series of prompts. 
These prompts will ask for:
- the conditions
- what logical connector (and/or) is to be applied
- how many cases are to be found
- which lists are to be searched
- where the cases are to go

Once this data is assembled, the chat window will show a summary of the search parameters
and ask for confirmation. For the first implementation we will not allow the user
to change the parameters they have selected.

At any point, the user can abandon this search setup process. No history will be kept
of what parameters have been set so far.

Note that a search can be started without there being a currently selected case.

### Where cases are to go
Search result cases cannot go to the `Processed` or `Cornerstone` case lists.
Apart from these, the user can choose an existing case list (ignoring the case of the name)
or a new one, as with copying cases.

If the user has specified that the cases should go into a new case list, but
no cases are found, the case list will not be created.

### Should cases be copied or moved?
As with copying cases, we shall initially put copies of the found cases into
the results list.

Note that because we may have multiple copies of a case in a list, or between lists,
we may end up with multiple copies of the same case in the search results. No
attempt will be made to do any de-duplication (though this could be a later enhancement).

### Number of cases
If the user specifies how many cases are to be found, the search stops when that
many cases are found that satisfy the search conditions. The search will continue
until the specified number of cases is found or all cases in the case lists to
be searched have been tried.

The user can also specify that all cases be searched, or that there is no limit
to the number of cases to be found.

### Lists to be searched
Any case list can be searched. A search can be on any selection of the case lists.

### Cancellation
The user can ask that the current search be terminated.
Any cases found up to the point of termination are retained.

### Search lifetime
A search will continue even if the user closes the KB and opens another.

If the user closes the KB and comes back after the search has completed, they
will not see the report of the search and its results (even if they are 
using the chat session for another KB when the search finishes).

If the KB is deleted while a search is in progress, the search is terminated
and any cases found are lost, along with the rest of the KB.

### Reporting
When the search completes, the user will be told, 
through the chat service:
- how many cases were found (possibly none)
- the case lists searched
- the search conditions (both the user-defined form and the formal form)
- where the cases were put.

If the search was terminated, the chat service will simply state this fact.

## Limitations
We will not allow (at least for the first version):
- concurrent searches within a single KB
- case searches while a rule is being built
- rule building while a search is in progress

As we may wish to search using multiple threads, no guarantees are made about the
order of cases that are added to the destination list.

