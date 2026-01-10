package io.rippledown.server.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.verify
import io.rippledown.constants.api.CREATE_KB
import io.rippledown.constants.api.CREATE_KB_FROM_SAMPLE
import io.rippledown.constants.api.DELETE_KB
import io.rippledown.constants.api.EXPORT_KB
import io.rippledown.constants.api.IMPORT_KB
import io.rippledown.constants.api.KB_LIST
import io.rippledown.constants.api.SELECT_KB
import io.rippledown.constants.server.KB_ID
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import io.rippledown.server.OpenRDRServerTestBase
import java.io.File
import kotlin.test.Test

class KBManagementTest: OpenRDRServerTestBase() {

     @Test
    fun kbList() = testApplication {
         setupServer()
         val kbs = listOf(KBInfo("10", "Glucose"), KBInfo("1", "Thyroids"), KBInfo("3", "Whatever"))
         every { serverApplication.kbList() } returns kbs
         val result = httpClient.get(KB_LIST) {
             parameter(KB_ID, kbId)
         }
         result.status shouldBe HttpStatusCode.Companion.OK
         result.body<List<KBInfo>>() shouldBe kbs
         verify { serverApplication.kbList() }
     }

    @Test
    fun selectKB() = testApplication {
        setupServer()
        val kbInfo = KBInfo("10", "Glucose")
        every { serverApplication.selectKB(kbInfo.id) } returns kbInfo
        val result = httpClient.post(SELECT_KB) {
            setBody(kbInfo.id)
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.body<KBInfo>() shouldBe kbInfo
        verify { serverApplication.selectKB(kbInfo.id) }
    }

    @Test
    fun `should create a KB with a specified name`() = testApplication {
        setupServer()
        val kbInfoToReturnOnCreation = KBInfo("east", "Bondi")
        every { serverApplication.createKB(any(), any()) } returns kbInfoToReturnOnCreation
        val result = httpClient.post(CREATE_KB) {
            contentType(ContentType.Application.Json)
            setBody("Bondi")
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.body<KBInfo>() shouldBe kbInfoToReturnOnCreation
        verify { serverApplication.createKB("Bondi", true) }
    }

    @Test
    fun `create KB from sample`() = testApplication {
        setupServer()
        val kbInfoToReturn = KBInfo("east", "Bondi")
        every { serverApplication.createKBFromSample(any(), any()) } returns kbInfoToReturn
        val result = httpClient.post(CREATE_KB_FROM_SAMPLE) {
            contentType(ContentType.Application.Json)
            setBody(Pair(kbInfoToReturn.name, SampleKB.TSH))
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.body<KBInfo>() shouldBe kbInfoToReturn
        verify { serverApplication.createKBFromSample(kbInfoToReturn.name, SampleKB.TSH) }
    }

    @Test
    fun deleteKB() = testApplication {
        setupServer()
        every { serverApplication.deleteKB(kbId) } returns Unit
        val result = httpClient.delete(DELETE_KB) { parameter(KB_ID, kbId) }
        result.status shouldBe HttpStatusCode.Companion.OK
        verify { serverApplication.deleteKB(kbId) }
    }

    @Test
    fun exportKB() = testApplication {
        setupServer()
        val zipFile = File("src/test/resources/export/Empty.zip")
        every { kbEndpoint.kbInfo() } returns KBInfo("Empty")
        every { kbEndpoint.exportKBToZip() } returns zipFile
        val result = httpClient.get(EXPORT_KB) {
            parameter(KB_ID, kbId)
        }
        result.status shouldBe HttpStatusCode.Companion.OK
        result.headers[HttpHeaders.ContentType] shouldBe "application/zip"
        result.headers[HttpHeaders.ContentDisposition] shouldBe "attachment; filename=Empty.zip"
        result.headers[HttpHeaders.ContentLength] shouldBe zipFile.length().toString()
        verify { kbEndpoint.exportKBToZip() }
    }

    @Test
    fun importKB() = testApplication {
        setupServer()
        val zipFile = File("src/test/resources/export/Whatever.zip")
        val zipBytes = zipFile.readBytes()
        val toReturn = KBInfo("3435", "Something")
        every { serverApplication.importKBFromZip(zipBytes) } returns toReturn
        val boundary = "WebAppBoundary"
        val response = httpClient.post(IMPORT_KB) {
            parameter(KB_ID, kbId)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("zip", zipBytes, Headers.Companion.build {
                            append(HttpHeaders.ContentType, "application/zip")
                            append(HttpHeaders.ContentDisposition, "filename=\"KBExported.zip\"")
                        })
                    },
                    boundary,
                    ContentType.MultiPart.FormData.withParameter("boundary", boundary)
                )
            )
        }
        response.body<KBInfo>() shouldBe toReturn
        verify { serverApplication.importKBFromZip(zipBytes) }
    }
 }