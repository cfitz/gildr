package dog.wildtulsa

import com.google.gson.Gson
import io.ktor.config.MapApplicationConfig
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.withTestApplication
import java.io.File

val gson = Gson()

fun TestApplicationRequest.addJwtHeader() = addHeader("Authorization", "Bearer ${getToken()}")

val testPlayer = Player(name = "Cool Data", password = "dadsonly")
val testGuild = Guild(name = "Rad Dads", description = "Dads Only!", ownerIds = listOf(testPlayer.id))

var testDataDir = File("${System.getProperty("java.io.tmpdir")}/.testData_${System.currentTimeMillis() / 1000L}")
val testStore: DAOFacadeCache = DAOFacadeCache(DAOFacadeStore(testDataDir), File(testDataDir, "cache"))

fun setup() {
    testStore.init()
    testStore.add(testPlayer)
    testStore.add(testGuild)
}

val simpleJwt = SimpleJWT("my-super-secret-for-jwt")

fun getToken() = simpleJwt.sign(testPlayer.id.toString())


fun withServer(block: TestApplicationEngine.() -> Unit) {
    setup()
    withTestApplication({
        (environment.config as MapApplicationConfig).apply {
            // Set here the properties
        }
        module(testing = true, store = testStore)
    }, block)
}
