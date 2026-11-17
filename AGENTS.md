# Project Rules

## Protected Paths
- Do not modify files under `app/src/main/` unless explicitly requested.

## Checks Before Every Commit
- Run `./gradlew test` and ensure all tests pass before committing.

## Dependencies
- Add no dependency unless it is FOSS (MIT, Apache-2.0, BSD, GPL, etc.). Name the library and its license first and wait for my OK.

## Network Calls
- Do not add trackers, analytics or hard-coded hosts. Network calls only in `app/src/main/java/` or `app/src/main/res/values/strings.xml`.

## Commit Workflow
- Commit with `git add -A && git commit -m "<message>" && git push`.

## Communication Style
- Keep replies short and show each command with its expected output.
