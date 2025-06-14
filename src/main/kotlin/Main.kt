import kotlinx.coroutines.*
import java.io.*
import java.net.*
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
            val input = BufferedInputStream(socket.getInputStream())
            val output = socket.getOutputStream()

            while (true) {
                val command = parseRESP(input) ?: break
                val response = processCommand(command)
                output.write(response.toByteArray())
                output.flush()
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

    private fun parseRESP(input: BufferedInputStream): List<String>? {
        try {
            val firstByte = input.read()
            if (firstByte == -1) return null

            when (firstByte.toChar()) {
                '*' -> {
                    // Array
                    val arrayLength = readLine(input).toInt()
                    val result = mutableListOf<String>()

                    for (i in 0 until arrayLength) {
                        val elementType = input.read().toChar()
                        when (elementType) {
                            '$' -> {
                                // Bulk string
                                val length = readLine(input).toInt()
                                if (length == -1) {
                                    result.add("")
                                } else {
                                    val bytes = ByteArray(length)
                                    input.read(bytes)
                                    input.read() // \r
                                    input.read() // \n
                                    result.add(String(bytes))
                                }
                            }
                            '+' -> {
                                // Simple string
                                result.add(readLine(input))
                            }
                        }
                    }
                    return result
                }
                '+' -> {
                    // Simple string
                    return listOf(readLine(input))
                }
                '$' -> {
                    // Bulk string
                    val length = readLine(input).toInt()
                    if (length == -1) return listOf()
                    val bytes = ByteArray(length)
                    input.read(bytes)
                    input.read() // \r
                    input.read() // \n
                    return listOf(String(bytes))
                }
                else -> {
                    return null
                }
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun readLine(input: BufferedInputStream): String {
        val result = StringBuilder()
        while (true) {
            val byte = input.read()
            if (byte == '\r'.code) {
                input.read() // consume \n
                break
            }
            result.append(byte.toChar())
        }
        return result.toString()
    }

    private fun processCommand(args: List<String>): String {
        if (args.isEmpty()) return "-ERR empty command\r\n"

        val command = args[0].uppercase()

        return when (command) {
            "PING" -> {
                if (args.size > 1) {
                    "+${args[1]}\r\n"
                } else {
                    "+PONG\r\n"
                }
            }

            "SET" -> {
                if (args.size < 3) {
                    "-ERR wrong number of arguments for 'set' command\r\n"
                } else {
                    val key = args[1]
                    val value = args[2]
                    storage[key] = value
                    "+OK\r\n"
                }
            }

            "GET" -> {
                if (args.size < 2) {
                    "-ERR wrong number of arguments for 'get' command\r\n"
                } else {
                    val key = args[1]
                    val value = storage[key]
                    if (value != null) {
                        "$${value.length}\r\n$value\r\n"
                    } else {
                        "$-1\r\n"
                    }
                }
            }

            "DEL" -> {
                if (args.size < 2) {
                    "-ERR wrong number of arguments for 'del' command\r\n"
                } else {
                    val key = args[1]
                    val existed = storage.remove(key) != null
                    ":${if (existed) 1 else 0}\r\n"
                }
            }

            "EXISTS" -> {
                if (args.size < 2) {
                    "-ERR wrong number of arguments for 'exists' command\r\n"
                } else {
                    val key = args[1]
                    ":${if (storage.containsKey(key)) 1 else 0}\r\n"
                }
            }

            "KEYS" -> {
                val pattern = if (args.size > 1) args[1] else "*"
                val keys = if (pattern == "*") {
                    storage.keys.toList()
                } else {
                    storage.keys.filter { key ->
                        when {
                            pattern.endsWith("*") -> key.startsWith(pattern.dropLast(1))
                            pattern.startsWith("*") -> key.endsWith(pattern.drop(1))
                            else -> key == pattern
                        }
                    }
                }

                val response = StringBuilder()
                response.append("*${keys.size}\r\n")
                for (key in keys) {
                    response.append("$${key.length}\r\n$key\r\n")
                }
                response.toString()
            }

            "FLUSHALL" -> {
                storage.clear()
                "+OK\r\n"
            }

            "QUIT" -> {
                "+OK\r\n"
            }

            else -> {
                "-ERR unknown command '$command'\r\n"
            }
        }
    }

    fun stop() {
        running = false
    }

    fun getStorageSize(): Int = storage.size
}

fun main() = runBlocking {
    val server = RedisServer(6379)

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
