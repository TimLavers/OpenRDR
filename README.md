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
documentation is [documentation/README.md](./documentation/README.md).

## Setup

### Platform

OpenRDR is written in Kotlin and built with Gradle.
Use JDK 21 and the checked-in Gradle wrapper (`gradlew` or `gradlew.bat`); a separate Gradle installation is not required.

### Database
OpenRDR uses Postgres as its persistence provider. The following environment variables can override the defaults to
allow connection to the Postgres server:

- `OPEN_RDR_DB_URL`: the url of the Postgres server
- `OPEN_RDR_DB_USER`: name of a user with database creation, modification and deletion privileges
- `OPEN_RDR_DB_PASSWORD`: password for the user above.

The default values for the Postgres parameters are `jdbc:postgresql://localhost:5432/postgres`, `postgres`
and `postgres`.

#### Running Postgres locally with Podman

A quick way to get a Postgres server matching the default parameters above is to run one in a container.
Using [Podman](https://podman.io/):

```shell
podman run --name openrdr-postgres --restart=always \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=postgres \
  -p 5432:5432 -d docker.io/library/postgres:18
```

This listens on `localhost:5432` with user/password/database all set to `postgres`, so no environment
variables need to be set. The `--restart=always` policy brings the container back automatically (for
example after the Podman machine restarts).

Useful follow-up commands:

```shell
podman ps                                 # check it is running
podman exec openrdr-postgres pg_isready -U postgres   # confirm it accepts connections
podman stop openrdr-postgres              # stop it (data is retained)
podman start openrdr-postgres             # start it again
podman rm -f openrdr-postgres             # remove it for a clean slate
```

On macOS the container only comes back after a full reboot once the Podman machine is running; start it
if needed with `podman machine start`.

### Google Gemini

OpenRDR uses Google Gemini to generate rule conditions from user-entered expressions,
support chat authoring, and evaluate AI Boolean derived attributes from primary case text
using expert-approved criteria. Runtime classifications are stored for reproducible inference.
See the [ten-case diabetes demonstration](documentation/ai_derived_attribute_demo.md).
Chat, condition translation, AI classification, reports and voice transcription require a Gemini API key. Set it
as the value of an environment variable named `API_KEY`. To create a Google API key, see
[Gemini API key setup](https://ai.google.dev/gemini-api/docs/api-key).

## Development and tests

Import the Gradle project into IntelliJ. Follow [AGENTS.md](AGENTS.md) for coding and execution instructions.
The [testing guide](documentation/design/testing.md) lists focused test commands, desktop constraints,
Cucumber tasks and model/database requirements.

Use `./gradlew versionCatalogUpdate` to update dependency versions. Build a portable demonstration with
`./gradlew demoZip`; see [the packaged demo guide](packaging/README-demo.txt) for setup and scenarios.

Third-party code, sample sources and icon acknowledgements are in [credits](documentation/credits.md).
