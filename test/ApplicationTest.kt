package dog.wildtulsa

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @KtorExperimentalAPI
    @Test
    fun testRoot() {
        withServer {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}
