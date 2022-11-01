# Case View
The `CaseView` displays the data and interpretation for a case to the user.

## Case data
The data is shown in a table in which:
- there is a row for each attribute having data in the case
- there is a column for each date at which one or more test results were given.

| Requirement     | Description                                                                                    | Validation |
|-----------------|------------------------------------------------------------------------------------------------|------------|
| Case name       | The name of the case is shown as a heading above the case table.                               |            |
| Case rows       | There is a row for each attribute that has data in the case.                                   |            |
| Units           | The units for a test result are shown alongsided the value.                                    |            |
| Reference range | If there is a reference range for the most recent test result, it is shown in the last column. |            |
|                 |                                                                                                |            |

## Order of attributes
There is an ordering of attributes, which is used to order the rows of the case table.
The attributes can be ordered by dragging them within the case table. The initial ordering
is essentially random.
