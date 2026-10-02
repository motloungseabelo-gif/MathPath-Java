package com.mathpath.core;

import java.util.List;

/** Short original lessons with explicit examples learners can try before practice. */
public final class Lessons {
    private Lessons() { }
    public record Lesson(String rule, String explanation, String example, List<String> steps, String tip) { }

    public static Lesson forTopic(Topic topic) {
        return switch (topic) {
            case ARITHMETIC -> new Lesson("One operation at a time.",
                    "Addition combines quantities. Subtraction finds a difference. Multiplication makes equal groups, and division shares them. In a mixed expression, do brackets first, multiplication and division next, and addition and subtraction last. Work left to right within each pair.",
                    "8 + 3 × 4 = ?", List.of("Multiply first: 3 × 4 = 12.", "Then add: 8 + 12 = 20.", "The answer is 20."),
                    "A negative answer is allowed when you subtract a larger number from a smaller one.");
            case FRACTIONS -> new Lesson("The denominator tells you the size of each piece.",
                    "A fraction names part of a whole. To add or subtract, make the denominators equal first. To multiply, multiply the tops and the bottoms. To divide, turn the second fraction upside down and multiply. Reduce the result by dividing both parts by the same factor.",
                    "1/2 + 1/4 = ?", List.of("Rewrite 1/2 as 2/4.", "Add the numerators: 2/4 + 1/4 = 3/4.", "3/4 is already reduced. It is also 0.75."),
                    "Equivalent fractions are accepted: 3/4 and 6/8 represent the same number.");
            case ALGEBRA -> new Lesson("Keep both sides balanced.",
                    "A letter stands for an unknown number. Find it by undoing the operations around it. Whatever you add, subtract, multiply or divide on one side, do on the other. Check by putting your answer into the original equation.",
                    "3x + 5 = 20", List.of("Subtract 5 on both sides: 3x = 15.", "Divide both sides by 3: x = 5.", "Check: 3 × 5 + 5 = 20."),
                    "For 2(x + 3) = 14, divide by 2 before subtracting 3. Enter just the value of x.");
            case GEOMETRY -> new Lesson("Choose the formula that fits the question.",
                    "Perimeter is the distance around a shape. Area measures its surface. Volume measures the space inside. For a rectangle, area = length × width and perimeter = 2 × (length + width). Triangle area = ½ × base × perpendicular height. A box has volume = length × width × height.",
                    "A triangle has base 8 cm and height 5 cm.", List.of("Use A = ½ × base × height.", "A = ½ × 8 × 5 = 20.", "The area is 20 cm². Enter 20 in the answer box."),
                    "Circle area = π × radius². MathPath circle questions explicitly use π = 3.14.");
            case PERCENTAGES -> new Lesson("Percent means out of one hundred.",
                    "Write a percentage as a fraction over 100 or divide it by 100 to get a decimal. Multiply by the original amount to find the part. Subtract that part for a discount, or add it for an increase. To recover an original amount, divide the known part by its percentage as a decimal.",
                    "A R200 bag has a 15% discount.", List.of("15% = 15/100 = 0.15.", "Discount = 0.15 × 200 = R30.", "Sale price = 200 − 30 = R170."),
                    "An increase of 20% means the new total is 120% of the original amount.");
        };
    }
}
