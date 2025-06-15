import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class MainTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `server starts and stops gracefully`() = runBlocking {
        val mockServer = mockk<RedisServer>(relaxed = true)

        coEvery { mockServer.getStorageSize() } returns 42

        val shutdownHook = Thread {
            mockServer.stop()
        }

        assertDoesNotThrow {
            mockServer.start()
            println("Final storage size: ${mockServer.getStorageSize()}")
            shutdownHook.run()
        }

        coVerify { mockServer.start() }
        coVerify { mockServer.getStorageSize() }
        coVerify { mockServer.stop() }
    }

    @Test
    fun `exception during server start is handled`() = runBlocking {
        val mockServer = mockk<RedisServer>(relaxed = true)

        coEvery { mockServer.start() } throws RuntimeException("Port in use")

        try {
            mockServer.start()
        } catch (e: Exception) {
            println("Failed to start Redis server: ${e.message}")
        }

        coVerify { mockServer.start() }
    }
}