# Availability query performance

The probe measures the server's H2 query for active bookings in one room on one date. It seeds 50,000 bookings over 400 dates in a separate in-memory database. Both configurations use the same data and target date, with 20 warm-up queries and 40 timed queries each. `EXPLAIN ANALYZE` reports how many rows were scanned.

| Configuration | Returned rows | Median elapsed time | Rows scanned |
| --- | ---: | ---: | ---: |
| Without the room/status/start index | 125 | 0.519 ms | 50,001 |
| With the composite index | 125 | 0.123 ms | 126 |

The indexed query visited about 400 times fewer rows and returned the same records. Its median elapsed time was about 76% lower in the clean-checkout run. The schema retains the index on `(room_id, status, start_at)`, which supports the room and status filters before the date range. Absolute timings depend on the machine and cache state. The scan count also shows the reduction in work performed inside H2.

Reproduce the probe with `./gradlew :server:benchmark` or `.\gradlew.bat :server:benchmark` on Windows. The probe creates an isolated in-memory database.
