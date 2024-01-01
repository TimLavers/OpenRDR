# KB Management
The `KB`s within an OpenRDR system are organised within a `KBManager`, which has the role
of creating, persisting, renaming, and so on, the various `KB`s. 

In the initial version of the system, an empty `KB` called 'Thyroids' will be created
if there is no `KB` with that name. (This is to simplify the front end so that 
it never has to deal with the situation in which there are no `KB`s.)

At startup, the server will load all persisted `KB`s. This simplifies things but may
cause problems down the track in systems that have large numbers of rarely used `KB`s.

| Requirement         | Description                                           | Validation |
|---------------------|-------------------------------------------------------|------------|
| List `KB`s.         | The `KBInfo`s for the managed `KB`s can be retrieved. | KBM-1      |
| Create a `KB`.      | A `KB` can be created for a given name.               | KBM-2      |
| Ids are UUIDs.      | The `KBInfo.id` of a `KB` is a random UUID.           | KBM-3      |
| Retrieve `KB`s.     | A `KB` can be retrieved based on its `KBInfo`.        | KBM-4      |
| Delete a `KB`.      | A `KB` can deleted, based on its `KBInfo.id`.         | KBM-5      |
| Load `KB`s at init. | All `KB`s are loaded at server startup.               | KBM-6      |
 

