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

The default values for the Postgres parameters are `jdbc:postgresql://localhost:5432/postgres`, `postgres` and `postgres`.

## Development
Import the Gradle project into IntelliJ.

## Running the tests
For now, the unit tests are run from IntelliJ.

There are some "integration tests" which are not really full integration tests yet, as they are not
running from the jar, however they are a start. To run these:
- get the application running by running the Gradle task `application:run`.
- then run the individual tests from IntelliJ
