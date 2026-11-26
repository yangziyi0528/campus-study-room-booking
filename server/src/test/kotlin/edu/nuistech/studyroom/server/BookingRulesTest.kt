package edu.nuistech.studyroom.server

import edu.nuistech.studyroom.contract.Booking
import edu.nuistech.studyroom.contract.BookingStatus
import edu.nuistech.studyroom.contract.NewBooking
import edu.nuistech.studyroom.contract.TimeSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BookingRulesTest {
    private val day = LocalDate.of(2030, 2, 1)
    private val now = day.minusDays(1).atTime(12, 0)

    @Test
    fun `accepts opening boundaries and normalizes the room and student IDs`() {
        val request = NewBooking(" r101 ", " s-12 ", time(8), time(22))
        assertEquals(request.copy(roomId = "R101", studentId = "S-12"), BookingRules.validate(request, now))
    }

    @Test
    fun `rejects reversed past cross-day and out-of-hours bookings`() {
        val request = NewBooking("R101", "S1", time(9), time(10))
        listOf(
            request.copy(end = time(9)),
            request.copy(start = time(7)),
            request.copy(end = time(10).plusDays(1)),
            request.copy(start = time(9).plusSeconds(1)),
        ).forEach { invalid -> assertThrows(BookingFailure::class.java) { BookingRules.validate(invalid, now) } }
        assertThrows(BookingFailure::class.java) { BookingRules.validate(request, time(9)) }
    }

    @Test
    fun `adjacent bookings leave no gap between them`() {
        val reservations = listOf(booking(10, 11), booking(9, 10))
        assertEquals(listOf(TimeSlot(time(8), time(9)), TimeSlot(time(11), time(22))),
            BookingRules.freeSlots(day, now, reservations))
    }

    @Test
    fun `past days have no available time`() {
        assertTrue(BookingRules.freeSlots(day, now.plusDays(2), emptyList()).isEmpty())
    }

    private fun time(hour: Int): LocalDateTime = day.atTime(hour, 0)

    private fun booking(start: Int, end: Int) =
        Booking(UUID.randomUUID(), "R101", "S1", time(start), time(end), BookingStatus.ACTIVE)
}
