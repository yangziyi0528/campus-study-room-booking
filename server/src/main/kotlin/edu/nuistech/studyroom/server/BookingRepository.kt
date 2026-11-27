package edu.nuistech.studyroom.server

import edu.nuistech.studyroom.contract.Booking
import edu.nuistech.studyroom.contract.NewBooking
import edu.nuistech.studyroom.contract.Room
import java.time.LocalDate
import java.util.UUID

interface BookingRepository {
    fun rooms(): List<Room>
    fun room(roomId: String): Room?
    fun activeOn(roomId: String, date: LocalDate): List<Booking>
    fun create(request: NewBooking): Booking
    fun forStudent(studentId: String): List<Booking>
    fun cancel(bookingId: UUID, studentId: String): Booking
}
