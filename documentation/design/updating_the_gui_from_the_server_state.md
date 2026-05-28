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

The client is updated via a web socket endpoint exposed by the server. The
client connects on startup, and the server pushes events to the client when
its state changes — for example, when a new case arrives, when the currently
selected case changes, or when the user initiates a rule-building session
through the chat interface and the cornerstone view needs to show.

### Alternative considered

A polling approach was also considered, in which the server would expose an
endpoint returning its current state (the list of case names and the
currently selected case) and the client would poll it on a timer. This was
rejected in favour of the web socket because pushing updates avoids the
trade-off between latency and load that polling forces, and because the
chat-driven cornerstone-review flow needs near-immediate updates.
