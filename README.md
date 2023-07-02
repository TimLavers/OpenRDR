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

[water wave icon](https://uxwing.com/water-wave-icon) made by [uxwing](https://www.uxwing.com)