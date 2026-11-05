# Issues and Risks

Opened on 2026-11-05 from the code review and Gradle runs. Priority is an initial assessment. None of these items has been fixed.

The project's bug history is not in the repository, so this is not a complete defect log. Add bugs from the team's tracker.

## Open issues

| ID | Issue | Evidence | Priority | Owner | Target date | Status |
|---|---|---|---|---|---|---|
| ISSUE-01 | 4 `OmronParseTest` tests fail because `android.util.Log` is not available in local unit tests | [07-test-report.md](07-test-report.md) | Medium | To be filled | To be filled | Open |
| ISSUE-02 | Lint reports 46 errors in `:app` and 3 in `:wear`, so `lintDebug` fails | [07-test-report.md](07-test-report.md) | Medium | To be filled | To be filled | Open |
| ISSUE-03 | Lab reports, prescriptions and other data may travel over plain HTTP (SEC-1) | `ApiConfig.kt`, `network_security_config.xml` | High | To be filled | To be filled | Open |
| ISSUE-04 | Patient data is stored on the device without app-level encryption (SEC-2) | `PrefsManager.kt:109` | Medium | To be filled | To be filled | Open |
| ISSUE-05 | The video consultation address is a local test server, for all build types | `ApiConfig.CONSULTATION` | High if released | To be filled | To be filled | Open |
| ISSUE-06 | An admin's phone number is hard-coded in code, rules and docs (SEC-5) | `FeatureFlags.kt`, `firestore.rules`, `firebase/README.md` | Medium | To be filled | To be filled | Open |
| ISSUE-07 | The NutriAnalyser secret can be extracted from the APK (SEC-4) | `app/build.gradle.kts` | Medium | To be filled | To be filled | Open |
| ISSUE-08 | A release build can be signed with the debug key if signing details are missing (SEC-6) | `app/build.gradle.kts` | Medium | To be filled | To be filled | Open |
| ISSUE-09 | Temporary files are tracked in git: `diff.patch`, `old_manager.kt`, `temp_code.txt`, `check_depth.py`, `sheet_depth.txt` | `git ls-files` | Low | To be filled | To be filled | Open |
| ISSUE-10 | `ARCHITECTURE.md` is out of date: it lists a different local server address, and its Hilt note names AGP 8.11.0 while the project uses 9.3.3 | `ARCHITECTURE.md`, `ApiConfig.kt`, `libs.versions.toml` | Low | To be filled | To be filled | Open |
| ISSUE-11 | `Log.e` and `Log.w` calls remain in release builds and have not been reviewed for sensitive data (SEC-3) | `app/proguard-rules.pro` | Medium | To be filled | To be filled | Open |
| ISSUE-12 | `JAVA_HOME` on the developer's machine points to an incomplete JDK folder (`C:\Program Files\Android\Android Studio\jbr`), so Gradle fails from the command line. The 2026-11-05 runs used `~/.jdks/jbr-21.0.11`. | Local machine | Low | To be filled | To be filled | Open |

## Process risks

| ID | Risk | Effect | Suggested action | Owner | Status |
|---|---|---|---|---|---|
| RISK-01 | No commit has a task ID, and many messages repeat | Work cannot be traced from requirement to code | Adopt the commit rules in `CONTRIBUTING.md` | To be filled | Open |
| RISK-02 | No release tags exist | Released versions cannot be identified or rebuilt reliably | Tag every release, and backfill past ones from confirmed records | To be filled | Open |
| RISK-03 | No CI configuration exists | Failing tests and lint errors can reach `develop` unnoticed | Run tests and lint on every pull request | To be filled | Open |
| RISK-04 | Many features have no automated tests, including diet, vitals, fluid and consultation | Regressions may reach users | Agree on a test plan, starting with health data flows | To be filled | Open |
| RISK-05 | Most changes reach `develop` without a pull request or recorded review | No evidence of peer review | Require pull requests through branch protection | To be filled | Open |
| RISK-06 | The history rewrite may be undone by an old clone | Removed lines and old hashes could return | Confirm that all team clones are reset | To be filled | Open |
