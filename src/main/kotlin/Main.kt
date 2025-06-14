import kotlinx.coroutines.launch
import java.net.ServerSocket
import kotlinx.coroutines.runBlocking
//import import io.codecrafters.build_your_own_redis.RedisServer.handleClient
//import HandleClient.*
//import java.net.Socket

fun main() = runBlocking {
    val serverSocket = ServerSocket(6379)
    serverSocket.reuseAddress = true

    println("Server started on port 6379")

    while (true) {
        val client = serverSocket.accept()
        launch {
            handleClient(client)
        }
    }
}

suspend fun handleClient(socket: java.net.Socket) {
    val input = socket.getInputStream()
    val output = socket.getOutputStream()
    try {
        val buffer = ByteArray(1024)
        val bytesRead = input.read(buffer)
        if (bytesRead > 0) {
            output.write("+PONG\r\n".toByteArray())
            output.flush()
        }
    } finally {
        socket.close()
    }
}
