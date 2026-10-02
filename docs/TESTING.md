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

Local test logs are in [test-results.txt](test-results.txt). Screenshots are in [screenshots/](screenshots/).

## GitHub Actions verification

[Verify MathPath run 1](https://github.com/motloungseabelo-gif/MathPath-Java/actions/runs/37061447745) completed successfully for published application commit `9335563a7b2f0237254da7b7d3d2401bfa259af5`. Its source tree exactly matches the locally tested source tree.

| CI job | Result |
| --- | --- |
| Java 17 / Ubuntu | Passed: build, core suite, Swing flows, rendering and native desktop input |
| Java 17 / Windows | Passed: Windows build script, core suite, Swing flows and rendering |
| Java 17 / macOS | Passed: build, core suite, Swing flows and rendering |
| Runnable JAR artifact | Uploaded successfully by all three jobs |

The native Linux test used a real Swing window under Xvfb and verified mouse navigation, typed input, Enter submission, saved progress and window resizing.

## Verification limits

The optional native desktop smoke test could not run in the local execution environment because its X server could not establish a listening socket. The headless Swing control and rendering tests passed locally. The native test subsequently passed on the GitHub Actions Linux runner.

Windows and macOS execution was not available locally; their GitHub Actions jobs passed. Native mouse and keyboard testing was performed on Linux, not on Windows or macOS.

Full screen reader validation and evaluation with students or teachers have not been performed.
