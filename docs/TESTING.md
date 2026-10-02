# Verification report

Verified on **2 October 2026**, using **OpenJDK 17.0.20** on Linux.

## Completed checks

| Check | Result |
| --- | --- |
| Compile with Java 17 compatibility | Passed |
| Treat all compiler warnings as errors | Passed |
| Build executable JAR with an entry point | Passed |
| Core behavioral tests | 37 passed, 0 failed |
| Independent generated-answer audit | 15,000 questions passed; all 20 operation templates exercised |
| Swing control workflows | Passed |
| Saved progress after GUI activity | Reloaded snapshot matches the in-memory snapshot |
| GUI screenshot rendering | Standard and 1040 × 700 minimum widths rendered |

The core suite verifies fraction equivalence, decimal rounding, rejected input, single submission, hints, points, streaks, adaptive learning, fixed difficulty, review lifecycle, balanced quizzes, interrupted quiz handling, bounded histories, save/reload, corruption backup, unsupported schemas and failed saves.

The Swing suite exercises all five lessons, linked answer labels, invalid text and zero-denominator feedback, Enter submission, disabled grading after submission, hints, navigation back to completed questions, mistake review, all ten quiz answers and the difficulty picker. It renders the application's actual components, rather than a separate mockup.

## Commands

```bash
bash scripts/test.sh
```

Equivalent Windows command:

```bat
test.bat
```

Test logs are in [test-results.txt](test-results.txt). Screenshots are in [screenshots/](screenshots/).

## Verification limits

The optional native desktop smoke test could not run in the current execution environment because its X server could not establish a listening socket. The headless Swing control and rendering tests passed. The optional test is included for a normal desktop or CI runner, but no local native-window pass is claimed.

Windows and macOS execution was not available locally. The workflow includes both platforms and Linux, but it has not run until the project is published to GitHub. Compiler compatibility and scripted UI verification are not a substitute for a platform-specific pass.

Full screen reader validation and evaluation with students or teachers have not been performed.
