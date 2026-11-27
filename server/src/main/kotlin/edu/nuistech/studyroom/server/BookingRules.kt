package edu.nuistech.studyroom.server

import edu.nuistech.studyroom.contract.Booking
import edu.nuistech.studyroom.contract.NewBooking
import edu.nuistech.studyroom.contract.TimeSlot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class BookingError { INVALID_REQUEST, ROOM_NOT_FOUND, BOOKING_NOT_FOUND, BOOKING_CONFLICT }

class BookingFailure(val code: BookingError, message: String) : RuntimeException(message)

internal object BookingRules {
    private val open = LocalTime.of(8, 0)
    private val close = LocalTime.of(22, 0)
    private val idPattern = Regex("[A-Z0-9][A-Z0-9-]{0,29}")

    fun identifier(value: String, field: String): String {
        val normalized = value.trim().uppercase(Locale.ROOT)
        if (!normalized.matches(idPattern)) {
            invalid("$field must be 1 to 30 letters, digits, or hyphens and start with a letter or digit.")
        }
        return normalized
    }

    fun validate(request: NewBooking, now: LocalDateTime): NewBooking {
        val start = request.start
        val end = request.end
        validateDate(start.toLocalDate())
        if (start.toLocalDate() != end.toLocalDate() || start >= end) {
            invalid("Start and end must be on the same date and end must be after start.")
        }
        if (start.toLocalTime() < open || end.toLocalTime() > close) {
            invalid("Choose a time between 08:00 and 22:00, campus time.")
        }
        if (start != start.truncatedTo(ChronoUnit.MINUTES) || end != end.truncatedTo(ChronoUnit.MINUTES)) {
            invalid("Choose start and end times at whole minutes.")
        }
        if (start <= now) invalid("The start time must be in the future.")
        return request.copy(
            roomId = identifier(request.roomId, "Room ID"),
            studentId = identifier(request.studentId, "Student ID"),
        )
    }

    fun validateDate(date: LocalDate) {
        if (date.year !in 1..9999) invalid("Use a four-digit year between 0001 and 9999.")
    }

    fun freeSlots(date: LocalDate, now: LocalDateTime, reservations: List<Booking>): List<TimeSlot> {
        val earliest = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        val closeAt = date.atTime(close)
        var cursor = maxOf(date.atTime(open), earliest)
        // Bookings use half-open intervals, so the next booking may start at the previous end.
        return buildList {
            for (booking in reservations.sortedBy { it.start }) {
                if (booking.end <= cursor) continue
                if (booking.start > cursor) add(TimeSlot(cursor, booking.start))
                cursor = maxOf(cursor, booking.end)
            }
            if (cursor < closeAt) add(TimeSlot(cursor, closeAt))
        }
    }

    private fun invalid(message: String): Nothing = throw BookingFailure(BookingError.INVALID_REQUEST, message)
}
