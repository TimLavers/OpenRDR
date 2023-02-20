package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.server.routes.CREATE_KB
import io.rippledown.server.routes.EXPORT_KB
import io.rippledown.server.routes.IMPORT_KB
import io.rippledown.server.routes.KB_INFO
import java.io.File
import kotlin.test.Test

class KBManagementTest: OpenRDRServerTestBase() {

    @Test
    fun kbName() = testApplication {
        setupWithMock()
        val kbInfo = KBInfo("Glucose")
        every { serverApplicationMock.kbName() } returns kbInfo
        val result = httpClient.get(KB_INFO)
        result.status shouldBe HttpStatusCode.OK
        result.body<KBInfo>() shouldBe kbInfo
        verify { serverApplicationMock.kbName() }
    }

    @Test
    fun createKB() = testApplication {
        setupWithMock()
        every { serverApplicationMock.createKB() } returns Unit
        val result = httpClient.post(CREATE_KB)
        result.status shouldBe HttpStatusCode.OK
        result.body<OperationResult>().message shouldBe "KB created"
        verify { serverApplicationMock.createKB() }
    }

    @Test
    fun exportKB() = testApplication {
        setupWithMock()
        val zipFile = File("src/jvmTest/resources/export/Empty.zip")
        every { serverApplicationMock.kbName() } returns KBInfo("Empty")
        every { serverApplicationMock.exportKBToZip() } returns zipFile
        val result = httpClient.get(EXPORT_KB)
        result.status shouldBe HttpStatusCode.OK
        result.headers[HttpHeaders.ContentType] shouldBe "application/zip"
        result.headers[HttpHeaders.ContentDisposition] shouldBe "attachment; filename=Empty.zip"
        result.headers[HttpHeaders.ContentLength] shouldBe zipFile.length().toString()
        verify { serverApplicationMock.exportKBToZip() }
    }

    @Test
    fun importKB() = testApplication {
        setupWithMock()
        val zipFile = File("src/jvmTest/resources/export/KBExported.zip")
        val zipBytes = zipFile.readBytes()
        every {serverApplicationMock.importKBFromZip(zipBytes)} returns Unit
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
        verify { serverApplicationMock.importKBFromZip(zipBytes) }
    }
 }