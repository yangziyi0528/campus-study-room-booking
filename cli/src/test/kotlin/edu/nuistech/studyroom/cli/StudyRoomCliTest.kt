package edu.nuistech.studyroom.cli

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetSocketAddress

class StudyRoomCliTest {
    private data class Result(val exitCode: Int, val out: String, val err: String)

    @Test
    fun `help is available without a server and invalid flags are rejected`() {
        assertTrue(run("--help").out.contains("bookings"))
        assertEquals(2, run("free", "--room", "R101", "--date", "invalid").exitCode)
        assertEquals(2, run("--server", "file:///tmp", "rooms").exitCode)
    }

    @Test
    fun `reads the documented room response`() {
        withResponse(200, """[{"id":"R101","name":"East Study Room","capacity":4}]""") { endpoint ->
            val result = run("rooms", "--server", endpoint)
            assertEquals(0, result.exitCode)
            assertTrue(result.out.contains("R101  East Study Room (capacity 4)"))
        }
    }

    @Test
    fun `shows a server rejection without throwing a stack trace`() {
        withResponse(409, """{"code":"BOOKING_CONFLICT","message":"Room unavailable."}""") { endpoint ->
            val result = run("--server", endpoint, "rooms")
            assertEquals(3, result.exitCode)
            assertTrue(result.err.contains("BOOKING_CONFLICT"))
        }
    }

    @Test
    fun `reports a connection failure on standard error`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val endpoint = "http://127.0.0.1:${server.address.port}"
        server.stop(0)
        val result = run("--server", endpoint, "rooms")
        assertEquals(1, result.exitCode)
        assertTrue(result.err.contains("Cannot reach the booking service"))
    }

    private fun withResponse(status: Int, body: String, check: (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/v1/rooms") { exchange ->
            val bytes = body.toByteArray()
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            check("http://127.0.0.1:${server.address.port}")
        } finally {
            server.stop(0)
        }
    }

    private fun run(vararg args: String): Result {
        val out = StringWriter()
        val err = StringWriter()
        val code = runCli(args, PrintWriter(out), PrintWriter(err))
        return Result(code, out.toString(), err.toString())
    }
}
