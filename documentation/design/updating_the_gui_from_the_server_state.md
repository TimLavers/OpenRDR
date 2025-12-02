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

There are 2 mechanisms for the client to be updated when the state of the server changes:
1. The Server will provide an endpoint that returns its current state, i.e.
a. the list of all case names, and
b. the currently selected case
The client will poll for this information.
2. The server provides a web socket endpoint that the client can connect to. The server will send updates to the client
when the state of the server changes. This is used to update the GUI when the user has initiated a rule building session
using the chat interface.

The web socket approach is preferred over polling and it is planned to replace polling with web sockets in the future.
