# Gap List

These items could not be produced from the repository. Collect them from their real source. If a record never existed, record the gap and start it from now, rather than recreating it with an old date.

## Developer can complete

| Gap | Where it goes |
|---|---|
| Review and sign the documents in this folder | [README.md](README.md) |
| Purpose of each Android permission | [08-dependency-permission-register.md](08-dependency-permission-register.md) |
| Task IDs for each feature, from the task tracker | [02-requirements-traceability.md](02-requirements-traceability.md) |
| Which commit became each Play release, from Play Console | [04-configuration-management.md](04-configuration-management.md) |
| Manual test results for recent work, starting with the next task | New test records |
| Fix or assign the open issues | [10-issues-and-risks.md](10-issues-and-risks.md) |

## Needs the team lead or project manager

| Gap | Why it is needed |
|---|---|
| Approved requirements or SRS | The traceability matrix has no approved source |
| Project plan, schedule and estimates | Planning evidence |
| Effort records from the task tracker | Estimate and actual data, used for metrics |
| Approvals for the change records and history rewrite | [06-change-records.md](06-change-records.md) |
| Team notification about the history rewrite | CR-002 |
| GitHub branch protection | Enforces reviews and blocks force-pushes |
| Decision records for major technical choices | Decision analysis evidence |

## Needs QA or the process team

| Gap | Why it is needed |
|---|---|
| Test plan, test cases and UAT sign-off | Only automated test results are recorded here |
| Defect log with severity and phase found | Needed for quality metrics and root cause analysis |
| Process audit reports | Quality assurance evidence |
| Organization process documents and templates | Documents here may need to move to the required format |
| Metrics, baselines and control charts for Level 4 and 5 | These need data collected across projects |
| Security testing and privacy review | [09-security-privacy-review.md](09-security-privacy-review.md) is only a code review |

## Suggested order

1. Review these documents and confirm the "To be filled" items that you can answer.
2. Merge the contributing guide, and agree on the rules with the team.
3. Turn on branch protection, and start using task IDs and pull requests.
4. Fix ISSUE-01, then decide on ISSUE-03 and ISSUE-05 before the next release.
5. Tag the next release and record its release notes and test results.
