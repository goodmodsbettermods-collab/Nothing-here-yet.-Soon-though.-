package com.goodmods.questmodfilestorage

import java.io.*
import java.net.ServerSocket
import java.net.Socket

/**
 * Very small HTTP server that supports a simple PUT /upload?name=FILENAME
 * to write the raw body to a file in the provided root directory.
 *
 * Usage: start() then use curl:
 * curl -X PUT --data-binary @localfile "http://HEADSET_IP:8080/upload?name=mods/myfile.zip"
 */
class SimpleHttpServer(private val rootDir: File, private val port: Int = 8080) {
    private var running = false
    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (running) return
        running = true
        serverThread = Thread {
            try {
                serverSocket = ServerSocket(port)
                while (running) {
                    val socket = serverSocket!!.accept()
                    handle(socket)
                }
            } catch (e: IOException) {
                // stopped
            }
        }
        serverThread?.start()
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        try { serverThread?.interrupt() } catch (_: Exception) {}
    }

    private fun handle(socket: Socket) {
        Thread {
            socket.use { s ->
                val input = BufferedInputStream(s.getInputStream())
                val reader = BufferedReader(InputStreamReader(input))
                val requestLine = reader.readLine() ?: return@Thread
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@Thread
                val method = parts[0]
                val path = parts[1]

                val headers = mutableMapOf<String, String>()
                var line: String? = reader.readLine()
                var contentLength = 0
                while (line != null && line.isNotEmpty()) {
                    val idx = line.indexOf(":")
                    if (idx > 0) {
                        val key = line.substring(0, idx).trim()
                        val value = line.substring(idx + 1).trim()
                        headers[key.lowercase()] = value
                        if (key.equals("Content-Length", ignoreCase = true)) {
                            contentLength = value.toIntOrNull() ?: 0
                        }
                    }
                    line = reader.readLine()
                }

                if (method == "PUT" && path.startsWith("/upload")) {
                    // parse query
                    val name = path.substringAfter("?name=", "uploaded.bin")
                    val cleanName = name.replace("..", "_")
                    val outFile = File(rootDir, cleanName)
                    outFile.parentFile?.mkdirs()

                    val out = FileOutputStream(outFile)
                    val buffer = ByteArray(8192)
                    var remaining = contentLength
                    if (contentLength > 0) {
                        while (remaining > 0) {
                            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            remaining -= read
                        }
                    } else {
                        // read until socket closes
                        var r = input.read(buffer)
                        while (r > 0) {
                            out.write(buffer, 0, r)
                            r = input.read(buffer)
                        }
                    }
                    out.flush(); out.close()

                    val resp = "HTTP/1.1 201 Created\r\nContent-Length: 0\r\n\r\n"
                    s.getOutputStream().write(resp.toByteArray())
                    s.getOutputStream().flush()
                } else {
                    val resp = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
                    s.getOutputStream().write(resp.toByteArray())
                    s.getOutputStream().flush()
                }
            }
        }.start()
    }
}
