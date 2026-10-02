package com.mathpath;

import java.util.Objects;

/** Tiny dependency-free assertion helpers. Failures throw and make the test process exit non-zero. */
final class Checks {
    private Checks() { }
    static void that(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + " but received " + actual);
    }
    static <T extends Throwable> void throwsType(Class<T> type, Throwing action) {
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + " but received " + error, error);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " to be thrown.");
    }
    @FunctionalInterface interface Throwing { void run() throws Exception; }
}
