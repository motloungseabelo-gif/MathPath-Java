package com.mathpath.core;

public enum Difficulty {
    STARTER("Starter", 10), BUILDER("Builder", 15), CHALLENGE("Challenge", 20);

    private final String label;
    private final int points;

    Difficulty(String label, int points) {
        this.label = label;
        this.points = points;
    }

    public int points() { return points; }
    @Override public String toString() { return label; }
}
