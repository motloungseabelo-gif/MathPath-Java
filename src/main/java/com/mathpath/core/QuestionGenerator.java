package com.mathpath.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/** Seedable question factory. Every prompt, hint and solution derives from the same parameters. */
public final class QuestionGenerator {
    private final Random random;

    public QuestionGenerator() { this(new Random()); }
    public QuestionGenerator(Random random) { this.random = Objects.requireNonNull(random); }

    public Question generate(Topic topic, Difficulty difficulty) {
        Objects.requireNonNull(topic);
        Objects.requireNonNull(difficulty);
        return switch (topic) {
            case ARITHMETIC -> arithmetic(difficulty);
            case FRACTIONS -> fractions(difficulty);
            case ALGEBRA -> algebra(difficulty);
            case GEOMETRY -> geometry(difficulty);
            case PERCENTAGES -> percentages(difficulty);
        };
    }

    private int between(int min, int max) { return min + random.nextInt(max - min + 1); }
    private int limit(Difficulty d) { return switch (d) { case STARTER -> 12; case BUILDER -> 30; case CHALLENGE -> 80; }; }

    private Question arithmetic(Difficulty d) {
        int a = between(2, limit(d)), b = between(2, limit(d));
        int type = random.nextInt(d == Difficulty.CHALLENGE ? 5 : 4);
        return switch (type) {
            case 0 -> q(Topic.ARITHMETIC, d, "add", a + " + " + b + " = ?", Rational.of(a + b),
                    "Add the tens, then the ones. Regroup when needed.",
                    List.of("Start at " + a + ".", "Add " + b + " to reach " + (a + b) + "."), "a", a, "b", b);
            case 1 -> {
                int high = d == Difficulty.CHALLENGE ? a : Math.max(a, b);
                int low = d == Difficulty.CHALLENGE ? b : Math.min(a, b);
                yield q(Topic.ARITHMETIC, d, "subtract", high + " − " + low + " = ?", Rational.of(high - low),
                        "Move left on a number line when subtracting. You may pass zero.",
                        List.of("Start at " + high + ".", "Move " + low + " places left: " + (high - low) + "."), "a", high, "b", low);
            }
            case 2 -> q(Topic.ARITHMETIC, d, "multiply", a + " × " + b + " = ?", Rational.of((long) a * b),
                    "Multiplication is equal groups. Split a number into easier parts.",
                    List.of("There are " + a + " groups of " + b + ".", a + " × " + b + " = " + ((long) a * b) + "."), "a", a, "b", b);
            case 3 -> q(Topic.ARITHMETIC, d, "divide", (a * b) + " ÷ " + b + " = ?", Rational.of(a),
                    "Think: what number multiplied by " + b + " gives " + (a * b) + "?",
                    List.of(b + " × " + a + " = " + (a * b) + ".", "So " + (a * b) + " ÷ " + b + " = " + a + "."), "a", a * b, "b", b);
            default -> {
                int c = between(2, 12);
                yield q(Topic.ARITHMETIC, d, "order", a + " + " + b + " × " + c + " = ?", Rational.of(a + (long) b * c),
                        "Do multiplication before addition.", List.of(b + " × " + c + " = " + (b * c) + ".",
                                a + " + " + (b * c) + " = " + (a + b * c) + "."), "a", a, "b", b, "c", c);
            }
        };
    }

    private Question fractions(Difficulty d) {
        int b = between(2, d == Difficulty.STARTER ? 8 : 12);
        int e = d == Difficulty.STARTER ? b : between(2, 12);
        int a = between(1, b - 1), c = between(1, e - 1);
        Rational left = new Rational(a, b), right = new Rational(c, e);
        int type = random.nextInt(d == Difficulty.STARTER ? 2 : 4);
        String symbol = switch (type) { case 0 -> " + "; case 1 -> " − "; case 2 -> " × "; default -> " ÷ "; };
        if (type == 1 && (long) a * e < (long) c * b) {
            int t = a; a = c; c = t; t = b; b = e; e = t;
            left = new Rational(a, b); right = new Rational(c, e);
        }
        Rational result = switch (type) {
            case 0 -> left.add(right); case 1 -> left.subtract(right);
            case 2 -> left.multiply(right); default -> left.divide(right);
        };
        String hint = type < 2 ? "Use a common denominator before combining the numerators."
                : type == 2 ? "Multiply the numerators and the denominators, then simplify."
                : "Flip the second fraction, then multiply.";
        String first = type < 2 ? "A common denominator is " + (b * e) + ". Rewrite as " + (a * e) + "/" + (b * e)
                + symbol + (c * b) + "/" + (b * e) + "."
                : type == 2 ? "Multiply: (" + a + " × " + c + ") / (" + b + " × " + e + ")."
                : "Rewrite as " + a + "/" + b + " × " + e + "/" + c + ".";
        return q(Topic.FRACTIONS, d, new String[]{"fractionAdd", "fractionSubtract", "fractionMultiply", "fractionDivide"}[type],
                a + "/" + b + symbol + c + "/" + e + " = ?", result, hint,
                List.of(first, "Calculate and reduce the fraction: " + result + ".", "As a decimal, rounded to 2 places: " + result.decimal() + "."),
                "a", a, "b", b, "c", c, "d", e);
    }

    private Question algebra(Difficulty d) {
        int x = between(d == Difficulty.CHALLENGE ? -12 : 1, limit(d));
        int b = between(1, 15), a = d == Difficulty.STARTER ? 1 : between(2, 9);
        boolean bracket = d == Difficulty.CHALLENGE && random.nextBoolean();
        int c = bracket ? a * (x + b) : a * x + b;
        String lhs = bracket ? a + "(x + " + b + ")" : (a == 1 ? "x" : a + "x") + " + " + b;
        List<String> steps = bracket
                ? List.of("Divide both sides by " + a + ": x + " + b + " = " + (c / a) + ".",
                        "Subtract " + b + " from both sides: x = " + x + ".", "Check: " + a + "(" + x + " + " + b + ") = " + c + ".")
                : List.of("Subtract " + b + " from both sides: " + (a == 1 ? "x" : a + "x") + " = " + (c - b) + ".",
                        "Divide by " + a + ": x = " + x + ".", "Check: " + a + " × " + x + " + " + b + " = " + c + ".");
        return q(Topic.ALGEBRA, d, bracket ? "bracket" : "linear", "Solve for x:  " + lhs + " = " + c,
                Rational.of(x), bracket ? "First undo the multiplication outside the brackets." : "Undo addition, then undo multiplication. Do the same to both sides.",
                steps, "a", a, "b", b, "c", c);
    }

    private Question geometry(Difficulty d) {
        int a = between(2, limit(d)), b = between(2, limit(d)), c = between(2, 12);
        int type = random.nextInt(d == Difficulty.STARTER ? 2 : d == Difficulty.BUILDER ? 3 : 5);
        return switch (type) {
            case 0 -> q(Topic.GEOMETRY, d, "rectangle", "A rectangle is " + a + " cm long and " + b + " cm wide. Find its area (cm²).",
                    Rational.of((long) a * b), "Area of a rectangle = length × width.",
                    List.of("A = length × width.", "A = " + a + " × " + b + " = " + (a * b) + " cm²."), "a", a, "b", b);
            case 1 -> q(Topic.GEOMETRY, d, "perimeter", "A rectangle is " + a + " cm long and " + b + " cm wide. Find its perimeter (cm).",
                    Rational.of(2L * (a + b)), "Perimeter is the total distance around the shape.",
                    List.of("P = 2 × (length + width).", "P = 2 × (" + a + " + " + b + ") = " + (2 * (a + b)) + " cm."), "a", a, "b", b);
            case 2 -> q(Topic.GEOMETRY, d, "triangle", "A triangle has base " + a + " cm and perpendicular height " + b + " cm. Find its area (cm²).",
                    new Rational((long) a * b, 2), "A triangle is half of a rectangle with the same base and height.",
                    List.of("A = ½ × base × perpendicular height.", "A = ½ × " + a + " × " + b + " = " + new Rational((long) a * b, 2).decimal() + " cm²."), "a", a, "b", b);
            case 3 -> q(Topic.GEOMETRY, d, "cuboid", "A box measures " + a + " × " + b + " × " + c + " cm. Find its volume (cm³).",
                    Rational.of((long) a * b * c), "Volume = length × width × height.",
                    List.of("V = length × width × height.", "V = " + a + " × " + b + " × " + c + " = " + (a * b * c) + " cm³."), "a", a, "b", b, "c", c);
            default -> q(Topic.GEOMETRY, d, "circle", "A circle has radius " + c + " cm. Find its area (cm²). Use π = 3.14.",
                    new Rational(314L * c * c, 100), "Area = π × radius². The radius is half the diameter.",
                    List.of("A = π × r².", "r² = " + c + " × " + c + " = " + (c * c) + ".",
                            "A = 3.14 × " + (c * c) + " = " + new Rational(314L * c * c, 100).decimal() + " cm²."), "r", c);
        };
    }

    private Question percentages(Difficulty d) {
        int[] rates = d == Difficulty.STARTER ? new int[]{10, 20, 25, 50, 75} : new int[]{5, 12, 15, 20, 25, 30, 35, 45, 60};
        int p = rates[random.nextInt(rates.length)], amount = between(2, d == Difficulty.STARTER ? 12 : 50) * 20;
        Rational part = new Rational((long) p * amount, 100);
        int type = random.nextInt(d == Difficulty.STARTER ? 1 : d == Difficulty.BUILDER ? 3 : 4);
        Rational result = switch (type) { case 0 -> part; case 1 -> Rational.of(amount).subtract(part);
            case 2 -> Rational.of(amount).add(part); default -> Rational.of(amount); };
        String prompt = switch (type) {
            case 0 -> "What is " + p + "% of " + amount + "?";
            case 1 -> "A school bag costs R" + amount + ". It is " + p + "% off. What is the sale price (R)?";
            case 2 -> "A club has " + amount + " members. Membership rises by " + p + "%. How many members are there now?";
            default -> p + "% of a number is " + part.decimal() + ". What is the original number?";
        };
        // Switch to a price example when the computed membership would not be a whole number.
        if (type == 2 && !result.denominator().equals(java.math.BigInteger.ONE)) {
            prompt = "A textbook costs R" + amount + ". The price rises by " + p + "%. What is its new price (R)?";
        }
        String first = p + "% = " + p + "/100.";
        String second = type == 3 ? "Original = " + part.decimal() + " ÷ (" + p + "/100) = " + amount + "."
                : "Find the percentage: " + p + "/100 × " + amount + " = " + part.decimal() + ".";
        String last = switch (type) {
            case 0 -> "The answer is " + result.decimal() + ".";
            case 1 -> "Subtract the discount: " + amount + " − " + part.decimal() + " = " + result.decimal() + ".";
            case 2 -> "Add the increase: " + amount + " + " + part.decimal() + " = " + result.decimal() + ".";
            default -> "Check: " + p + "/100 × " + amount + " = " + part.decimal() + ".";
        };
        return q(Topic.PERCENTAGES, d, new String[]{"percent", "discount", "increase", "reversePercent"}[type], prompt, result,
                type == 3 ? "Divide the known part by the percentage written as a decimal." : "Divide the percentage by 100, then multiply by the amount.",
                List.of(first, second, last), "p", p, "amount", amount);
    }

    private Question q(Topic topic, Difficulty d, String kind, String prompt, Rational answer, String hint,
                       List<String> steps, Object... pairs) {
        Map<String, Long> parameters = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) parameters.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
        return new Question(UUID.randomUUID().toString(), topic, d, kind, prompt, answer, hint, steps, parameters);
    }
}
