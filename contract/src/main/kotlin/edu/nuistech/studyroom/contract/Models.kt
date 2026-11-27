package edu.nuistech.studyroom.contract

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Room(val id: String, val name: String, val capacity: Int)

data class TimeSlot(val start: LocalDateTime, val end: LocalDateTime)

data class Availability(val room: Room, val date: LocalDate, val slots: List<TimeSlot>)

data class NewBooking(val roomId: String, val studentId: String, val start: LocalDateTime, val end: LocalDateTime)

enum class BookingStatus { ACTIVE, CANCELLED }

data class Booking(
    val id: UUID,
    val roomId: String,
    val studentId: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val status: BookingStatus,
)

data class ApiError(val code: String, val message: String)
