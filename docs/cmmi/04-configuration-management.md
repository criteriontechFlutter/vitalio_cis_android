# Configuration Management

This document records how source code and versions are managed today. The proposed rules for future work are in `CONTRIBUTING.md`, which is on the branch `docs/contributing-and-pr-template` and is not yet approved.

## Items under version control

| Item | Location |
|---|---|
| Source code | `app`, `core`, `wear` |
| Build configuration | `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` |
| App versions | `version.properties` |
| Firebase rules and functions | `firebase` |
| Documentation | `ARCHITECTURE.md`, `docs` |

Local secrets and signing details are in `local.properties`, which git ignores.

## Repository

| Item | Value |
|---|---|
| Repository | `https://github.com/criteriontechFlutter/vitalio_cis_android` |
| Default branch | `main` |

## Branches on 2026-11-05

| Branch | Tip | Purpose |
|---|---|---|
| `main` | `0556e39` | Default branch |
| `develop` | `0e9450c` | Current development |
| `10039/taskid5.1/wearos-companion` | `80f18f1` | Wear OS companion work |
| `docs/contributing-and-pr-template` | `b883769` | Contributing guide and pull request template |
| `feature/health-share` (local only) | `acc9862` | Share Health Records |

## Current practice compared with the proposed rules

| Practice | Today | Proposed |
|---|---|---|
| Task ID in commits | 0 of 136 commits | Every commit |
| Pull requests | 3 merged through GitHub pull requests (#1, #2, #3) | Every change |
| Direct commits to `develop` | Common | Not allowed |
| Branch protection | Not verified | Required on `main` and `develop` |
| Release tags | None | Every release |
| Automated builds and tests (CI) | No configuration in the repository | Not yet proposed |

## Version history

These values come from the version settings in git history. **Git does not show whether each version was released**, so the release columns are left open.

| App | Version | Version code | Commit | Commit date | Released on Play | Approved by |
|---|---|---|---|---|---|---|
| Phone | 1.0 | 1 | `93363c9` | 2026-03-19 | To be filled | To be filled |
| Phone | 2.0.24 | 31 | `24981b2` | 2026-10-13 | To be filled | To be filled |
| Phone | 2.0.25 | 32 | `7c29f81` | 2026-10-15 | To be filled | To be filled |
| Phone | 2.0.26 | 33 | `3ef8089` | 2026-10-19 | To be filled | To be filled |
| Phone | 2.0.27 | 34 | `fa022ad` | 2026-10-23 | To be filled | To be filled |
| Wear OS | 1.0.0 | 1 | `16021b9` | 2026-10-30 | To be filled | To be filled |

Versions moved from `app/build.gradle.kts` to `version.properties` in commit `16021b9`.

## Baselines

No formal baselines exist, because the repository has no tags. The commits above can be used to create baselines for past releases, once the actual released commits are confirmed from Play Console and team records.

## Release build controls

These controls are in the code:

- Release builds always use the production environment.
- A release build can only switch servers if it is built with `ENV_SWITCHER=true`.
- Signing details come from `local.properties`. If they are missing, the release build falls back to the debug key and Gradle prints a warning. See ISSUE-08 in [10-issues-and-risks.md](10-issues-and-risks.md).

## History changes

The history of `main` and `develop` was rewritten on 2026-11-04. Commit hashes recorded before that date may have changed. See CR-002 in [06-change-records.md](06-change-records.md) and the [hash map](06a-history-rewrite-hash-map.md).
