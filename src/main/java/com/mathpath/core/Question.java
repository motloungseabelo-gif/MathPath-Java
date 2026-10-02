package com.mathpath.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable exercise with a reproducible set of parameters and a worked solution. */
public record Question(String id, Topic topic, Difficulty difficulty, String kind, String prompt,
                       Rational answer, String hint, List<String> steps, Map<String, Long> parameters) {
    public Question {
        Objects.requireNonNull(id);
        Objects.requireNonNull(topic);
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(prompt);
        Objects.requireNonNull(answer);
        Objects.requireNonNull(hint);
        if (id.isBlank() || prompt.isBlank() || hint.isBlank()) throw new IllegalArgumentException("Question text is required.");
        steps = List.copyOf(steps);
        parameters = Map.copyOf(parameters);
        if (steps.isEmpty()) throw new IllegalArgumentException("A worked solution is required.");
    }

    public String answerLabel() {
        return answer.denominator().equals(java.math.BigInteger.ONE)
                ? answer.toString() : answer + "  (≈ " + answer.decimal() + ")";
    }
}
