package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.CREATE_KB
import io.rippledown.constants.api.EXPORT_KB
import io.rippledown.constants.api.IMPORT_KB
import io.rippledown.constants.api.KB_INFO
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import java.io.File
import kotlin.test.Test

class KBManagementTest: OpenRDRServerTestBase() {

    @Test
    fun kbName() = testApplication {
        setup()
        val kbInfo = KBInfo("Glucose")
        every { serverApplication.kbName() } returns kbInfo
        val result = httpClient.get(KB_INFO)
        result.status shouldBe HttpStatusCode.OK
        result.body<KBInfo>() shouldBe kbInfo
        verify { serverApplication.kbName() }
    }

    @Test
    fun createKB() = testApplication {
        setup()
        every { serverApplication.reCreateKB() } returns Unit
        val result = httpClient.post(CREATE_KB)
        result.status shouldBe HttpStatusCode.OK
        result.body<OperationResult>().message shouldBe "KB created"
        verify { serverApplication.reCreateKB() }
    }

    @Test
    fun exportKB() = testApplication {
        setup()
        val zipFile = File("src/jvmTest/resources/export/Empty.zip")
        every { serverApplication.kbName() } returns KBInfo("Empty")
        every { serverApplication.exportKBToZip() } returns zipFile
        val result = httpClient.get(EXPORT_KB)
        result.status shouldBe HttpStatusCode.OK
        result.headers[HttpHeaders.ContentType] shouldBe "application/zip"
        result.headers[HttpHeaders.ContentDisposition] shouldBe "attachment; filename=Empty.zip"
        result.headers[HttpHeaders.ContentLength] shouldBe zipFile.length().toString()
        verify { serverApplication.exportKBToZip() }
    }

    @Test
    fun importKB() = testApplication {
        setup()
        val zipFile = File("src/jvmTest/resources/export/KBExported.zip")
        val zipBytes = zipFile.readBytes()
        every { serverApplication.importKBFromZip(zipBytes) } returns Unit
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
        verify { serverApplication.importKBFromZip(zipBytes) }
    }
 }