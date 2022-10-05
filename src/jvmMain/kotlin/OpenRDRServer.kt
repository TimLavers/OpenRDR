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
import io.rippledown.model.Conclusion
import io.rippledown.model.Interpretation
import io.rippledown.model.OperationResult
import io.rippledown.model.condition.Condition
import io.rippledown.server.ServerApplication
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

const val WAITING_CASES = "/api/waitingCasesInfo"
const val CASE = "/api/case"
const val START_SESSION = "/api/startSession"
const val ADD_CONDITION = "/api/addCondition"
const val COMMIT_SESSION = "/api/commitSession"
const val CREATE_KB = "/api/createKB"
const val SHUTDOWN = "/api/shutdown"
const val PING = "/api/ping"

const val STARTING_SERVER = "Starting server"
const val STOPPING_SERVER = "Stopping server"

lateinit var server: NettyApplicationEngine

val logger = LoggerFactory.getLogger("rdr")

fun main() {
    val application = ServerApplication()

    server = embeddedServer(Netty, 9090) {
        install(ContentNegotiation) {
            json(Json { allowStructuredMapKeys = true })
        }
        install(CallLogging) {
            level = Level.TRACE
            filter { call -> call.request.path().startsWith("/") }
            format { call ->
                val status = call.response.status()
                val httpMethod = call.request.httpMethod.value
                val userAgent = call.request.headers["User-Agent"]
                "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
            }
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
            get(WAITING_CASES) {
                call.respond(application.waitingCasesInfo())
            }
            get(CASE) {
                val id = call.parameters["id"] ?: error("Invalid case id.")
                call.respond(application.case(id))
            }
            post("/api/interpretationSubmitted") {
                val interpretation = call.receive<Interpretation>()
                val result = application.saveInterpretation(interpretation)
                call.respond(HttpStatusCode.OK, result)
            }
            post(START_SESSION) {
                val id = call.parameters["id"] ?: error("Invalid case id.")
                val conclusion = call.receive<Conclusion>()
                application.startRuleSessionToAddConclusion(id, conclusion)
                call.respond(HttpStatusCode.OK, OperationResult("Session started"))
            }
            post(ADD_CONDITION) {
                println("add condition----------------------")
                val str = call.receiveText()
                println("got this data as text: $str")
                val condition = Json.decodeFromString(Condition.serializer(), str)
                println("condition is: $condition")
                application.addConditionToCurrentRuleBuildingSession(condition)
                call.respond(HttpStatusCode.OK, OperationResult("Condition added"))
            }
            post(COMMIT_SESSION) {
                application.commitCurrentRuleSession()
                call.respond(HttpStatusCode.OK, OperationResult("Session committed"))
            }
            post(CREATE_KB) {
                application.createKB()
                call.respond(HttpStatusCode.OK, OperationResult("KB created"))
            }
            get(PING) {
                call.respond(HttpStatusCode.OK, "OK")
            }
            post(SHUTDOWN) {
                logger.info(STOPPING_SERVER)
                server.stop(0, 0)
            }
        }
    }
    logger.info(STARTING_SERVER)
    server.start(wait = true)
}