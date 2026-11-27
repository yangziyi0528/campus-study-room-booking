# Local technical review

The local review followed the booking from the CLI through the REST API and into the database. It also checked the clean Gradle build, the release files, and the workflow definition.

| Review area | Result |
| --- | --- |
| Input and interval rules | Unit tests cover opening limits, invalid dates and time ranges, ID normalization, and adjacent slots. |
| Concurrent writes | Two concurrent HTTP requests for an overlapping slot returned one 201 and one 409. The check and insert share the room lock and transaction. |
| Persistence | The released-format JAR restored a previously created booking after a process restart using the same H2 database file. |
| CLI and errors | The extracted CLI ZIP handled a valid booking, a conflict, cancellation, missing service, and exit status propagation. |
| Toolchain | The checked-in locks supported a clean build with 13 passing tests and zero Detekt findings. The workflow YAML and all embedded Bash commands passed local syntax checks. |
| Performance | The 50,000-row probe returned the same 125 rows in both configurations and reduced the scan count from 50,001 to 126 with the added index. |

Detekt's JVM target is set to 17 to match the application target when Gradle runs on a newer JDK. The Spring Boot and Picocli process entry points each suppress the spread-operator warning because the command argument array is forwarded once at startup.

The project uses fictional identifiers on loopback. The supplied student ID is suitable for the demonstration model. Production deployment would require campus authentication and authorization.

GitHub-hosted workflow execution, peer PR approval, project-board activity, and the tagged public release are recorded after they occur in the team repository.
