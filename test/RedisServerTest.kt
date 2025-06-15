import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RedisServerTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `set and get command store and retrieve value`() = runBlocking {
        val server = spyk(RedisServer(6379))

        // Simulate SET command
        val setResult = server.javaClass.getDeclaredMethod("processCommand", String::class.java)
            .apply { isAccessible = true }
            .invoke(server, "SET foo bar")
        assertEquals("+OK", setResult)

        // Simulate GET command
        val getResult = server.javaClass.getDeclaredMethod("processCommand", String::class.java)
            .apply { isAccessible = true }
            .invoke(server, "GET foo")
        assertEquals("+bar", getResult)
    }

    @Test
    fun `del command removes key`() = runBlocking {
        val server = spyk(RedisServer(6379))
        // Pre-populate storage
        server.javaClass.getDeclaredMethod("processCommand", String::class.java)
            .apply { isAccessible = true }
            .invoke(server, "SET key1 value1")

        val delResult = server.javaClass.getDeclaredMethod("processCommand", String::class.java)
            .apply { isAccessible = true }
            .invoke(server, "DEL key1")
        assertEquals(":1", delResult)

        val getResult = server.javaClass.getDeclaredMethod("processCommand", String::class.java)
            .apply { isAccessible = true }
            .invoke(server, "GET key1")
        assertEquals("$-1", getResult)
    }

    @Test
    fun `unknown command returns error`() = runBlocking {
        val server = spyk(RedisServer(6379))
        val result = server.javaClass.getDeclaredMethod("processCommand", String::class.java)
            .apply { isAccessible = true }
            .invoke(server, "FOO")
        assertEquals("-ERR unknown command 'FOO'", result)
    }
}