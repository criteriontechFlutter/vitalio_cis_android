# Contributing to Vitalio CIS

Every change must be traceable from its task to its release:

**task → branch → commit → pull request and review → test evidence → release**

This record is also used as process evidence, so write it while you do the work. Never backdate it or fill it in later from memory.

## Project layout

| Module | Purpose |
|---|---|
| `:app` | Phone app |
| `:wear` | Wear OS app |
| `:core` | Code shared by the phone and Wear OS apps |

Read [ARCHITECTURE.md](ARCHITECTURE.md) before adding a feature. New code follows the UI → ViewModel → UseCase → Repository flow described there.

## Setup

1. Clone the repository and open it in Android Studio.
2. Add the required values to `local.properties`, such as `NUTRI_APP_ID` and `NUTRI_APP_SECRET`. Ask a team member for them. This file is not committed.
3. Run `./gradlew assembleDebug` to confirm the project builds.

## Workflow

### 1. Start from a task

Every change needs a task ID in the task tracker, including small fixes. Record the estimate before you start and the actual effort when you finish.

For a large feature, add a short design note to the task covering screens, APIs and data flow. If you chose between technical options, record which one you chose and why.

### 2. Create a branch

Branch from `develop` and include the task ID:

```text
<task-id>/<short-description>
```

Example: `10039/diet-intake-api`

### 3. Commit

Include the task ID and describe what changed:

```text
<task-id>: <what changed>
```

| Good | Bad |
|---|---|
| `10039: Add diet intake API` | `added code` |
| `10412: Fix crash when opening an empty vitals list` | `fix` |

Keep each commit to one logical change. Do not repeat the same message for different work.

### 4. Open a pull request

- Open the pull request against `develop` and complete the template.
- At least one other developer must review and approve it. Do not approve your own pull request.
- Do not commit directly to `develop` or `main`.
- Resolve review comments with a fix or a written reason, so the discussion stays on record.
- Never force-push to `develop` or `main`. If it is unavoidable, the team must agree first and it must be recorded in a change request.

### 5. Test

Run these before requesting review:

```shell
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

- Add or update unit tests for changed logic.
- Test on a real device or emulator, and list the device and Android version in the pull request.
- Add screenshots or a recording for UI changes, including dark mode and Hindi text where the screen is affected.
- Test `:wear`, Health Connect and BLE changes on the actual watch or device model where possible. If you could not, say so in the pull request.

### 6. Report defects

Log each bug with its ID, severity, the phase where it was found, the fixing commit and the retest result. For a severe or repeated bug, also record the root cause and what will prevent it from happening again.

## Code standards

- Follow the existing architecture and the patterns already used in the module you are changing.
- Add user-facing text to string resources, in both English (`values/`) and Hindi (`values-hi/`).
- Remove debug code, unused code and temporary files before opening a pull request.
- Justify any new dependency or Android permission in the pull request.

## Security and health data

This app handles patient health data.

- Never commit keys, passwords, signing files or tokens. Use `local.properties`.
- Never log health data, personal details or auth tokens.
- Do not use real patient data in tests, screenshots or pull requests.
- Call out any change to how health data is stored, sent or shared in the pull request.

If a secret is committed, tell the team immediately and replace it. Deleting it in a later commit does not remove it from history.

## Releases

1. In a pull request to `develop`, update `version.properties`. It is the only place versions are set, and the version code must always increase:
   - Phone app: `VERSION_NAME` and `VERSION_CODE`
   - Wear OS app: `WEAR_VERSION_NAME` and `WEAR_VERSION_CODE`
2. Merge `develop` into `main` through a reviewed pull request.
3. Confirm the release build is signed with the release key, not the debug key. Gradle prints a warning if it falls back to the debug key.
4. Tag the release commit on `main` and push the tag:

   ```shell
   git tag -a v2.0.28 -m "Phone app 2.0.28 (35)"
   git tag -a wear-v1.0.1 -m "Wear OS app 1.0.1 (2)"
   git push origin v2.0.28 wear-v1.0.1
   ```

   Only create the tag for the app you are releasing.

5. Record the release notes: what was added, which bugs were fixed and any known issues. Also record the commit, the approver, the rollout date and the previous stable version to roll back to.
6. After rollout, check crash and ANR rates in Crashlytics and Play Console.
