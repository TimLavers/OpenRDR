# Updating the GUI from the Server state

## What do we want to achieve?

There are several instances where we want the GUI to be updated when the state of the Server changes:

1. When the list of cases held by the Server changes, e.g. when a new case is received via the REST interface. We then
   want the list of cases shown in the GUI to be correspondingly updated,
2. When the state of the currently shown case changes, e.g. its interpretation changes or the case's attributes change.
   We then want the view of the current case to be updated, and
3. When the user has started building a rule using the chat and has indicated in the chat that they want to review the
   cornerstone cases. We then want the view of the cornerstone case to show.

## Design

1. There is currently no mechanism for the Server to directly update the client (e.g. Reverse Invoke using gRPC or
   WebSockets). This approach may be considered in the future, but for now we will use polling.
2. The GUI will poll the Server at regular intervals (e.g. every 2 seconds) to check if the state of the Server has
   changed. If it has, the GUI will update correspondingly.
3. The Server will provide an endpoint that returns its current state, i.e.
    1. the list of all case names,
    2. the currently selected case, and
    3. cornerstone case information (i.e. CornerstoneStatus)

