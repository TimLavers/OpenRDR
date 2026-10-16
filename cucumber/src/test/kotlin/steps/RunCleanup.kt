package steps

import io.cucumber.java.AfterAll

@AfterAll
fun stopCucumberServer() = StepsInfrastructure.stopServer()
