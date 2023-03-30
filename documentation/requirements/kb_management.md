# KB Management
The `KB`s within an OpenRDR system are organised within a `KBManager`, which has the role
of creating, persisting, renaming, and so on, the various `KB`s.

| Requirement                      | Description                                                  | Validation |
|----------------------------------|--------------------------------------------------------------|------------|
| List `KB`s.                      | The `KBInfo`s for the managed `KB`s can be retrieved.        | KBM-1      |
| Create a `KB`.                   | A `KB` can be created for a given name.                      | KBM-2      |
| Ids are UUIDs.                   | The `KBInfo.id` of a `KB` is a random UUID.                  | KBM-3      |
| Retrieve `KB`s.                  | A `KB` can be retrieved based on its `KBInfo`.               | KBM-4      |

