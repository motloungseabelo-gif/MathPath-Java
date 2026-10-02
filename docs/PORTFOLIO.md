# Portfolio notes

## Short project description

**MathPath — Java Math Learning Application**

I built MathPath to help students learn from mistakes and develop confidence in mathematics. The offline desktop app combines guided lessons, generated practice, hints, worked solutions and an independent mistake review queue. It adapts question difficulty to recent performance and saves progress between sessions.

**Stack:** Java 17, Swing, Java2D, BigInteger, local file persistence and GitHub Actions.

**Engineering focus:** exact answer marking, explicit session state, input validation, responsive desktop screens, ordered background saves and automated behavioral tests. The test suite independently checks 15,000 generated questions and drives complete Swing learning flows.

## One-minute demo

1. Open the overview and explain the student problem: an incorrect answer needs a useful next step.
2. Open the Fractions lesson and walk through its worked example.
3. Start practice, use a hint, then submit an answer to show the explanation and points.
4. Reveal another solution to create a review question. Solve it independently in Mistake review.
5. Open Your progress and show topic accuracy, streaks and quiz history.
6. Close and reopen the app to demonstrate that completed answers are retained.

## Interview discussion points

- Why exact rational numbers are safer than comparing floating-point results.
- How a session state machine prevents repeated submissions from inflating scores.
- Why adaptive level changes require recent evidence at the current level.
- How atomic replacement and save-file validation protect local progress.
- Why mathematical correctness and GUI interaction need different kinds of tests.

Only describe GitHub Actions checks as passing once the actual workflow has run successfully. Do not claim production student adoption or measured educational outcomes without evidence.
