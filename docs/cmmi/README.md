# Vitalio CIS Mobile: CMMI Developer Documents

These documents record the mobile developer's work on the Vitalio CIS Android and Wear OS apps, as of 2026-11-05.

| Item | Value |
|---|---|
| Repository | `criteriontechFlutter/vitalio_cis_android` |
| Source commit | `0e9450c` on `develop` |
| Drafted by | Claude Code, from the repository and from Gradle runs on 2026-11-05 |
| Developer review | To be filled: name, date |
| Approval | To be filled: name, date |

## Status

**These are drafts until the review and approval above are filled in.** They describe what exists in the repository. They do not show that a CMMI practice has been appraised or satisfied.

Fields marked "To be filled" could not be found in the repository. Fill them only from real records, and do not backdate them.

Word copies of every document are in `docs/cmmi/word`. Document IDs such as `VCM-CMMI-01` are proposed, and should be replaced if the organization has its own numbering.

## Documents

| # | Document | What it covers | State |
|---|---|---|---|
| 01 | [Technical design](01-technical-design.md) | Modules, architecture, data flow, integrations | Ready for review |
| 02 | [Requirements traceability](02-requirements-traceability.md) | Feature to screen, code and test | Draft: needs requirement sources |
| 03 | [API inventory](03-api-inventory.md) | Environments, services, endpoints | Ready for review |
| 04 | [Configuration management](04-configuration-management.md) | Branches, versions, baselines, releases | Draft: needs release records |
| 05 | [Change history](05-change-history.md) | All 136 commits on `develop` | Generated from git |
| 06 | [Change records](06-change-records.md) | Changes made on 2026-11-04 and 2026-11-05 | Draft: needs approvals |
| 06a | [History rewrite hash map](06a-history-rewrite-hash-map.md) | Old to new commit hashes | Generated from git |
| 07 | [Test report](07-test-report.md) | Unit test, lint and build results | Generated from the 2026-11-05 run |
| 08 | [Dependency and permission register](08-dependency-permission-register.md) | Libraries, SDK levels, permissions | Draft: needs permission purposes |
| 09 | [Security and privacy review](09-security-privacy-review.md) | How the code handles health data | Draft: code-level review only |
| 10 | [Issues and risks](10-issues-and-risks.md) | Open technical issues and risks | Draft: needs owners and dates |
| 11 | [Gap list](11-gap-list.md) | What is missing and who can supply it | Action list |

The repository also has these related documents:

- [ARCHITECTURE.md](../../ARCHITECTURE.md): how to add features
- [firebase/README.md](../../firebase/README.md): feature flags
- `CONTRIBUTING.md` and `.github/pull_request_template.md`: on the branch `docs/contributing-and-pr-template`, not yet on `develop`

## Current results at a glance

| Check | Result on 2026-11-05 |
|---|---|
| Debug build | Passed |
| Unit tests | 176 of 180 passed, 4 failed |
| Lint | Failed: 49 errors in total |
| Instrumented, manual and acceptance testing | No records in the repository |
| Commits with a task ID | 0 of 136 |
| Release tags | 0 |
