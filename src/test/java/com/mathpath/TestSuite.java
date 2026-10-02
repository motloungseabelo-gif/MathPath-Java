package com.mathpath;

import com.mathpath.core.Attempt;
import com.mathpath.core.Difficulty;
import com.mathpath.core.LearningService;
import com.mathpath.core.Lessons;
import com.mathpath.core.Progress;
import com.mathpath.core.Question;
import com.mathpath.core.QuestionGenerator;
import com.mathpath.core.QuizResult;
import com.mathpath.core.Rational;
import com.mathpath.core.Topic;
import com.mathpath.storage.ProgressRepository;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.mathpath.Checks.equal;
import static com.mathpath.Checks.that;
import static com.mathpath.Checks.throwsType;

/** Behavioral tests plus independently recomputed answers for 15,000 generated exercises. */
public final class TestSuite {
    private static int passed, failed;
    private static final Instant NOW = Instant.parse("2026-10-02T12:00:00Z");
    private TestSuite() { }

    public static void main(String[] args) throws Exception {
        test("fractions normalize signs and reduce", () -> { equal(new Rational(-1, 2), new Rational(2, -4)); equal("0", new Rational(0, 4).toString()); });
        test("integer, decimal and comma answers", () -> { equal(Rational.of(-12), Rational.parse(" -12 ")); equal(new Rational(3, 4), Rational.parse("0.75")); equal(new Rational(3, 4), Rational.parse("0,75")); equal(new Rational(1, 2), Rational.parse(".5")); });
        test("equivalent fractions and spaced division", () -> equal(new Rational(3, 4), Rational.parse(" 6 / 8 ")));
        test("exact rational operations", () -> { equal(new Rational(5, 6), new Rational(1, 2).add(new Rational(1, 3))); equal(new Rational(1, 6), new Rational(1, 2).subtract(new Rational(1, 3))); equal(new Rational(1, 6), new Rational(1, 2).multiply(new Rational(1, 3))); equal(new Rational(3, 2), new Rational(1, 2).divide(new Rational(1, 3))); });
        test("zero denominators are rejected", () -> { throwsType(IllegalArgumentException.class, () -> Rational.parse("1/0")); throwsType(IllegalArgumentException.class, () -> Rational.of(1).divide(Rational.of(0))); });
        test("malformed or oversized answers are rejected", () -> { for (String input : List.of("", "NaN", "Infinity", "1e9", "x=5", "25%", "1/2/3", "1.2.3", "1000000000", "2 cm", "0".repeat(100))) throwsType(IllegalArgumentException.class, () -> Rational.parse(input)); });
        test("rounding accepts hundredths without accepting inaccurate fractions", () -> { that(new Rational(1, 3).accepts("0.33"), "Rounded decimal rejected"); that(new Rational(2, 3).accepts("0.67"), "Rounded decimal rejected"); that(!new Rational(1, 3).accepts("33/100"), "Inexact fraction accepted"); that(!new Rational(1, 3).accepts("0.34"), "Wrong decimal accepted"); that(new Rational(-2, 3).accepts("-0.67"), "Negative rounding failed"); });
        test("question collections are immutable", () -> { Question q = generator().generate(Topic.FRACTIONS, Difficulty.BUILDER); throwsType(UnsupportedOperationException.class, () -> q.steps().add("changed")); throwsType(UnsupportedOperationException.class, () -> q.parameters().put("a", 99L)); });
        test("15,000 generated answers match independent arithmetic", TestSuite::auditGenerator);
        test("five original lessons include examples and worked steps", () -> { for (Topic t : Topic.values()) { var lesson = Lessons.forTopic(t); that(!lesson.rule().isBlank() && !lesson.explanation().isBlank() && !lesson.tip().isBlank(), "Incomplete lesson"); equal(3, lesson.steps().size()); } });
        test("empty progress is valid", () -> { var s = new Progress().snapshot(); equal(0L, s.answered()); equal(0, s.accuracy()); equal(5, s.topics().size()); });
        test("next requires a completed answer", () -> { LearningService s = service(new Progress()); s.startPractice(Topic.ARITHMETIC, null); throwsType(IllegalStateException.class, s::next); });
        test("invalid input cannot change progress", () -> { Progress p = new Progress(); LearningService s = service(p); s.startPractice(Topic.ALGEBRA, null); throwsType(IllegalArgumentException.class, () -> s.submit("bad")); throwsType(IllegalArgumentException.class, () -> s.submit("3/0")); equal(0L, p.snapshot().answered()); that(!s.answered(), "Invalid input consumed the question"); });
        test("each question can only be graded once", () -> { Progress p = new Progress(); LearningService s = service(p); s.startPractice(Topic.ARITHMETIC, null); s.submit(s.current().answer().toString()); throwsType(IllegalStateException.class, () -> s.submit("1")); throwsType(IllegalStateException.class, s::reveal); throwsType(IllegalStateException.class, s::hint); equal(1L, p.snapshot().answered()); });
        test("correct independent answers earn full points and streak", () -> { Progress p = new Progress(); LearningService s = service(p); s.startPractice(Topic.ARITHMETIC, Difficulty.BUILDER); equal(15, s.submit(s.current().answer().toString()).points()); equal(15L, p.snapshot().xp()); equal(1, p.snapshot().streak()); equal(1, p.snapshot().bestStreak()); });
        test("hints earn half points and break the independent streak", () -> { Progress p = new Progress(); LearningService s = service(p); s.startPractice(Topic.ALGEBRA, Difficulty.BUILDER); s.hint(); equal(7, s.submit(s.current().answer().toString()).points()); equal(1L, p.snapshot().correct()); equal(0, p.snapshot().streak()); });
        test("revealed solutions earn no points and enter review", () -> { Progress p = new Progress(); LearningService s = service(p); s.startPractice(Topic.GEOMETRY, null); s.reveal(); equal(0L, p.snapshot().xp()); equal(0L, p.snapshot().correct()); equal(1, p.snapshot().mistakes().size()); that(p.snapshot().recent().get(0).revealed(), "Reveal flag missing"); });
        test("wrong answers enter review", () -> { Progress p = new Progress(); LearningService s = service(p); s.startPractice(Topic.ARITHMETIC, null); s.submit(s.current().answer().add(Rational.of(1)).toString()); equal(1, p.snapshot().mistakes().size()); equal(0L, p.snapshot().correct()); });
        test("review needs an independent correct answer to clear a mistake", TestSuite::reviewLifecycle);
        test("an empty review queue does not replace a practice session", () -> { LearningService s = service(new Progress()); s.startPractice(Topic.ALGEBRA, null); String id = s.current().id(); that(!s.startReview(), "Empty review started"); equal(id, s.current().id()); });
        test("adaptive increases require six attempts at each level", TestSuite::adaptiveLifecycle);
        test("adaptive decreases after six struggling attempts", () -> { Progress p = new Progress(); for (int i = 0; i < 6; i++) record(p, generator().generate(Topic.ALGEBRA, Difficulty.BUILDER), false, false); equal(Difficulty.STARTER, p.recommendedDifficulty(Topic.ALGEBRA)); });
        test("hints prevent an adaptive level increase", () -> { Progress p = new Progress(); for (int i = 0; i < 6; i++) record(p, generator().generate(Topic.ALGEBRA, Difficulty.STARTER), true, true); equal(Difficulty.STARTER, p.recommendedDifficulty(Topic.ALGEBRA)); });
        test("topic difficulty adapts independently", () -> { Progress p = new Progress(); for (int i = 0; i < 6; i++) record(p, generator().generate(Topic.ALGEBRA, Difficulty.STARTER), true, false); equal(Difficulty.BUILDER, p.recommendedDifficulty(Topic.ALGEBRA)); equal(Difficulty.STARTER, p.recommendedDifficulty(Topic.GEOMETRY)); });
        test("fixed practice levels remain fixed", () -> { LearningService s = service(new Progress()); s.startPractice(Topic.ARITHMETIC, Difficulty.STARTER); for (int i = 0; i < 9; i++) { s.submit(s.current().answer().toString()); s.next(); equal(Difficulty.STARTER, s.current().difficulty()); } });
        test("quiz contains two questions per topic and records one result", TestSuite::quizLifecycle);
        test("quiz skips count as wrong and hints are unavailable", () -> { Progress p = new Progress(); LearningService s = service(p); s.startQuiz(Difficulty.CHALLENGE); throwsType(IllegalStateException.class, s::hint); for (int i = 0; i < 10; i++) { s.reveal(); s.next(); } equal(0, p.snapshot().quizzes().get(0).correct()); equal(10L, p.snapshot().answered()); equal(10, p.snapshot().mistakes().size()); });
        test("unfinished quizzes preserve attempts but no quiz result", () -> { Progress p = new Progress(); LearningService s = service(p); s.startQuiz(Difficulty.STARTER); s.submit(s.current().answer().toString()); s.startPractice(Topic.ALGEBRA, null); equal(1L, p.snapshot().answered()); equal(0, p.snapshot().quizzes().size()); });
        test("history and review bounds preserve lifetime totals", () -> { Progress p = new Progress(); QuestionGenerator g = generator(); for (int i = 0; i < 650; i++) record(p, g.generate(Topic.FRACTIONS, Difficulty.BUILDER), false, false); equal(650L, p.snapshot().answered()); equal(500, p.snapshot().recent().size()); equal(100, p.snapshot().mistakes().size()); equal(650L, p.snapshot().topics().get(Topic.FRACTIONS).answered()); });
        test("quiz history retains the latest twenty results", () -> { Progress p = new Progress(); for (int i = 0; i < 25; i++) p.addQuiz(new QuizResult(i % 11, 10, Difficulty.STARTER, NOW.plusSeconds(i))); equal(20, p.snapshot().quizzes().size()); equal(NOW.plusSeconds(5), p.snapshot().quizzes().get(0).completedAt()); });
        test("mismatched attempts are rejected without mutation", () -> { Progress p = new Progress(); Question q = generator().generate(Topic.ALGEBRA, Difficulty.STARTER); throwsType(IllegalArgumentException.class, () -> p.record(q, new Attempt("wrong-id", q.topic(), q.difficulty(), true, false, false, NOW))); equal(0L, p.snapshot().answered()); });
        test("missing save data starts a fresh profile", () -> withDirectory(dir -> { var load = new ProgressRepository(dir).load(); equal(0L, load.progress().snapshot().answered()); equal("", load.warning()); }));
        test("saved totals, unicode, review questions and quiz results round-trip", TestSuite::persistenceRoundTrip);
        test("damaged data is backed up before recovery", () -> withDirectory(dir -> { ProgressRepository r = new ProgressRepository(dir); Files.writeString(r.file(), "schema=1\nanswered=oops"); var load = r.load(); that(!load.warning().isBlank(), "Missing recovery warning"); that(!Files.exists(r.file()), "Damaged data was not quarantined"); try (var files = Files.list(dir)) { equal(1L, files.filter(f -> f.getFileName().toString().startsWith("progress.corrupt-")).count()); } r.save(load.progress().snapshot()); equal(0L, r.load().progress().snapshot().answered()); }));
        test("unsupported schema is preserved and cannot be overwritten", () -> withDirectory(dir -> { ProgressRepository r = new ProgressRepository(dir); String original = "schema=99\nfuture-data=keep me"; Files.writeString(r.file(), original); that(!r.load().warning().isBlank(), "Missing version warning"); throwsType(IOException.class, () -> r.save(new Progress().snapshot())); equal(original, Files.readString(r.file())); }));
        test("inconsistent counters recover instead of displaying fake totals", () -> withDirectory(dir -> { ProgressRepository r = new ProgressRepository(dir); r.save(new Progress().snapshot()); String data = Files.readString(r.file()).replace("answered=0", "answered=7"); Files.writeString(r.file(), data); that(!r.load().warning().isBlank(), "Invalid counters were accepted"); equal(0L, r.load().progress().snapshot().answered()); }));
        test("failed saves report an error and leave no temporary files", () -> withDirectory(dir -> { Path blocked = dir.resolve("blocked"); Files.writeString(blocked, "file, not folder"); ProgressRepository r = new ProgressRepository(blocked); throwsType(IOException.class, () -> r.save(new Progress().snapshot())); equal("file, not folder", Files.readString(blocked)); }));
        System.out.println("Core tests: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) throw new AssertionError("Core test suite failed.");
    }

    private static void test(String name, Checks.Throwing run) {
        try { run.run(); passed++; System.out.println("PASS " + name); }
        catch (Throwable e) { failed++; System.err.println("FAIL " + name); e.printStackTrace(); }
    }
    private static QuestionGenerator generator() { return new QuestionGenerator(new Random(4319)); }
    private static LearningService service(Progress p) { return new LearningService(generator(), p, Clock.fixed(NOW, ZoneOffset.UTC), new Random(22)); }
    private static void record(Progress p, Question q, boolean correct, boolean hinted) { p.record(q, new Attempt(q.id(), q.topic(), q.difficulty(), correct, hinted, false, NOW)); }

    private static void auditGenerator() {
        QuestionGenerator g = generator(); Set<String> kinds = new HashSet<>(); Set<String> ids = new HashSet<>();
        for (Topic t : Topic.values()) for (Difficulty d : Difficulty.values()) for (int i = 0; i < 1000; i++) {
            Question q = g.generate(t, d); kinds.add(q.kind()); that(ids.add(q.id()), "Duplicate question id");
            equal(t, q.topic()); equal(d, q.difficulty()); equal(referenceAnswer(q), q.answer());
            that(q.answer().accepts(q.answer().toString()), "Exact answer rejected");
            that(q.answer().accepts(q.answer().decimal()), "Rounded answer rejected");
            that(!q.answer().accepts(q.answer().add(Rational.of(1)).toString()), "Incorrect answer accepted");
            that(q.steps().stream().noneMatch(String::isBlank), "Empty solution step");
            if (q.kind().startsWith("fraction")) that(q.parameters().get("b") > 0 && q.parameters().get("d") > 0, "Invalid fraction denominator");
            if (q.kind().equals("circle")) that(q.prompt().contains("3.14"), "Circle omitted pi convention");
        }
        equal(20, kinds.size());
    }

    private static Rational referenceAnswer(Question q) {
        Map<String, Long> p = q.parameters(); long a = p.getOrDefault("a", 0L), b = p.getOrDefault("b", 0L),
                c = p.getOrDefault("c", 0L), d = p.getOrDefault("d", 1L), rate = p.getOrDefault("p", 0L), amount = p.getOrDefault("amount", 0L);
        // Independent closed-form equations rather than calls back into generation or marking.
        return switch (q.kind()) {
            case "add" -> Rational.of(a + b); case "subtract" -> Rational.of(a - b);
            case "multiply", "rectangle" -> Rational.of(a * b); case "divide" -> new Rational(a, b);
            case "order" -> Rational.of(a + b * c);
            case "fractionAdd" -> new Rational(a * d + c * b, b * d);
            case "fractionSubtract" -> new Rational(a * d - c * b, b * d);
            case "fractionMultiply" -> new Rational(a * c, b * d);
            case "fractionDivide" -> new Rational(a * d, b * c);
            case "linear" -> new Rational(c - b, a); case "bracket" -> new Rational(c - a * b, a);
            case "perimeter" -> Rational.of(a + b + a + b); case "triangle" -> new Rational(a * b, 2);
            case "cuboid" -> Rational.of(a * b * c); case "circle" -> new Rational(157L * p.get("r") * p.get("r"), 50);
            case "percent" -> new Rational(rate * amount, 100); case "discount" -> new Rational((100 - rate) * amount, 100);
            case "increase" -> new Rational((100 + rate) * amount, 100); case "reversePercent" -> Rational.of(amount);
            default -> throw new AssertionError("Missing independent equation for " + q.kind());
        };
    }

    private static void reviewLifecycle() {
        Progress p = new Progress(); LearningService s = service(p); s.startPractice(Topic.FRACTIONS, Difficulty.BUILDER); s.reveal();
        that(s.startReview(), "Review did not start"); s.hint(); s.submit(s.current().answer().toString()); s.next();
        equal(1, p.snapshot().mistakes().size());
        s.startReview(); s.submit(s.current().answer().toString()); that(!s.next(), "Review did not finish"); equal(0, p.snapshot().mistakes().size());
    }

    private static void adaptiveLifecycle() {
        LearningService s = service(new Progress()); s.startPractice(Topic.ALGEBRA, null);
        for (int i = 0; i < 6; i++) { equal(Difficulty.STARTER, s.current().difficulty()); s.submit(s.current().answer().toString()); s.next(); }
        equal(Difficulty.BUILDER, s.current().difficulty());
        for (int i = 0; i < 6; i++) { equal(Difficulty.BUILDER, s.current().difficulty()); s.submit(s.current().answer().toString()); s.next(); }
        equal(Difficulty.CHALLENGE, s.current().difficulty());
    }

    private static void quizLifecycle() {
        Progress p = new Progress(); LearningService s = service(p); s.startQuiz(Difficulty.BUILDER);
        Map<Topic, Integer> counts = new EnumMap<>(Topic.class);
        for (int i = 0; i < 10; i++) {
            equal(i + 1, s.questionNumber()); equal(10, s.questionCount()); counts.merge(s.current().topic(), 1, Integer::sum);
            s.submit(s.current().answer().toString()); equal(i < 9, s.next());
        }
        for (Topic t : Topic.values()) equal(2, counts.get(t));
        equal(1, p.snapshot().quizzes().size()); equal(10, p.snapshot().quizzes().get(0).correct());
        that(s.finished(), "Quiz did not finish"); throwsType(IllegalStateException.class, s::next); equal(1, p.snapshot().quizzes().size());
    }

    private static void persistenceRoundTrip() throws Exception {
        withDirectory(dir -> {
            Progress p = new Progress(); LearningService s = service(p);
            s.startPractice(Topic.GEOMETRY, Difficulty.CHALLENGE); s.reveal();
            s.startPractice(Topic.FRACTIONS, Difficulty.BUILDER); s.hint(); s.submit(s.current().answer().toString());
            s.startQuiz(Difficulty.BUILDER); for (int i = 0; i < 10; i++) { s.submit(s.current().answer().toString()); s.next(); }
            ProgressRepository r = new ProgressRepository(dir); r.save(p.snapshot());
            var loaded = r.load(); equal("", loaded.warning()); equal(p.snapshot(), loaded.progress().snapshot());
            r.save(loaded.progress().snapshot()); equal(p.snapshot(), r.load().progress().snapshot());
            try (var files = Files.list(dir)) { equal(1L, files.count()); }
        });
    }

    private static void withDirectory(DirectoryTest run) throws Exception {
        Path dir = Files.createTempDirectory("mathpath-test-");
        try { run.run(dir); } finally {
            try (var paths = Files.walk(dir)) { for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
    }
    @FunctionalInterface private interface DirectoryTest { void run(Path directory) throws Exception; }
}
