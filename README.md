# Campus Study Room Booking System

This assignment prototype lets students find an available campus study room, reserve it, cancel a reservation, and view their booking history. A Kotlin command-line client communicates with a Kotlin and Spring Boot REST service. H2 stores bookings on disk. The server ships with three fictional study rooms and binds to the local computer by default.

## Build

Use JDK 17 or 21, or JDK 24 as used for local verification. The Gradle Wrapper supplies the build tool, and the source is compiled for Java 17.

```powershell
.\gradlew.bat clean build
.\gradlew.bat :cli:installDist
```

On Linux or macOS, use `./gradlew` with the same tasks. A successful build runs JUnit and Detekt. The outputs are `server/build/libs/study-room-server.jar` and `cli/build/distributions/study-room-cli.zip`.

## Run

From the repository root, start the server in one terminal.

```powershell
java -jar server/build/libs/study-room-server.jar
```

In another PowerShell terminal, use the installed CLI distribution.

```powershell
$cli = '.\cli\build\install\study-room\bin\study-room.bat'
$day = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')
& $cli rooms
& $cli free --room R101 --date $day
& $cli book --room R101 --student DEMO-1 --start "${day}T09:00" --end "${day}T10:00"
& $cli bookings --student DEMO-1
$bookingId = 'paste-booking-uuid-here'
& $cli cancel --student DEMO-1 --id $bookingId
```

Set `$bookingId` to the ID printed by the book command. On Linux or macOS, the equivalent launcher is `cli/build/install/study-room/bin/study-room`. The CLI uses `http://127.0.0.1:8080` by default and accepts `--server` to use another origin. Exit statuses are 0 for success, 1 for a connection or response failure, 2 for invalid command syntax, and 3 for a rejected server request.

Use fictional student identifiers such as `DEMO-1`. This classroom prototype identifies bookings by a supplied student ID and has no campus authentication. H2 stores the database in `data/` relative to the server's working directory.

## Architecture and verification

| Module | Responsibility |
| --- | --- |
| `contract` | Shared request, response, and booking data classes |
| `server` | REST contract, local-time validation, transactional room locking, and H2 storage |
| `cli` | Argument parsing, HTTP calls, and clear messages when the service rejects a request or is unavailable |

The server locks the room row before checking for overlaps and inserting a reservation. This keeps conflict checks and writes within one transaction. An end time equal to the next start is allowed. The tests exercise request validation, published responses, concurrent booking, cancellation, H2 storage, and CLI behavior.

Run the repeatable database probe with `.\gradlew.bat :server:benchmark`. See the [API contract](docs/api.md), [performance results](docs/performance.md), [team plan](docs/team-plan.md), [project record](docs/project-record.md), and [repository setup](docs/repository-setup.md).

## References and assistance

- Software Development Tools Continuous Assessment Brief, 2026–2027, Assignment 2.
- [Spring Boot Kotlin support](https://docs.spring.io/spring-boot/3.5/reference/features/kotlin.html) and [Kotlin Gradle configuration](https://kotlinlang.org/docs/gradle-configure-project.html)
- [H2 database documentation](https://github.com/h2database/h2database) and [Picocli user manual](https://picocli.info/)
- [Detekt Gradle plugin](https://detekt.dev/docs/1.23.8/gettingstarted/gradle/) and [GitHub CLI release documentation](https://cli.github.com/manual/gh_release_create)

Codex assisted with the initial code, tests, workflow configuration, and project documentation. Record team members' contributions and pull-request reviews as they take place.
