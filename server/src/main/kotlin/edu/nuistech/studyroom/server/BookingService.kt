package edu.nuistech.studyroom.server

import edu.nuistech.studyroom.contract.Availability
import edu.nuistech.studyroom.contract.NewBooking
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class BookingService(private val repository: BookingRepository, private val clock: Clock) {
    fun rooms() = repository.rooms()

    fun availability(roomId: String, date: LocalDate): Availability {
        val id = BookingRules.identifier(roomId, "Room ID")
        val room = repository.room(id) ?: throw BookingFailure(BookingError.ROOM_NOT_FOUND, "Room $id was not found.")
        val now = LocalDateTime.now(clock)
        val bookings = if (date < now.toLocalDate()) emptyList() else repository.activeOn(id, date)
        return Availability(room, date, BookingRules.freeSlots(date, now, bookings))
    }

    fun create(request: NewBooking) = repository.create(BookingRules.validate(request, LocalDateTime.now(clock)))

    fun bookings(studentId: String) = repository.forStudent(BookingRules.identifier(studentId, "Student ID"))

    fun cancel(bookingId: UUID, studentId: String) =
        repository.cancel(bookingId, BookingRules.identifier(studentId, "Student ID"))
}
