# Knowledge Bases as Library Components
A Knowledge Base (KB) can be used as an interpretation engine via a REST API.
However, this can be inconvenient because it involves running a server somewhere and
building the data that is to be interpreted into a case format suitable for the API.

Sometimes we just have a map of keys to values and want a KB to interpret it.
To do this, we can:
1. Export the KB as a zip file.
2. Add the OpenRDR jar to the dependencies of the codebase in which the map is to be interpreted.
3. Write code that creates a `SimpleInterpreter` component from the zip file.
4. Use the `SimpleInterpreter` component to interpret the map.

The `CompileAndRunProgramUsingOpenRDRJar.kt` file is an example of this.
In fact, this is used in the Cucumber verification test for this feature.