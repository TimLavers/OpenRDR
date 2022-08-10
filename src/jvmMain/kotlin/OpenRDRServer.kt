import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.rippledown.model.Interpretation
import io.rippledown.server.ServerApplication
import kotlinx.serialization.json.Json

fun main() {
    val application = ServerApplication()
    embeddedServer(Netty, 9090) {
        install(ContentNegotiation) {
            json(Json { allowStructuredMapKeys = true })
        }
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Delete)
            anyHost()
        }
        install(Compression) {
            gzip()
        }
        routing {
            get("/") {
                call.respondText(
                    this::class.java.classLoader.getResource("index.html")!!.readText(),
                    ContentType.Text.Html
                )
            }
            static("/") {
                resources("")
            }
            get("/api/waitingCasesInfo") {
                call.respond(application.waitingCasesInfo())
            }
            get("/api/case") {
                val id =call.parameters["id"] ?:error("Invalid case id.")
                call.respond(application.case(id))
            }
            post("/api/interpretationSubmitted") {
                val interpretation = call.receive<Interpretation>()
                val result = application.saveInterpretation(interpretation)
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }.start(wait = true)
}