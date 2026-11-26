# Team responsibilities

| Planned workload | Responsibility | Evidence to collect |
| --- | --- | --- |
| 50% | Core booking REST service, API contract, integration, and merge quality gate | Controller and service changes, contract tests, branch rules, and the end-to-end demonstration |
| 30% | Persistence, concurrent booking tests, and performance analysis | Database schema, transactional repository, database tests, and measured optimization results |
| 20% | CLI, release packaging, and generated release notes | CLI changes and tests, tag workflow, and released artifacts |

Use one GitHub repository. Each member uses their own account and reviews another member's pull request. The project board tracks task, owner, and status. Update the project record weekly with decisions, actual progress, and links to evidence. Each member writes their contribution statement from the work they performed.

## Suggested review handoff

- **50%**. Review `BookingRules.kt`, `BookingController.kt`, and the API documentation. Check the HTTP contract and coordinate the end-to-end run. Own the build check and branch protection in the team repository.
- **30%**. Review `JdbcBookingRepository.kt`, the SQL schema, and the concurrency test. Reproduce the performance probe and explain the measured result.
- **20%**. Review the `cli` module, the packaged command examples, and the tag-driven release job. Verify the release assets and generated notes on GitHub.
