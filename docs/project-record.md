# Project record

## 2026-11-26

- Confirmed the Campus Study Room Booking System and a planned workload split of 50:30:20.
- Selected Kotlin with Spring Boot to follow the assignment and the instructor's Spring Boot requirement.
- Divided the code into a shared API contract, a REST server, and a command-line client. The local prototype will work with fictional study rooms and booking IDs.
- Started the Gradle Wrapper build. One team repository will record actual member contributions, reviews, workflow runs, and releases once the group creates it.
- Codex assisted with the initial local implementation and project documentation.
- Implemented and verified the API, transactional H2 storage, and CLI. Added local endpoint and concurrency tests alongside command-line tests.
- Chose the room/status/start index after the 50,000-row probe returned the same 125 bookings while reducing the scan count from 50,001 to 126.
- Added a workflow for pull-request checks and tag-driven releases. GitHub-hosted checks, the project board, member reviews, and the formal release await the team repository.
- Built a clean clone with the checked-in dependency locks. All 13 JUnit tests passed, and Detekt reported no findings. A separate run from the packaged JAR and CLI ZIP passed 10 checks, including persistence across a server restart.

## 2026-11-27

- Pushed the baseline and the complete prototype to the team repository. Opened [integration PR 1](https://github.com/yangziyi0528/campus-study-room-booking/pull/1) and created the [team project](https://github.com/users/yangziyi0528/projects/1) with review tasks.
- The first hosted build failed with HTTP 429 during Maven Central plugin dependency download. Configured GitHub Actions to try the [Google-hosted Maven Central mirror](https://docs.cloud.google.com/docs/buildpacks/java#google_cloud_hosted_maven_central_mirror) first. Existing dependency locks and the local repository configuration still apply.
