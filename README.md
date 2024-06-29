# OpenRDR

An open source knowledge acquisition system using the RippleDown approach.

## Requirements documentation

The background, requirements, design principles, and so on to the project are documented in the
`documentation` directory that is a sibling to this file. The starting point for the
documentation is [OpenRDR](./documentation/openrdr.md).

## Setup

Install Java 17 and Gradle.

OpenRDR uses Postgres as its persistence provider. The following environment variables are required in order to
allow connection to the Postgres server:

- `OPEN_RDR_DB_URL`: the url of the Postgres server
- `OPEN_RDR_DB_USER`: name of a user with database creation, modification and deletion privileges
- `OPEN_RDR_DB_PASSWORD`: password for the user above.

The default values for the Postgres parameters are `jdbc:postgresql://localhost:5432/postgres`, `postgres`
and `postgres`.

## Development

Import the Gradle project into IntelliJ.

## Running the tests

There are four types of tests:

| Test type         | Gradle verification task | 
|-------------------|--------------------------|
| jvm unit tests    | jvmTest                  |    
| js unit tests     | jsTest                   |   
| integration tests | integrationTest          |    
| cucumber tests    | cucumberTest             | 

The integration and cucumber tests build a fat jar and run it in a separate JVM to the client. The client is controlled
by Selenium.

To run a single cucumber test or feature file, add the @single annotation at the start of the scenario or feature file and run the `cucumberSingleTest` task.

## Acknowledgements

[water wave icon](https://uxwing.com/water-wave-icon) by [uxwing](https://www.uxwing.com)

[collapse icon](https://icons8.com/icon/60653/collapse-arrow) by [Icons8](https://icons8.com)

[expand icon](https://icons8.com/icon/60662/expand-arrow) by [Icons8](https://icons8.com)

[plus minus icons](https://www.flaticon.com/free-icons/plus-minus) by [Icon Hubs - Flaticon](https://www.flaticon.com/)

[document_icons](https://www.flaticon.com/free-icons/document) by [Icon Hubs - Flaticon](https://www.flaticon.com/)

[wrench icon](https://www.flaticon.com/free-icons/work-tools) by [Icon Hubs - Flaticon](https://www.flaticon.com/)

[plus icon](https://www.flaticon.com/free-icons/plus) by [Freepik - Flaticon](https://www.flaticon.com/)

<a href="https://www.flaticon.com/free-icons/plus" title="plus icons">Plus icons created by Royyan Wijaya - Flaticon</a>

[minus icon](https://www.flaticon.com/free-icons/minus) by [Freepik - Flaticon](https://www.flaticon.com/)

<a href="https://www.flaticon.com/free-icons/arrow-symbol" title="arrow symbol icons">Arrow symbol icons created by
Steven Edward Simanjuntak - Flaticon</a>

<a href="https://www.flaticon.com/free-icons/tick" title="tick icons">Tick icons created by Maxim Basinski Premium -
Flaticon</a>

<a href="https://www.flaticon.com/free-icons/delete-file" title="delete file icons">Delete file icons created by
yaicon - Flaticon</a>

<a href="https://www.flaticon.com/free-icons/paper" title="paper icons">Paper icons created by SeyfDesigner -
Flaticon</a>

<a href="https://www.flaticon.com/free-icons/files-and-folders" title="files and folders icons">Files and folders icons
created by SeyfDesigner - Flaticon</a>

<a href="https://www.flaticon.com/free-icons/replace" title="replace icons">Replace icons created by Delwar018 -
Flaticon</a>

