import java.net.Socket

class HandleClient(client: Socket) {
    suspend fun handleClient(socket: java.net.Socket) {
        val input = socket.getInputStream().bufferedReader()
        val output = socket.getOutputStream().bufferedWriter()
        try {
            while (true) {
                val line = input.readLine() ?: break
                if (line.contains("PING", ignoreCase = true)) {
                    output.write("+PONG\r\n")
                    output.flush()
                }
            }
        } finally {
            socket.close()
        }
    }

    companion object

}