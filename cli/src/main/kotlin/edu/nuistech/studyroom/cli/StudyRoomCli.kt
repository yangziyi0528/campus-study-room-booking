package edu.nuistech.studyroom.cli

import edu.nuistech.studyroom.contract.Booking
import edu.nuistech.studyroom.contract.NewBooking
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.ScopeType
import java.io.PrintWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(runCli(args, PrintWriter(System.out, true), PrintWriter(System.err, true)))
}

@Suppress("SpreadOperator") // Picocli accepts command arguments as a vararg.
internal fun runCli(args: Array<out String>, out: PrintWriter, err: PrintWriter): Int {
    val command = CommandLine(StudyRoomCommands(out))
    command.out = out
    command.err = err
    command.setExecutionExceptionHandler { failure, cli, _ ->
        if (failure is ClientFailure) {
            cli.err.println("Error: ${failure.message}")
            failure.exitCode
        } else {
            throw failure
        }
    }
    return command.execute(*args)
}

@Command(
    name = "study-room", mixinStandardHelpOptions = true, version = ["0.1.0"],
    description = ["Find and book a campus study room."],
    subcommands = [
        StudyRoomCommands.Rooms::class, StudyRoomCommands.Free::class, StudyRoomCommands.Book::class,
        StudyRoomCommands.Bookings::class, StudyRoomCommands.Cancel::class,
    ],
)
internal class StudyRoomCommands(private val out: PrintWriter) {
    @Option(names = ["--server"], scope = ScopeType.INHERIT, description = ["Booking service origin."])
    private var server = "http://127.0.0.1:8080"

    private fun connect() = BookingClient(server)

    private fun printBooking(booking: Booking) {
        out.println("${booking.id}  ${booking.roomId}  ${booking.start} to ${booking.end}  ${booking.status}")
    }

    @Command(name = "rooms", mixinStandardHelpOptions = true, description = ["List study rooms."])
    class Rooms : Runnable {
        @ParentCommand private lateinit var parent: StudyRoomCommands

        override fun run() {
            parent.connect().rooms().forEach { room ->
                parent.out.println("${room.id}  ${room.name} (capacity ${room.capacity})")
            }
        }
    }

    @Command(name = "free", mixinStandardHelpOptions = true, description = ["Show openings on one date."])
    class Free : Runnable {
        @ParentCommand private lateinit var parent: StudyRoomCommands
        @Option(names = ["--room"], required = true) private lateinit var room: String
        @Option(names = ["--date"], required = true, paramLabel = "YYYY-MM-DD") private lateinit var date: LocalDate

        override fun run() {
            val result = parent.connect().availability(room, date)
            parent.out.println("${result.room.name} on ${result.date}")
            if (result.slots.isEmpty()) parent.out.println("No free time.")
            result.slots.forEach { parent.out.println("${it.start.toLocalTime()} to ${it.end.toLocalTime()}") }
        }
    }

    @Command(name = "book", mixinStandardHelpOptions = true, description = ["Reserve one room."])
    class Book : Runnable {
        @ParentCommand private lateinit var parent: StudyRoomCommands
        @Option(names = ["--room"], required = true) private lateinit var room: String
        @Option(names = ["--student"], required = true) private lateinit var student: String
        @Option(names = ["--start"], required = true) private lateinit var start: LocalDateTime
        @Option(names = ["--end"], required = true) private lateinit var end: LocalDateTime

        override fun run() {
            parent.printBooking(parent.connect().book(NewBooking(room, student, start, end)))
        }
    }

    @Command(name = "bookings", mixinStandardHelpOptions = true, description = ["Show a student's booking history."])
    class Bookings : Runnable {
        @ParentCommand private lateinit var parent: StudyRoomCommands
        @Option(names = ["--student"], required = true) private lateinit var student: String

        override fun run() {
            val bookings = parent.connect().bookings(student)
            if (bookings.isEmpty()) parent.out.println("No bookings.")
            bookings.forEach(parent::printBooking)
        }
    }

    @Command(name = "cancel", mixinStandardHelpOptions = true, description = ["Cancel a booking by ID."])
    class Cancel : Runnable {
        @ParentCommand private lateinit var parent: StudyRoomCommands
        @Option(names = ["--student"], required = true) private lateinit var student: String
        @Option(names = ["--id"], required = true) private lateinit var id: UUID

        override fun run() {
            parent.printBooking(parent.connect().cancel(id, student))
        }
    }
}
