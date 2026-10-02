# Design notes

## Learning flow

`LearningService` owns the current session. An exercise transitions from open to answered, then to the next exercise or a completed session. Invalid input leaves it open. Hints flag an open practice/review exercise; quiz exercises reject hints. Marking an answered exercise again is rejected before progress can change.

Practice generates a new exercise after each answer. A quiz generates two exercises from each topic, shuffles them, and records its score after the tenth marked answer. Review takes a snapshot of the queue and visits each question once. Failed and hinted review attempts remain available for another session.

The adaptive algorithm considers up to six consecutive attempts in a topic at the most recent level. At least five independent successes move up one level. At most two successes move down one level. Intermediate performance keeps the current level. After a level change, the learner needs six attempts at that new level before another change.

## Mathematical modelling

`Rational` normalizes signs and reduces fractions with the greatest common divisor. Generated arithmetic and fraction operations are exact. `Question` keeps the topic, level, operation kind, parameters, hint and worked steps together as immutable data.

The UI asks for a bare numeric answer. Integers, decimals with a point or comma, and simple fractions are supported. Mixed numbers, expressions, scientific notation and units are not supported. Decimal marking also accepts the reference result rounded to two places; fractional marking requires exact equivalence. Circle questions use 3.14 rather than an unspecified value of π.

## Persistence

The repository writes a versioned `Properties` file through UTF-8 readers and writers. Each save first writes a temporary file in the same directory and then replaces the destination with an atomic move. A replace move is used when the filesystem does not support atomic replacement.

The loader validates schema, counts, enum names, booleans, timestamps, fractions and overall/topic totals. Input size is limited to 4 MiB. On corrupted data, it moves the original to a uniquely named backup and creates a fresh in-memory profile. If that backup fails, the file cannot be read, or its schema is unsupported, saving is disabled for that repository instance.

Lifetime totals remain independent of bounded recent history. Saves use one background executor so an older snapshot cannot finish after a newer one. Window closing queues disposal after saves. A save failure is visible in the footer; learning can continue in memory. There is no inter-process locking, so one application instance should use a given data folder at a time.

## Interface

The application uses standard Swing controls with a small shared color and component system. The main viewport tracks the available width and scrolls vertically. The minimum window size is 1040 × 700. Artwork is drawn with Java2D and stays available offline.

Answer fields have a linked label, an accessible name and description, and Enter submission. Buttons retain keyboard focus and have visible focus outlines. Text and icons identify states alongside color. Full screen reader certification has not been performed.

## Tradeoffs and next steps

- Single local learner keeps setup simple; multiple profiles would need explicit data ownership and switching.
- Short original lessons cover fundamentals; a production education product would need teacher review, curriculum mapping and learner evaluation.
- Procedural exercises offer variety within 20 operation templates. They do not replace a full question bank.
- Adding graphs, equations with fractional coefficients, configurable study goals or a packaged native installer would extend the project without changing the core session model.
