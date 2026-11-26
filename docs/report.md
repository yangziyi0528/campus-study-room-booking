# Nanjing University of Information Science and Technology

**Assignment:** Team Assignment 2    **Course:** Software Development Tools  
**Verification date:** 2026-11-26    **Status:** Local verification draft  
**Team roster:** Pending team confirmation

---

# Campus Study Room Booking System

## 1. Abstract

Campus Study Room Booking System lets a student list rooms, find a free interval, place a booking, and cancel it. A Kotlin command-line program calls a Kotlin and Spring Boot REST service. H2 stores reservations across server restarts. The prototype includes a Gradle build, automated tests, a source analysis gate, and a GitHub Actions workflow for tagged releases.

A clean local checkout passed 13 tests, and the extracted CLI ZIP successfully exercised the packaged server in 10 local checks. In the performance probe, adding a composite index reduced rows scanned from 50,001 to 126 while the query returned the same 125 bookings.

## 2. Scope and environment

Each fictional room is booked as a unit from 08:00 to 22:00 in Asia/Shanghai time. Reservations start in the future and use whole-minute boundaries. The server is configured for loopback use with demonstration identifiers. The assignment prototype can be run entirely on one computer.

| Part | Selection |
| --- | --- |
| Runtime | Windows 11, Java 24.0.2 for local verification, Java 17 bytecode target |
| Build | Gradle Wrapper 8.14.3 and dependency locks for the three modules |
| Service | Kotlin, Spring Boot, JDBC and H2 |
| Client | Kotlin, Picocli, Java HTTP client and shared model classes |
| Quality checks | JUnit unit and integration tests and Detekt static analysis |

H2 was selected to exercise SQL transactions and preserve reservations on disk without provisioning a separate database server. The Gradle Wrapper and dependency locks keep the build consistent when it is run from a new checkout. GitHub Actions is configured to run the same Gradle checks before packaging a tagged release, so the release job depends on a successful build, test run, and Detekt analysis.

### 2.1 Reproduce the local run

From the repository root, run `.\gradlew.bat clean build` and start the executable JAR in `server/build/libs`. Extract the CLI ZIP from `cli/build/distributions`, then run `.\study-room.bat rooms` in PowerShell from its `bin` folder to check connectivity. The README lists the full commands and booking examples.

<div class="page-break"></div>

## 3. Service design

The `contract` module defines the JSON request and response shapes used by the server and client. The service validates inputs before writing them. A repository transaction locks the selected room row, checks for overlap, and inserts the booking while holding that lock. The condition `existing.start < request.end` and `existing.end > request.start` rejects overlapping reservations. A booking ending at 10:00 therefore leaves the next slot available at 10:00.

| REST operation | Expected response |
| --- | --- |
| `GET /api/v1/rooms` | Room IDs, names, and capacities |
| `GET /api/v1/rooms/{roomId}/availability` | Free intervals for a requested date |
| `POST /api/v1/bookings` | 201 with the stored reservation, or 409 on overlap |
| `GET /api/v1/bookings` | Booking history for the supplied student identifier |
| `DELETE /api/v1/bookings/{id}` | Cancelled booking, or 404 for an unknown or mismatched booking |

Cancellation remains in the booking history and frees the corresponding time. The CLI displays structured error codes when the server rejects a request. It reports network failures to standard error with exit status 1.

## 4. Build and test results

The clean checkout ran four domain tests, five HTTP tests, and four CLI tests. The HTTP tests include two simultaneous requests for the same slot and assert that the statuses are 201 and 409. Other tests cover adjacent bookings, validation, cancellation ownership, repeated cancellation, error handling, and a missing service. Detekt ran against all three modules and reported zero findings.

![Clean-checkout Gradle build](../output/evidence/01-clean-build.png)

*Figure 1. Clean local checkout, packaged artifacts, and automated quality checks.*

<div class="page-break"></div>

## 5. Packaged application verification

A separate run unpacked the CLI ZIP and started the executable server JAR. A created booking remained visible after the server restarted against the same database file. Cancellation reopened the interval, and the CLI reported that the service was unreachable after shutdown. The run used only fictional rooms and student identifiers.

![Packaged server and CLI checks](../output/evidence/02-packaged-workflow.png)

*Figure 2. Ten checks on the packaged client and server.*

## 6. Performance experiment

The benchmark seeded 50,000 bookings in an isolated H2 database and ran the same availability query with and without an index on room, status, and start time. Each configuration used 20 warm-up queries and 40 measured runs. Both returned 125 bookings.

![Measured H2 query benchmark](../output/evidence/03-query-benchmark.png)

*Figure 3. Median query time and H2 scan count for both configurations.*

The index reduced the median local time from 0.519 ms to 0.123 ms. H2 visited 126 rows with the index, compared with 50,001 without it. The index remains in the schema because it matches the equality filters followed by the date range. Timings will vary by machine, while the scan count records the work done for this fixture.

<div class="page-break"></div>

## 7. Development and release workflow

The build is separated into API, storage, and client delivery branches. The GitHub workflow runs a clean Gradle build on pushes and pull requests. A version tag on a commit in main's history triggers packaging and publishes a server JAR and CLI ZIP with notes generated from the change history. The repository setup instructions identify the check to require on the protected main branch.

### 7.1 Assessment evidence

| Item | Status |
| --- | --- |
| Local Gradle build, tests, and Detekt | Verified from a clean checkout |
| Packaged application and database persistence | Verified in ten local checks |
| Profiling evidence and optimization | Measured and recorded |
| GitHub board, protected main, member reviews, hosted workflow, and release | Pending team repository |
| Live commit-to-release demonstration | Pending GitHub setup and team session |

### 7.2 Planned responsibilities

| Workload | Planned responsibility |
| --- | --- |
| 50% | REST API, booking rules, quality gate, integration, and demonstration |
| 30% | Persistence, concurrency, integration tests, and performance |
| 20% | Command-line client, release packaging, and release notes |

Each team member's contribution statement will describe the work that member actually completed. This local draft records the implementation and verification evidence available before the team's GitHub setup.

## 8. References and assistance

- Software Development Tools Continuous Assessment Brief, 2026–2027, Assignment 2.
- [Spring Boot Kotlin support](https://docs.spring.io/spring-boot/3.5/reference/features/kotlin.html), [H2](https://github.com/h2database/h2database), and [Picocli](https://picocli.info/).
- [Kotlin Gradle configuration](https://kotlinlang.org/docs/gradle-configure-project.html), [Detekt](https://detekt.dev/docs/1.23.8/gettingstarted/gradle/), and [GitHub CLI releases](https://cli.github.com/manual/gh_release_create).

Codex assisted with the initial implementation, workflow, verification, and document preparation.
