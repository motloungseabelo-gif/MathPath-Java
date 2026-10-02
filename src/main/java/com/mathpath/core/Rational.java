package com.mathpath.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/** Exact, reduced fractions keep generation and marking independent of floating-point errors. */
public record Rational(BigInteger numerator, BigInteger denominator) {
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:\\d{1,9}(?:[.,]\\d{0,6})?|[.,]\\d{1,6})");
    private static final Pattern FRACTION = Pattern.compile("[+-]?\\d{1,9}\\s*/\\s*[+-]?\\d{1,9}");

    public Rational {
        Objects.requireNonNull(numerator);
        Objects.requireNonNull(denominator);
        if (denominator.signum() == 0) throw new IllegalArgumentException("A fraction cannot have a zero denominator.");
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
    }

    public Rational(long numerator, long denominator) {
        this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public static Rational of(long value) { return new Rational(value, 1); }

    public static Rational parse(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Enter an answer first.");
        String text = raw.strip();
        if (text.length() > 64) throw new IllegalArgumentException("Use a shorter number or fraction.");
        if (FRACTION.matcher(text).matches()) {
            String[] pieces = text.split("/");
            return new Rational(new BigInteger(pieces[0].strip()), new BigInteger(pieces[1].strip()));
        }
        if (!DECIMAL.matcher(text).matches()) {
            throw new IllegalArgumentException("Use a number such as 12, 0.75 or 3/4. Leave out units.");
        }
        BigDecimal value = new BigDecimal(text.replace(',', '.'));
        return new Rational(value.unscaledValue(), BigInteger.TEN.pow(Math.max(0, value.scale())));
    }

    public Rational add(Rational other) {
        return new Rational(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Rational subtract(Rational other) { return add(other.negate()); }
    public Rational negate() { return new Rational(numerator.negate(), denominator); }
    public Rational multiply(Rational other) {
        return new Rational(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }
    public Rational divide(Rational other) {
        return multiply(new Rational(other.denominator, other.numerator));
    }
    public BigDecimal rounded(int places) {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), places, RoundingMode.HALF_UP);
    }
    public String decimal() { return rounded(2).stripTrailingZeros().toPlainString(); }

    /** Fractions must be exact. Decimal answers may also equal the expected value rounded to 2 places. */
    public boolean accepts(String input) {
        Rational submitted = parse(input);
        return equals(submitted) || (!input.contains("/") && parse(decimal()).equals(submitted));
    }

    @Override public String toString() {
        return denominator.equals(BigInteger.ONE) ? numerator.toString() : numerator + "/" + denominator;
    }
}
