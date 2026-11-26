# Repository setup and release

Use one team repository. The local prototype was developed in the following stacked branches.

| Branch | Review focus |
| --- | --- |
| `feature/booking-api` | Shared models, endpoint contract, time rules, and unit tests |
| `feature/booking-storage` | H2 schema, room locks, HTTP tests, and benchmark |
| `feature/client-delivery` | CLI, Gradle quality gate, and tag-triggered release |

To publish the prototype, push the baseline `main` and the complete `feature/client-delivery` branch. It contains the workflow and all three increments. Open an integration pull request to `main`. Once GitHub has run the workflow and exposes the check, protect `main` by requiring a pull request, at least one approving review, and the **Gradle quality gate** before merging. Team members then use their own branches and PRs for follow-up work. Configure the project board with the team's tasks, actual owners, and progress. Use the project record for decisions, evidence links, and weekly updates.

Every push and pull request runs a clean Gradle build. A tag in the form `vMAJOR.MINOR.PATCH` starts the same checks and packages the server JAR and CLI ZIP. The release job checks that the tagged commit belongs to `main`'s history and publishes the files with generated notes. Add the tag after the target commit passes review.

For the course demonstration, start with a reviewed change, such as an update to the fictional room descriptions. Show the commit, the passing checks, and the GitHub Release for the tagged version. Run the packaged server and client, then demonstrate a successful booking, an overlapping request, and a cancellation. Record the actual commit, workflow run, release, and test evidence in the final report.
