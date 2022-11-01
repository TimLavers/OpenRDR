import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
const val START_SESSION_TO_ADD_CONCLUSION = "/api/startSessionToAddConclusion"
const val START_SESSION_TO_REPLACE_CONCLUSION = "/api/startSessionToReplaceConclusion"
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
            json()
//            Json {
//                encodeDefaults = true
//                isLenient = true
//                allowSpecialFloatingPointValues = true
//                allowStructuredMapKeys = true
//                prettyPrint = false
//                useArrayPolymorphism = false
//            }
//            json(Json { allowStructuredMapKeys = true })
        }
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
            format { call ->
                val status = call.response.status()
                call.request.httpMethod
                val httpMethod = call.request.httpMethod.value
                val userAgent = call.request.headers["User-Agent"]
                "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
            }
        }
        install(CORS) {
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
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
                call.respond(application.viewableCase(id))
            }
            post("/api/interpretationSubmitted") {
                val interpretation = call.receive<Interpretation>()
                val result = application.saveInterpretation(interpretation)
                call.respond(HttpStatusCode.OK, result)
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
        ruleSession(application)
    }
    logger.info(STARTING_SERVER)
    server.start(wait = true)
}
fun Application.ruleSession(application: ServerApplication) {
    routing {
        post(START_SESSION_TO_ADD_CONCLUSION) {
            val id = call.parameters["id"] ?: error("Invalid case id.")
            val conclusion = call.receive<Conclusion>()
            application.startRuleSessionToAddConclusion(id, conclusion)
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
        }
        post(START_SESSION_TO_REPLACE_CONCLUSION) {
            val id = call.parameters["id"] ?: error("Invalid case id.")
            val conclusionPair = call.receive<List<Conclusion>>()
            application.startRuleSessionToReplaceConclusion(id, conclusionPair[0], conclusionPair[1])
            call.respond(HttpStatusCode.OK, OperationResult("Session started"))
        }
        post(ADD_CONDITION) {
            val str = call.receiveText()
            val condition = Json.decodeFromString(Condition.serializer(), str)
            application.addConditionToCurrentRuleBuildingSession(condition)
            call.respond(HttpStatusCode.OK, OperationResult("Condition added"))
        }
        post(COMMIT_SESSION) {
            application.commitCurrentRuleSession()
            call.respond(HttpStatusCode.OK, OperationResult("Session committed"))
        }
    }
}