package dog.wildtulsa

import io.ktor.config.MapApplicationConfig
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.withTestApplication
import java.util.*

fun TestApplicationRequest.addJwtHeader() = addHeader("Authorization", "Bearer ${getToken()}")

val simpleJwt = SimpleJWT("my-super-secret-for-jwt")

fun getToken() = simpleJwt.sign(UUID.randomUUID().toString())

fun withServer(block: TestApplicationEngine.() -> Unit) {
    withTestApplication({
        (environment.config as MapApplicationConfig).apply {
            // Set here the properties
            put("gildr.session.cookie.key", "03e156f6058a13813816065")
            put("gildr.data.dir", "${System.getProperty("java.io.tmpdir")}/.testData_${System.currentTimeMillis() / 1000L}")
        }
        module(testing = true) }, block)
}
