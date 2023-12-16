package io.rippledown.server

import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.*
import io.rippledown.model.KBInfo
import io.rippledown.model.OperationResult
import io.rippledown.server.routes.KB_ID
import java.io.File
import kotlin.test.Test

class KBManagementTest: OpenRDRServerTestBase() {

    @Test
    fun kbName() = testApplication {
        setup()
        val kbInfo = KBInfo("Glucose")
        every { kbEndpoint.kbName() } returns kbInfo
        val result = httpClient.get(KB_INFO){
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<KBInfo>() shouldBe kbInfo
        verify { kbEndpoint.kbName() }
    }

    @Test
    fun kbList() = testApplication {
        setup()
        val kbs = listOf(KBInfo("10", "Glucose"), KBInfo("1", "Thyroids"), KBInfo("3", "Whatever"))
        every { serverApplication.kbList() } returns kbs
        val result = httpClient.get(KB_LIST) {
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<List<KBInfo>>() shouldBe kbs
        verify { serverApplication.kbList() }
    }

    @Test
    fun `should create a KB with a specified name`() = testApplication {
        setup()
        val kbInfoToReturnOnCreation = KBInfo("east", "Bondi")
        every { serverApplication.createKB(any(), any()) } returns kbInfoToReturnOnCreation
        val result = httpClient.post(CREATE_KB) {
            contentType(ContentType.Application.Json)
            setBody("Bondi")
        }
        result.status shouldBe HttpStatusCode.OK
        result.body<KBInfo>() shouldBe kbInfoToReturnOnCreation
        verify { serverApplication.createKB("Bondi", true) }
    }

    @Test
    fun deleteKB() = testApplication {
        setup()
        every { serverApplication.deleteKB(kbId) } returns Unit
        val result = httpClient.delete(DELETE_KB) {parameter(KB_ID, kbId)}
        result.status shouldBe HttpStatusCode.OK
        verify { serverApplication.deleteKB(kbId) }
    }

    @Test
    fun exportKB() = testApplication {
        setup()
        val zipFile = File("src/jvmTest/resources/export/Empty.zip")
        every { kbEndpoint.kbName() } returns KBInfo("Empty")
        every { kbEndpoint.exportKBToZip() } returns zipFile
        val result = httpClient.get(EXPORT_KB){
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.OK
        result.headers[HttpHeaders.ContentType] shouldBe "application/zip"
        result.headers[HttpHeaders.ContentDisposition] shouldBe "attachment; filename=Empty.zip"
        result.headers[HttpHeaders.ContentLength] shouldBe zipFile.length().toString()
        verify { kbEndpoint.exportKBToZip() }
    }

    @Test
    fun importKB() = testApplication {
        setup()
        val zipFile = File("src/jvmTest/resources/export/KBExported.zip")
        val zipBytes = zipFile.readBytes()
        every { serverApplication.importKBFromZip(zipBytes) } returns Unit
        val boundary = "WebAppBoundary"
        val response = httpClient.post(IMPORT_KB) {
            parameter(KB_ID, kbId)
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