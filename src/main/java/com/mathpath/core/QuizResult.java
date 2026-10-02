package com.mathpath.core;

import java.time.Instant;
import java.util.Objects;

public record QuizResult(int correct, int total, Difficulty difficulty, Instant completedAt) {
    public QuizResult {
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(completedAt);
        if (total != 10 || correct < 0 || correct > total) throw new IllegalArgumentException("Invalid quiz result.");
    }
}
