package io.rippledown

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.server.ServerApplication
import java.io.File
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenRDRServerTest {
    lateinit var serverApplication: ServerApplication
    lateinit var serverApplicationSpy: ServerApplication
    lateinit var serverApplicationMock: ServerApplication
    lateinit var httpClient: HttpClient

    @Test
    fun testPing() = testApplication {
        setup()
        val response = httpClient.get(PING)
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun kbName() = testApplication {
        setup()
        val result = httpClient.get(KB_INFO)
        result.status shouldBe HttpStatusCode.OK
        result.body<KBInfo>().name shouldBe serverApplication.kb.name
    }

    @Test
    fun createKB() = testApplication {
        setup()
        val result = httpClient.post(CREATE_KB)
        result.status shouldBe HttpStatusCode.OK
        result.body<OperationResult>().message shouldBe "KB created"
        verify { serverApplicationSpy.createKB() }
    }

    @Test
    fun exportKB() = testApplication {
        setup()
        val result = httpClient.get(EXPORT_KB)
        result.status shouldBe HttpStatusCode.OK
        result.headers[HttpHeaders.ContentType] shouldBe "application/zip"
        result.headers[HttpHeaders.ContentDisposition] shouldBe "attachment; filename=Thyroids.zip"
        result.headers[HttpHeaders.ContentLength] shouldBe "949"
        verify { serverApplicationSpy.exportKBToZip() }
    }

    @Test
    fun importKB() = testApplication {
        setup()
        val zipFile = File("src/jvmTest/resources/export/KBExported.zip").toPath()
        val zipBytes = zipFile.readBytes()
        val boundary = "WebAppBoundary"
        val response = httpClient.post(IMPORT_KB) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("zip", zipBytes, Headers.build {
                            append(HttpHeaders.ContentType, "application/zip")
                            append(HttpHeaders.ContentDisposition, "filename=\"KBExported.zip\"")
                        })
                    },
                    boundary,
                    ContentType.MultiPart.FormData.withParameter("boundary", boundary)
                )
            )
        }
        response.body<OperationResult>().message shouldBe "KB imported"
        verify { serverApplicationSpy.importKBFromZip(zipBytes) }
    }

    @Test
    fun waitingCases() = testApplication {
        setup()
        val result = httpClient.get(WAITING_CASES)
        result.status shouldBe HttpStatusCode.OK
        result.body<CasesInfo>().count shouldBe 0
        verify { serverApplicationSpy.waitingCasesInfo() }
    }

    private fun ApplicationTestBuilder.setup() {
        serverApplication = ServerApplication()
        serverApplicationSpy = spyk(serverApplication)
        serverApplicationMock = mockk()
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
            serverManagement()
            kbManagement(serverApplicationSpy)
            caseManagement(serverApplicationSpy)
        }
        httpClient = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}