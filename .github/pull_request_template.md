## Task

Task ID and link:

## What changed and why

<!-- Summarize the change and the reason for it. For a bug fix, include the bug ID and the root cause. -->

## Type of change

- [ ] Feature
- [ ] Bug fix
- [ ] Refactor
- [ ] Release or version update
- [ ] Other:

## Modules affected

- [ ] `:app`
- [ ] `:wear`
- [ ] `:core`

## Testing

**Unit tests added or updated:**

**Manual test steps and results:**

**Tested on:**

| Device or emulator | Android or Wear OS version | Result |
|---|---|---|
|  |  |  |

**Not tested, and why:**

## Screenshots or recording

<!-- Required for UI changes. Do not include real patient data. -->

## Risk and rollback

**What could break:**

**How to roll back:**

## Author checklist

- [ ] The branch name and commits include the task ID
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintDebug` shows no new issues
- [ ] `./gradlew assembleDebug` succeeds
- [ ] User-facing text is in both `values/` and `values-hi/`
- [ ] No secrets, real patient data or debug code are included
- [ ] Logs do not contain health data, personal details or tokens
- [ ] Any new dependency or permission is explained above
- [ ] Any change to how health data is stored, sent or shared is explained above
- [ ] Related docs are updated
