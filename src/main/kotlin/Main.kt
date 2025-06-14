import java.net.ServerSocket

fun main(args: Array<String>) {
    // You can use print statements as follows for debugging,
    // they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!")

    // Uncomment this block to pass the first stage
    val serverSocket = ServerSocket(6379)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    val response = serverSocket.accept() // Wait for connection from client.
    val input = response.getInputStream().bufferedReader()
    val output = response.getOutputStream().bufferedWriter()

    while (true) {
        val line = input.readLine() ?: break
        if (line.contains("PING", ignoreCase = true)) {
            output.write("+PONG\r\n")
            output.flush()
        }
    }

    println("accepted new connection")
}
