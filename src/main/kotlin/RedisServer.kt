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

                    // Creates new coroutine without blocking current thread
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

    // Design pattern: Each client connection runs in its own coroutine
    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val clientAddress = socket.remoteSocketAddress
        println("Client connected: $clientAddress")

        try {
            val input = socket.getInputStream()
            val writer = PrintWriter(socket.getOutputStream(), true)

            while (true) {
                val command = parseRESPCommand(input) ?: break
                val response = processCommand(command)
                writer.print(response)
                writer.flush()
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

    // Parses a RESP command from the input stream and returns it as a single string (space-separated)
    private fun parseRESPCommand(input: java.io.InputStream): String? {
        fun readLine(): String? {
            val sb = StringBuilder()
            while (true) {
                val b = input.read()
                if (b == -1) return null
                if (b == '\r'.code) {
                    if (input.read() == '\n'.code) break
                } else {
                    sb.append(b.toChar())
                }
            }
            return sb.toString()
        }

        val firstByte = input.read()
        if (firstByte == -1) return null
        if (firstByte.toChar() != '*') return null // Only handle RESP arrays

        val numElements = readLine()?.toIntOrNull() ?: return null
        val parts = mutableListOf<String>()
        repeat(numElements) {
            val type = input.read()
            if (type == -1) return null
            if (type.toChar() != '$') return null // Only handle bulk strings
            val len = readLine()?.toIntOrNull() ?: return null
            if (len < 0) {
                parts.add("")
                return@repeat
            }
            val buf = ByteArray(len)
            var read = 0
            while (read < len) {
                val r = input.read(buf, read, len - read)
                if (r == -1) return null
                read += r
            }
            // Consume \r\n
            input.read()
            input.read()
            parts.add(String(buf))
        }
        return parts.joinToString(" ")
    }

    private fun processCommand(input: String): String {
        // Split the input into command and arguments
        // limit = 3: Splits into maximum 3 parts
        // Example: "SET key value with spaces" → ["SET", "key", "value with spaces"]
        // Preserves spaces in values
        val parts = input.split(" ", limit = 3)
        val command = parts[0].uppercase()

        return when (command) {
            "PING" -> {
                if (parts.size > 1) "+${parts[1]}\r\n" else "+PONG\r\n"
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

            "ECHO" -> {
                if (parts.size < 2) {
                    "-ERR wrong number of arguments for 'echo' command\r\n"
                } else {
                    "+${parts[1]}\r\n"
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