import kotlinx.coroutines.*

fun main() = runBlocking {
    val server = RedisServer(6379) // Default Redis port

    // Add shutdown hook to gracefully stop the server
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nShutting down Redis server...")
        println("Final storage size: ${server.getStorageSize()}")
        server.stop()
    })

    try {
        server.start()
    } catch (e: Exception) {
        println("Failed to start Redis server: ${e.message}")
    }
}