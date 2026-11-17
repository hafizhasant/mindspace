# Agent instructions for Mindspace

## Environment limits
- Java is NOT installed on this machine and cannot be installed by the agent.
- `sudo` requires a password the agent cannot enter. Do not run `sudo` commands.
- Do not retry `java -version`, `apt install`, or any local Android build.

## Build
- Builds run on GitHub Actions, not locally.
- Workflow: `.github/workflows/build.yml` (triggers on push).
- To build: commit and `git push origin HEAD`, then check the Actions tab.
- The APK is in the run's Artifacts section (`debug-apk`).

## Current task
1. Commit and push the current changes.
2. Check the latest GitHub Actions run for compile errors.
3. Fix any errors found, commit, push, and repeat until the build passes.
4. Follow TESTING_GUIDE_FINAL.md once an APK is available.

## Completed
- ThemeUtils class with save/load scheme methods
- Theme colors (forest, midnight, nordic) and styles
- Settings dialog theme spinner
- MainActivity applies theme before setContentView
- Testing guide, test script, and verification summary

## Blocked
- Local Java install (no sudo access for agent). Use GitHub Actions instead.
