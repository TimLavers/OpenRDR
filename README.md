# OpenRDR
An open source knowledge acquisition system using the RippleDown approach.

## Setup
Install Java 1.8 and Gradle.

Install Chromedriver (for testing) in `C:\chromedriver\chromedriver.exe` (need to fix this location requirement).

Import the Gradle project into IntelliJ.

## Running the tests
For now, the unit tests are run from IntelliJ.

There are some "integration tests" which are not really full integration tests yet, as they are not
running from the jar, however they are a start. To run these:
- get the application running by running the Gradle task `application:run`.
- then run the individual tests from IntelliJ
