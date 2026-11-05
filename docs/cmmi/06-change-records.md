# Change Records

These records cover changes made with Claude Code on 2026-11-04 and 2026-11-05. **They were written on 2026-11-05, after the changes were made.** No change request tickets existed at the time. Add a ticket ID only if a real ticket exists.

Earlier changes have no records of this kind. See [05-change-history.md](05-change-history.md).

## CR-001: Hide the Vital Orbit icon on the Vitals screen

| Item | Detail |
|---|---|
| Date | 2026-11-04 |
| Requested by | Developer (AnimeshCriterion) |
| Task or ticket ID | To be filled |
| Change | Commented out the icon button in the Vitals screen app bar |
| File | `ui/screens/VitalDetailsView.kt` |
| Commit | `5184883`, together with other work under the message "Added Diet intake" |
| Impact | The Health Orbit screen can no longer be opened from this screen. The route and screen code remain. |
| Rollback | Uncomment the button |
| Testing | Debug build passed on 2026-11-05. The screen has not been checked on a device. |
| Approved by | To be filled |

## CR-002: Remove Claude co-author lines from git history

| Item | Detail |
|---|---|
| Date | 2026-11-04 |
| Requested by | Developer (AnimeshCriterion), who confirmed the force-push before it was run |
| Task or ticket ID | To be filled |
| Reason | GitHub showed Claude as a contributor because 7 commits had a `Co-Authored-By: Claude` line |
| Change | Removed that line from the commit messages, then force-pushed `main` and `develop` |
| Code impact | None. The final code of every rewritten branch was checked and is identical. Author, author date, committer and commit date were also unchanged on `develop`. |
| Team approval | To be filled |
| Team notification | To be filled: who was told, and whether their clones were reset |

### Branches changed

| Branch | Old tip | New tip | Pushed |
|---|---|---|---|
| `main` | `9703807` | `0556e39` | Yes |
| `develop` | `1d892e1` | `571ce17` | Yes |
| `develop`, one local commit at the time | `b7827fa` | `5184883` | Later, by the developer |
| `feature/health-share` | `d423ad7` | `acc9862` | No, local only |
| `10039/taskid5.1/wearos-companion` | `80f18f1` | `80f18f1` | Unchanged |

### Side effects

- On the remote, 34 commit hashes changed on `develop` and 21 on `main`. The [hash map](06a-history-rewrite-hash-map.md) links old and new hashes on `develop`.
- The merge commits for pull requests #2 and #3 lost their GitHub signatures. The new commits are `b1625ac` and `2e8bbd0`.
- Other developers' clones must be reset, or the old history could return with their next push.
- Old commits may remain reachable on GitHub by their old hash for some time.

### Backup

A full backup of the previous history is at `C:\Users\CT-Animesh\AndroidStudioProjects\vitalio_cis_android-pre-rewrite-20261104.bundle` on the developer's machine. **Keep this file as the record of the original history.**

### Not verified

- Whether Claude has disappeared from GitHub's contributor list
- The GitHub collaborator list and open pull requests, because the GitHub CLI is not installed on this machine

## CR-003: Turn off Claude attribution for this project

| Item | Detail |
|---|---|
| Date | 2026-11-04 |
| Requested by | Developer (AnimeshCriterion) |
| Task or ticket ID | To be filled |
| Change | Set `attribution` to empty in `.claude/settings.local.json` |
| Commit | `0e9450c`, under the message "Added Diet intake" |
| Impact | No effect on the app |
| Rollback | Remove the setting |
| Approved by | To be filled |

## CR-004: Add a contributing guide and pull request template

| Item | Detail |
|---|---|
| Date | 2026-11-05 |
| Requested by | Developer (AnimeshCriterion) |
| Task or ticket ID | To be filled |
| Change | Added `CONTRIBUTING.md` and `.github/pull_request_template.md` |
| Commit | `b883769` on `docs/contributing-and-pr-template` |
| Status | Pushed. Not merged into `develop` as of 2026-11-05. |
| Impact | No effect on the app |
| Rollback | Revert the commit |
| Review and approval | To be filled |

## CR-005: Add these CMMI documents

| Item | Detail |
|---|---|
| Date | 2026-11-05 |
| Requested by | Developer (AnimeshCriterion) |
| Task or ticket ID | To be filled |
| Change | Added the `docs/cmmi` folder, with Word copies in `docs/cmmi/word` |
| Status | Not committed as of 2026-11-05 |
| Impact | No effect on the app |
| Review and approval | To be filled |
