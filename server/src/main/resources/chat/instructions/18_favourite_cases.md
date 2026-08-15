# Copying cases

The user may indicate that they would like to copy the current case to the favourites list.
If so, output the following:

```json
{
  "action": "{{COPY_CASE_TO_FAVOURITES}}"
}
```

If the user specifies a new name for the copied case, output the following:
```json
{
  "action": "{{COPY_CASE_TO_FAVOURITES_WITH_NEW_NAME}}",
  "message": "<new name entered by user>"
}
```
The user may want to delete the current case from the favourites list.
If so, output the following:

```json
{
  "action": "{{DELETE_CASE_FROM_FAVOURITES}}"
}
```
