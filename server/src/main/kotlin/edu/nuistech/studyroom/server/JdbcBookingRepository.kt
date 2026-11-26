package edu.nuistech.studyroom.server

import edu.nuistech.studyroom.contract.Booking
import edu.nuistech.studyroom.contract.BookingStatus
import edu.nuistech.studyroom.contract.NewBooking
import edu.nuistech.studyroom.contract.Room
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
class JdbcBookingRepository(private val jdbc: JdbcTemplate) : BookingRepository {
    private val roomRow = RowMapper { row, _ ->
        Room(row.getString("id"), row.getString("name"), row.getInt("capacity"))
    }
    private val bookingRow = RowMapper { row, _ ->
        Booking(
            row.getObject("id", UUID::class.java), row.getString("room_id"), row.getString("student_id"),
            row.getObject("start_at", LocalDateTime::class.java), row.getObject("end_at", LocalDateTime::class.java),
            BookingStatus.valueOf(row.getString("status")),
        )
    }

    override fun rooms(): List<Room> = jdbc.query("SELECT id, name, capacity FROM rooms ORDER BY id", roomRow)

    override fun room(roomId: String): Room? =
        jdbc.query("SELECT id, name, capacity FROM rooms WHERE id = ?", roomRow, roomId).firstOrNull()

    override fun activeOn(roomId: String, date: LocalDate): List<Booking> =
        jdbc.query(DAILY_BOOKINGS, bookingRow, roomId, date.atStartOfDay(), date.plusDays(1).atStartOfDay())

    @Transactional
    override fun create(request: NewBooking): Booking {
        lockRoom(request.roomId)
        val conflict = jdbc.query(
            """
                SELECT id FROM bookings
                WHERE room_id = ? AND status = 'ACTIVE' AND start_at >= ? AND start_at < ? AND end_at > ?
                FETCH FIRST 1 ROW ONLY
            """.trimIndent(),
            RowMapper { row, _ -> row.getObject(1, UUID::class.java) },
            request.roomId, request.start.toLocalDate().atStartOfDay(), request.end, request.start,
        ).isNotEmpty()
        if (conflict) {
            throw BookingFailure(
                BookingError.BOOKING_CONFLICT, "That room is already booked during the requested time.",
            )
        }
        val booking = Booking(
            UUID.randomUUID(), request.roomId, request.studentId, request.start, request.end, BookingStatus.ACTIVE,
        )
        jdbc.update(
            "INSERT INTO bookings (id, room_id, student_id, start_at, end_at, status) VALUES (?, ?, ?, ?, ?, ?)",
            booking.id, booking.roomId, booking.studentId, booking.start, booking.end, booking.status.name,
        )
        return booking
    }

    override fun forStudent(studentId: String): List<Booking> =
        jdbc.query("SELECT * FROM bookings WHERE student_id = ? ORDER BY start_at, id", bookingRow, studentId)

    @Transactional
    override fun cancel(bookingId: UUID, studentId: String): Booking {
        val booking = jdbc.query(
            "SELECT * FROM bookings WHERE id = ? AND student_id = ?", bookingRow, bookingId, studentId,
        ).firstOrNull() ?: throw BookingFailure(BookingError.BOOKING_NOT_FOUND, "Booking was not found.")
        lockRoom(booking.roomId)
        if (booking.status == BookingStatus.ACTIVE) {
            jdbc.update("UPDATE bookings SET status = 'CANCELLED' WHERE id = ?", bookingId)
        }
        return booking.copy(status = BookingStatus.CANCELLED)
    }

    private fun lockRoom(roomId: String) {
        // Taking the same row lock before checking and writing prevents simultaneous double-booking.
        val present = jdbc.query(
            "SELECT id FROM rooms WHERE id = ? FOR UPDATE", RowMapper { row, _ -> row.getString(1) }, roomId,
        )
        if (present.isEmpty()) throw BookingFailure(BookingError.ROOM_NOT_FOUND, "Room $roomId was not found.")
    }

    companion object {
        internal val DAILY_BOOKINGS = """
            SELECT * FROM bookings
            WHERE room_id = ? AND status = 'ACTIVE' AND start_at >= ? AND start_at < ?
            ORDER BY start_at
        """.trimIndent()
    }
}
