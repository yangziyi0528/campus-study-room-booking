package edu.nuistech.studyroom.server

import edu.nuistech.studyroom.contract.ApiError
import edu.nuistech.studyroom.contract.NewBooking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class BookingController(private val service: BookingService) {
    @GetMapping("/rooms")
    fun rooms() = service.rooms()

    @GetMapping("/rooms/{roomId}/availability")
    fun availability(@PathVariable roomId: String, @RequestParam date: LocalDate) = service.availability(roomId, date)

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: NewBooking) = service.create(request)

    @GetMapping("/bookings")
    fun bookings(@RequestParam studentId: String) = service.bookings(studentId)

    @DeleteMapping("/bookings/{id}")
    fun cancel(@PathVariable id: UUID, @RequestParam studentId: String) = service.cancel(id, studentId)
}

@RestControllerAdvice
class BookingProblemHandler {
    @ExceptionHandler(BookingFailure::class)
    fun bookingFailure(error: BookingFailure): ResponseEntity<ApiError> {
        val status = when (error.code) {
            BookingError.INVALID_REQUEST -> HttpStatus.BAD_REQUEST
            BookingError.ROOM_NOT_FOUND, BookingError.BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND
            BookingError.BOOKING_CONFLICT -> HttpStatus.CONFLICT
        }
        return ResponseEntity.status(status).body(ApiError(error.code.name, error.message.orEmpty()))
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun invalidRequest() = ApiError("INVALID_REQUEST", "Check required fields and use ISO dates and times.")
}
