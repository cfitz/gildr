package dog.wildtulsa

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.content.*
import io.ktor.http.content.*
import io.ktor.features.*
import io.ktor.auth.*
import com.fasterxml.jackson.databind.*
import io.ktor.config.MapApplicationConfig
import io.ktor.jackson.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.util.KtorExperimentalAPI

class ApplicationTest {
    @KtorExperimentalAPI
    @Test
    fun testRoot() {
        withServer {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }
}
