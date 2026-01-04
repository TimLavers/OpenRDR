# OpenRDR

An open source knowledge acquisition system using the RippleDown approach.

RippleDown Rules (RDR) was initially developed by [Paul Compton](https://cgi.cse.unsw.edu.au/~compton/) in the 1980s. A
good introduction is provided in "A Philosophical Basis for Knowledge Acquisition." Compton, P and Jansen, R, 1990.
*Knowledge Acquisition* 2:241-257.

A comprehensive description is given in his
book ["Ripple-Down Rules, the Alternative to Machine Learning"](https://www.amazon.com.au/Ripple-Down-Rules-Alternative-Machine-Learning-ebook/dp/B092KVD3HQ)
Paul Compton and Byeong Ho Kang, 2021. CRC Press.

## Requirements documentation

The background, requirements, design principles, and so on to the project are documented in the
`documentation` directory that is a sibling to this file. The starting point for the
documentation is [OpenRDR](./documentation/openrdr.md).

## Setup

### Platform

OpenRDR is written in Kotlin and built with Gradle.
Java version 21 and Gradle need to be installed for OpenRDR development.

### Database
OpenRDR uses Postgres as its persistence provider. The following environment variables are required in order to
allow connection to the Postgres server:

- `OPEN_RDR_DB_URL`: the url of the Postgres server
- `OPEN_RDR_DB_USER`: name of a user with database creation, modification and deletion privileges
- `OPEN_RDR_DB_PASSWORD`: password for the user above.

The default values for the Postgres parameters are `jdbc:postgresql://localhost:5432/postgres`, `postgres`
and `postgres`.

### Google Gemini

OpenRDR uses Google Gemini to generate rule conditions from user-entered expressions.
For this feature to work, and for the associated tests to pass, a Google API key is required and needs to be set
as the value of an environment variable named `API_KEY`. To create a Google API key, see
<a href="https://cloud.google.com/docs/authentication/api-keys" title="close icons">Manage API keys</a>

## Development
Import the Gradle project into IntelliJ.

## Running the tests
There are two layers of tests:

| Test type         | Gradle verification task | 
|-------------------|--------------------------|
| jvm unit tests    | test                     |    
| cucumber tests    | cucumberTest             | 

To run the unit tests, call the `test` task for each of the subprojects: 
- `./gradlew common:test`
- `./gradlew chat:test`
- `./gradlew hints:test`
- `./gradlew server:test`
- `./gradlew llm:test`
- `./gradlew ui:test`

The cucumber tests build a fat jar and run it in a separate JVM to the client. The client is controlled
and interrogated by the test code using the accessibility API. To run the cucumber tests 
call `./gradlew cucumber:cucumberTest`.

To run a single cucumber test or feature file, add the @single annotation at the start of the 
scenario or feature file and run the `cucumberSingleTest` task.

To run the cucumber tests in just one folder, run the task ```./gradlew cucumberFolderTest -Pfolder=<folder name>```,
for example:

```./gradlew cucumberFolderTest -Pfolder=chat```

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

<a href="https://www.flaticon.com/free-icons/text" title="text icons">Text icons created by DinosoftLabs - Flaticon</a>
<a href="https://www.flaticon.com/free-icons/close" title="close icons">Close icons created by ariefstudio -
Flaticon</a>