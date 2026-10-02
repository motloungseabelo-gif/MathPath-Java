package com.mathpath.core;

/** The five independently tracked learning areas. */
public enum Topic {
    ARITHMETIC("Arithmetic", "Build your number sense", "+", "01"),
    FRACTIONS("Fractions", "Make parts of a whole click", "½", "02"),
    ALGEBRA("Algebra", "Find the missing piece", "x", "03"),
    GEOMETRY("Geometry", "Think in shapes and space", "△", "04"),
    PERCENTAGES("Percentages", "Bring math into everyday life", "%", "05");

    private final String title;
    private final String description;
    private final String symbol;
    private final String number;

    Topic(String title, String description, String symbol, String number) {
        this.title = title;
        this.description = description;
        this.symbol = symbol;
        this.number = number;
    }

    public String title() { return title; }
    public String description() { return description; }
    public String symbol() { return symbol; }
    public String number() { return number; }
}
