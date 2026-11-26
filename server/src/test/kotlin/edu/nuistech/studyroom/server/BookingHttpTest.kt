package edu.nuistech.studyroom.server

import com.fasterxml.jackson.databind.JsonNode
import edu.nuistech.studyroom.contract.NewBooking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.datasource.url=jdbc:h2:mem:booking-http;DB_CLOSE_DELAY=-1"],
)
class BookingHttpTest(@Autowired private val http: TestRestTemplate) {
    private val today = LocalDate.now(ZoneId.of("Asia/Shanghai"))

    @Test
    fun `lists rooms and returns a date-specific availability response`() {
        val rooms = http.getForEntity("/api/v1/rooms", JsonNode::class.java)
        assertEquals(HttpStatus.OK, rooms.statusCode)
        assertEquals("R101", rooms.body!!.first()["id"].asText())
        val date = today.plusDays(1)
        val result = http.getForEntity("/api/v1/rooms/R101/availability?date=$date", JsonNode::class.java)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(date.toString(), result.body!!["date"].asText())
        assertTrue(result.body!!["slots"][0]["start"].asText().startsWith("${date}T08:00"))
    }

    @Test
    fun `rejects overlap but accepts adjacency`() {
        val request = booking(2, "S1", 9, 10)
        assertEquals(HttpStatus.CREATED, create(request).statusCode)
        val conflict = create(request.copy(studentId = "S2", start = request.start.plusMinutes(30)))
        assertEquals(HttpStatus.CONFLICT, conflict.statusCode)
        assertEquals("BOOKING_CONFLICT", conflict.body!!["code"].asText())
        assertEquals(HttpStatus.CREATED, create(request.copy(start = request.end, end = request.end.plusHours(1))).statusCode)
    }

    @Test
    fun `only owner cancels and cancelled slots can be booked again`() {
        val request = booking(3, "S3", 9, 10)
        val id = create(request).body!!["id"].asText()
        assertEquals(HttpStatus.NOT_FOUND, cancel(id, "S4").statusCode)
        repeat(2) { assertEquals("CANCELLED", cancel(id, "S3").body!!["status"].asText()) }
        val history = http.getForEntity("/api/v1/bookings?studentId=S3", JsonNode::class.java)
        assertEquals("CANCELLED", history.body!!.first()["status"].asText())
        assertEquals(HttpStatus.CREATED, create(request.copy(studentId = "S4")).statusCode)
    }

    @Test
    fun `rejects unknown rooms and malformed dates using documented codes`() {
        val unknown = create(booking(5, "S5", 9, 10).copy(roomId = "UNKNOWN"))
        assertEquals(HttpStatus.NOT_FOUND, unknown.statusCode)
        assertEquals("ROOM_NOT_FOUND", unknown.body!!["code"].asText())
        val malformed = http.getForEntity("/api/v1/rooms/R101/availability?date=no-date", JsonNode::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, malformed.statusCode)
        assertEquals("INVALID_REQUEST", malformed.body!!["code"].asText())
    }

    @Test
    fun `only one of two simultaneous requests gets the slot`() {
        val request = booking(4, "S6", 9, 10)
        val executor = Executors.newFixedThreadPool(2)
        val start = CyclicBarrier(2)
        try {
            val responses = listOf("S6", "S7").map { student ->
                executor.submit<Int> {
                    start.await(10, TimeUnit.SECONDS)
                    create(request.copy(studentId = student)).statusCode.value()
                }
            }
            assertEquals(listOf(201, 409), responses.map { it.get(10, TimeUnit.SECONDS) }.sorted())
        } finally {
            executor.shutdownNow()
        }
    }

    private fun booking(days: Long, student: String, start: Int, end: Int): NewBooking {
        val date = today.plusDays(days)
        return NewBooking("R101", student, date.atTime(start, 0), date.atTime(end, 0))
    }

    private fun create(request: NewBooking) = http.postForEntity("/api/v1/bookings", request, JsonNode::class.java)

    private fun cancel(id: String, student: String) =
        http.exchange("/api/v1/bookings/$id?studentId=$student", HttpMethod.DELETE, HttpEntity.EMPTY, JsonNode::class.java)
}
