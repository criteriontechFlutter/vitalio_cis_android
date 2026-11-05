# Test Report

Generated on 2026-11-05 from `develop` at commit `0e9450c`.

These results come from running the commands below. They cover automated unit tests, lint and the debug build only. Manual, device and user acceptance testing are not recorded here.

## Test run details

| Item | Value |
|---|---|
| Run date | 2026-11-05 |
| Commit tested | `0e9450c` on `develop` |
| Run by | Claude Code, at the developer's request |
| JDK | JetBrains Runtime 21.0.11 |
| Unit test command | `./gradlew testDebugUnitTest --continue` |
| Lint and build command | `./gradlew lintDebug assembleDebug --continue` |

## Result summary

| Check | Result |
|---|---|
| Unit tests | **176 of 180 passed, 4 failed**, 0 skipped |
| Debug build (`:app`, `:wear`, `:core`) | Passed |
| Lint | **Failed**: `:app` and `:wear` have errors |
| Instrumented tests | Not run: these need a device or emulator |

## Unit test results by class

| Module | Test class | Tests | Passed | Failed | Skipped |
|---|---|---:|---:|---:|---:|
| `:app` | `ExampleUnitTest` | 1 | 1 | 0 | 0 |
| `:app` | `OmronDeviceFilterTest` | 8 | 8 | 0 | 0 |
| `:app` | `OmronParseTest` | 6 | 2 | 4 | 0 |
| `:app` | `RoutesTest` | 19 | 19 | 0 | 0 |
| `:app` | `YonkerFramesTest` | 11 | 11 | 0 | 0 |
| `:app` | `DomainResultTest` | 14 | 14 | 0 | 0 |
| `:app` | `UseCaseTest` | 13 | 13 | 0 | 0 |
| `:app` | `AppointmentAndMedicineModelTest` | 9 | 9 | 0 | 0 |
| `:app` | `ModelTest` | 16 | 16 | 0 | 0 |
| `:app` | `PrefsManagerLogicTest` | 15 | 15 | 0 | 0 |
| `:app` | `LoginViewModelTest` | 16 | 16 | 0 | 0 |
| `:app` | `ViewModelStateLogicTest` | 19 | 19 | 0 | 0 |
| `:core` | `TranscriptStitcherTest` | 7 | 7 | 0 | 0 |
| `:core` | `VoiceNlpParserTest` | 26 | 26 | 0 | 0 |

## Failed tests

| Module | Test class | Test | Error |
|---|---|---|---|
| `:app` | `OmronParseTest` | invalid marker value is treated as missing | java.lang.RuntimeException: Method d in android.util.Log not mocked. |
| `:app` | `OmronParseTest` | millisecond timestamps are accepted too | java.lang.RuntimeException: Method d in android.util.Log not mocked. |
| `:app` | `OmronParseTest` | blood pressure readings are parsed with their measured time | java.lang.RuntimeException: Method d in android.util.Log not mocked. |
| `:app` | `OmronParseTest` | unset device clock falls back to transfer time | java.lang.RuntimeException: Method d in android.util.Log not mocked. |

**Analysis:** all four failures have the same cause. `OmronManager.parse` calls `android.util.Log.d` (`OmronManager.kt:491`), which is not available in local unit tests, so the tests stop before reaching their assertions. This points to a test setup problem, but the parsing behavior these four tests cover is unverified until it is fixed. It is logged as ISSUE-01 in [10-issues-and-risks.md](10-issues-and-risks.md).

## Lint results

| Module | Errors | Warnings | Hints |
|---|---:|---:|---:|
| `:app` | 46 | 277 | 16 |
| `:core` | 0 | 8 | 0 |
| `:wear` | 3 | 9 | 0 |

### Lint errors by type

| Module | Lint check | Errors |
|---|---|---:|
| `:app` | `LocalContextGetResourceValueCall` | 36 |
| `:app` | `NonObservableLocale` | 7 |
| `:app` | `SuspiciousIndentation` | 1 |
| `:app` | `RestrictedApi` | 1 |
| `:app` | `ImpliedQuantity` | 1 |
| `:wear` | `LocalContextGetResourceValueCall` | 3 |

## Instrumented tests not run

| Test file |
|---|
| `app/src/androidTest/java/com/critetiontech/vitalio_cis/ExampleInstrumentedTest.kt` |
| `app/src/androidTest/java/com/critetiontech/vitalio_cis/HealthConnectSyncTest.kt` |
