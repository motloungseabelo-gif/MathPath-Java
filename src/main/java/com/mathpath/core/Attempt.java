package com.mathpath.core;

import java.time.Instant;
import java.util.Objects;

public record Attempt(String questionId, Topic topic, Difficulty difficulty, boolean correct,
                      boolean hinted, boolean revealed, Instant at) {
    public Attempt {
        Objects.requireNonNull(questionId);
        Objects.requireNonNull(topic);
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(at);
        if (revealed && correct) throw new IllegalArgumentException("A revealed solution is not a correct attempt.");
    }

    public int points() { return !correct ? 0 : hinted ? difficulty.points() / 2 : difficulty.points(); }
}
