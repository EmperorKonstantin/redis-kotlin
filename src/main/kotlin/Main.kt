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
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            output.write("+PONG\r\n".toByteArray())
            output.flush()
        }
    } finally {
        socket.close()
    }
}
//suspend fun handleClient(socket: java.net.Socket) {
//    val input = socket.getInputStream()
//    val output = socket.getOutputStream()
//    try {
//        while (true) {
//            val firstByte = input.read()
//            if (firstByte == -1) break
//            if (firstByte.toChar() == '*') {
//                val arrayLen = input.readLineCRLF().toInt()
//                val items = mutableListOf<String>()
//                repeat(arrayLen) {
//                    val type = input.read()
//                    if (type.toChar() == '$') {
//                        val strLen = input.readLineCRLF().toInt()
//                        val str = ByteArray(strLen)
//                        input.readFully(str)
//                        input.readLineCRLF() // consume \r\n
//                        items.add(String(str))
//                    }
//                }
//                if (items.isNotEmpty() && items[0].equals("PING", ignoreCase = true)) {
//                    output.write("+PONG\r\n".toByteArray())
//                    output.flush()
//                }
//            } else {
//                // RESP expects +, -, :, $, or * as first byte
//                output.write("-ERR unknown command\r\n".toByteArray())
//                output.flush()
//                break
//            }
//        }
//    } finally {
//        socket.close()
//    }
//}

// Helper extension functions:
fun java.io.InputStream.readLineCRLF(): String {
    val sb = StringBuilder()
    while (true) {
        val c = this.read()
        if (c == -1) break
        if (c == '\r'.code) {
            if (this.read() == '\n'.code) break
        } else {
            sb.append(c.toChar())
        }
    }
    return sb.toString()
}

fun java.io.InputStream.readFully(buf: ByteArray) {
    var read = 0
    while (read < buf.size) {
        val r = this.read(buf, read, buf.size - read)
        if (r == -1) throw java.io.EOFException()
        read += r
    }
}