package edu.nuistech.studyroom.cli

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import edu.nuistech.studyroom.contract.ApiError
import edu.nuistech.studyroom.contract.Availability
import edu.nuistech.studyroom.contract.Booking
import edu.nuistech.studyroom.contract.NewBooking
import edu.nuistech.studyroom.contract.Room
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

internal class ClientFailure(message: String, val exitCode: Int, cause: Throwable? = null) :
    RuntimeException(message, cause)

internal class BookingClient(endpoint: String) {
    private val base = validateEndpoint(endpoint)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    private val json = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun rooms(): List<Room> = exchange(request("/rooms").GET())

    fun availability(roomId: String, date: LocalDate): Availability =
        exchange(request("/rooms/${encode(roomId)}/availability?date=$date").GET())

    fun bookings(studentId: String): List<Booking> =
        exchange(request("/bookings?studentId=${encode(studentId)}").GET())

    fun book(booking: NewBooking): Booking = exchange(
        request("/bookings").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(booking), UTF_8)),
    )

    fun cancel(bookingId: UUID, studentId: String): Booking =
        exchange(request("/bookings/$bookingId?studentId=${encode(studentId)}").DELETE())

    private fun request(path: String): HttpRequest.Builder =
        HttpRequest.newBuilder(URI.create("$base/api/v1$path"))
            .timeout(Duration.ofSeconds(5)).header("Accept", "application/json")

    private inline fun <reified T> exchange(builder: HttpRequest.Builder): T {
        val response = send(builder.build())
        checkStatus(response)
        return try {
            json.readValue<T>(response.body())
        } catch (failure: JsonProcessingException) {
            throw ClientFailure("The booking service returned an invalid response.", 1, failure)
        }
    }

    private fun send(request: HttpRequest): HttpResponse<String> =
        try {
            http.send(request, HttpResponse.BodyHandlers.ofString(UTF_8))
        } catch (failure: IOException) {
            throw ClientFailure(
                "Cannot reach the booking service at $base. Check the address and try again.", 1, failure,
            )
        } catch (failure: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ClientFailure("The booking request was interrupted.", 1, failure)
        }

    private fun checkStatus(response: HttpResponse<String>) {
        if (response.statusCode() !in 200..299) {
            val error = runCatching { json.readValue<ApiError>(response.body()) }.getOrNull()
            val detail = error?.let { "${it.code}: ${it.message}" } ?: "The service rejected the request."
            throw ClientFailure("HTTP ${response.statusCode()} $detail", 3)
        }
    }

}

private fun encode(value: String): String = URLEncoder.encode(value, UTF_8).replace("+", "%20")

private fun validateEndpoint(endpoint: String): String {
    val uri = runCatching { URI.create(endpoint) }.getOrNull()
    val hasOrigin = uri != null && uri.scheme in listOf("http", "https") && uri.host != null
    val hasValidPort = (uri?.port ?: -2) in -1..65535
    val modifiers = listOf(uri?.rawUserInfo, uri?.rawQuery, uri?.rawFragment)
    val originOnly = uri?.path in listOf("", "/") && modifiers.all { it == null }
    if (!hasOrigin || !hasValidPort || !originOnly) {
        throw ClientFailure("Use an HTTP or HTTPS server origin, such as http://127.0.0.1:8080.", 2)
    }
    return endpoint.trimEnd('/')
}
