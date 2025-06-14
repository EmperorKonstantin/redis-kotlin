import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class RedisServer(private val port: Int) {
    private val storage = ConcurrentHashMap<String, String>()
    @Volatile
    private var running = false

    suspend fun start() = withContext(Dispatchers.IO) {
        val serverSocket = ServerSocket(port)
        running = true

        println("Redis server started on port $port")
        println("Supported commands: PING, SET, GET, DEL, EXISTS, KEYS")

        try {
            while (running) {
                try {
                    val clientSocket = serverSocket.accept()
                    launch { handleClient(clientSocket) }
                } catch (e: Exception) {
                    if (running) {
                        println("Error accepting client: ${e.message}")
                    }
                }
            }
        } finally {
            serverSocket.close()
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val clientAddress = socket.remoteSocketAddress
        println("Client connected: $clientAddress")

        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val input = line?.trim() ?: continue
                if (input.isEmpty()) continue

                val response = processCommand(input)
                writer.println(response)
            }
        } catch (e: Exception) {
            println("Error handling client $clientAddress: ${e.message}")
        } finally {
            try {
                socket.close()
                println("Client disconnected: $clientAddress")
            } catch (e: Exception) {
                println("Error closing socket: ${e.message}")
            }
        }
    }

    private fun processCommand(input: String): String {
        val parts = input.split(" ", limit = 3)
        val command = parts[0].uppercase()

        return when (command) {
            "PING" -> {
                if (parts.size > 1) parts[1] else "PONG"
            }

            "SET" -> {
                if (parts.size < 3) {
                    "-ERR wrong number of arguments for 'set' command"
                } else {
                    val key = parts[1]
                    val value = parts[2]
                    storage[key] = value
                    "+OK"
                }
            }

            "GET" -> {
                if (parts.size < 2) {
                    "-ERR wrong number of arguments for 'get' command"
                } else {
                    val key = parts[1]
                    val value = storage[key]
                    if (value != null) "+$value" else "$-1"
                }
            }

            "DEL" -> {
                if (parts.size < 2) {
                    "-ERR wrong number of arguments for 'del' command"
                } else {
                    val key = parts[1]
                    val existed = storage.remove(key) != null
                    ":${if (existed) 1 else 0}"
                }
            }

            "EXISTS" -> {
                if (parts.size < 2) {
                    "-ERR wrong number of arguments for 'exists' command"
                } else {
                    val key = parts[1]
                    ":${if (storage.containsKey(key)) 1 else 0}"
                }
            }

            "KEYS" -> {
                val pattern = if (parts.size > 1) parts[1] else "*"
                val keys = if (pattern == "*") {
                    storage.keys.toList()
                } else {
                    // Simple pattern matching for demonstration
                    storage.keys.filter { key ->
                        when {
                            pattern.endsWith("*") -> key.startsWith(pattern.dropLast(1))
                            pattern.startsWith("*") -> key.endsWith(pattern.drop(1))
                            else -> key == pattern
                        }
                    }
                }

                if (keys.isEmpty()) {
                    "*0"
                } else {
                    "*${keys.size}\r\n" + keys.joinToString("\r\n") { "+$it" }
                }
            }

            "FLUSHALL" -> {
                storage.clear()
                "+OK"
            }

            "QUIT" -> {
                "+OK"
            }

            else -> {
                "-ERR unknown command '$command'"
            }
        }
    }

    fun stop() {
        running = false
    }

    fun getStorageSize(): Int = storage.size
}