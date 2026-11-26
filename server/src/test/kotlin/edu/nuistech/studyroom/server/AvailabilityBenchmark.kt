package edu.nuistech.studyroom.server

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

private const val DAYS = 400
private const val ROWS_PER_DAY = 125
private const val RUNS = 40
private const val BATCH_SIZE = 500
private const val WARMUP_RUNS = 20

private data class Measurement(val rows: Int, val medianMs: Double, val scanned: String)

fun main() {
    val dayOne = LocalDate.of(2030, 1, 1)
    DriverManager.getConnection("jdbc:h2:mem:availability-benchmark", "sa", "").use { connection ->
        val jdbc = JdbcTemplate(SingleConnectionDataSource(connection, true))
        val schema = checkNotNull(JdbcBookingRepository::class.java.getResourceAsStream("/schema.sql"))
            .bufferedReader().use { it.readText() }
        schema.split(';').map(String::trim).filter(String::isNotBlank).forEach(jdbc::execute)
        jdbc.update("INSERT INTO rooms VALUES ('R101', 'Sample Room', 4)")
        seed(connection, dayOne)

        val target = dayOne.plusDays(DAYS.toLong() / 2)
        jdbc.execute("DROP INDEX booking_room_day")
        val baseline = measure(jdbc, target)
        jdbc.execute("CREATE INDEX booking_room_day ON bookings(room_id, status, start_at)")
        val indexed = measure(jdbc, target)
        check(baseline.rows == ROWS_PER_DAY && indexed.rows == baseline.rows)

        println("H2 room/day lookup | ${DAYS * ROWS_PER_DAY} rows | $RUNS measured runs per configuration")
        println("Configuration       Matches    Median (ms)    Rows scanned")
        printResult("Without index", baseline)
        printResult("With index", indexed)
    }
}

private fun seed(connection: Connection, dayOne: LocalDate) {
    connection.autoCommit = false
    connection.prepareStatement("INSERT INTO bookings VALUES (?, 'R101', 'BENCH', ?, ?, 'ACTIVE')").use { statement ->
        repeat(DAYS * ROWS_PER_DAY) { index ->
            val date = dayOne.plusDays((index / ROWS_PER_DAY).toLong())
            val start = date.atTime(8, 0).plusMinutes(((index % ROWS_PER_DAY) * 6).toLong())
            statement.setObject(1, UUID(0, index.toLong()))
            statement.setObject(2, start)
            statement.setObject(3, start.plusMinutes(6))
            statement.addBatch()
            if ((index + 1) % BATCH_SIZE == 0) statement.executeBatch()
        }
        statement.executeBatch()
    }
    connection.commit()
    connection.autoCommit = true
}

private fun measure(jdbc: JdbcTemplate, date: LocalDate): Measurement {
    val from = date.atStartOfDay()
    val until = date.plusDays(1).atStartOfDay()
    val query = JdbcBookingRepository.DAILY_BOOKINGS
    repeat(WARMUP_RUNS) { jdbc.queryForList(query, "R101", from, until) }
    var rows = 0
    val times = List(RUNS) {
        val start = System.nanoTime()
        rows = jdbc.queryForList(query, "R101", from, until).size
        (System.nanoTime() - start) / 1_000_000.0
    }.sorted()
    val plan = jdbc.queryForObject("EXPLAIN ANALYZE $query", String::class.java, "R101", from, until).orEmpty()
    val scanned = Regex("scanCount:\\s*(\\d+)").find(plan)?.groupValues?.get(1) ?: "not reported"
    return Measurement(rows, (times[RUNS / 2 - 1] + times[RUNS / 2]) / 2, scanned)
}

private fun printResult(label: String, result: Measurement) {
    System.out.printf(Locale.ROOT, "%-20s %7d %14.3f %15s%n", label, result.rows, result.medianMs, result.scanned)
}
