import ru.sber.filesystem.VFilesystem
import java.io.IOException
import java.net.ServerSocket
import mu.KotlinLogging
import ru.sber.filesystem.VPath
import java.io.PrintWriter
import java.net.Socket

/**
 * A basic and very limited implementation of a file server that responds to GET
 * requests from HTTP clients.
 */
class FileServer {

    /**
     * Main entrypoint for the basic file server.
     *
     * @param socket Provided socket to accept connections on.
     * @param fs     A proxy filesystem to serve files from. See the VFilesystem
     *               class for more detailed documentation of its usage.
     * @throws IOException If an I/O error is detected on the server. This
     *                     should be a fatal error, your file server
     *                     implementation is not expected to ever throw
     *                     IOExceptions during normal operation.
     */
    @Throws(IOException::class)
    fun run(socket: ServerSocket, fs: VFilesystem) {
        socket.use {
            while (true) {
                LOG.info { "server started at port ${socket.localPort}. Listening for client connections..." }
                val socket = it.accept()
                handle(socket, fs)
            }
        }
    }

    private fun handle(socket: Socket, fs: VFilesystem) {
            socket.use { s ->
                val reader = s.getInputStream().bufferedReader()
                val clientRequest = reader.readLine()
                LOG.info { "receive from ${socket.remoteSocketAddress}  > clientRequest $clientRequest" }

                val writer = PrintWriter(s.getOutputStream())
                val serverResponse = getResponse(clientRequest, fs)
                writer.println(serverResponse)
                writer.flush()
                LOG.info { "send to ${socket.remoteSocketAddress} > $serverResponse" }
            }
    }

    private fun getResponse(clientRequest: String, fs: VFilesystem): String {
        val request = clientRequest.split(" ")
        val path = request[1]
        val file = fs.readFile(VPath(path))

        if (file != null){
            LOG.info { "STATUS OK" }
            return okResponse(file)
        }

        LOG.error { "ERROR" }
        return notFoundResponse()
    }

    private fun notFoundResponse() = "HTTP/1.0 404 Not Found\r\n\n" +
            "Server: FileServer\r\n\n" +
            "\r\n"

    private fun okResponse(file: String) = "HTTP/1.0 200 OK\r\n" +
            "Server: FileServer\r\n" +
            "\r\n" +
            "$file\r\n"

    companion object {
        val LOG = KotlinLogging.logger {}
    }
}