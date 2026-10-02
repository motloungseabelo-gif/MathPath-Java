package com.mathpath.storage;

import com.mathpath.core.Attempt;
import com.mathpath.core.Difficulty;
import com.mathpath.core.Progress;
import com.mathpath.core.Question;
import com.mathpath.core.QuizResult;
import com.mathpath.core.Rational;
import com.mathpath.core.Topic;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Versioned UTF-8 storage; saves replace atomically and damaged data is preserved before recovery. */
public final class ProgressRepository {
    public record Loaded(Progress progress, String warning) { }
    private static final int SCHEMA = 1;
    private static final long MAX_BYTES = 4 * 1024 * 1024;
    private final Path file;
    private boolean readOnly;

    public ProgressRepository(Path directory) { file = directory.toAbsolutePath().resolve("progress.properties"); }
    public Path file() { return file; }

    public static ProgressRepository userDefault() {
        String override = System.getProperty("mathpath.dataDir");
        return new ProgressRepository(override == null ? Path.of(System.getProperty("user.home"), ".mathpath") : Path.of(override));
    }

    public Loaded load() {
        if (!Files.exists(file)) return new Loaded(new Progress(), "");
        Properties p = new Properties();
        try {
            if (Files.size(file) > MAX_BYTES) throw new IllegalArgumentException("Progress file is too large.");
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(reader); }
            if (integer(p, "schema") != SCHEMA) {
                readOnly = true;
                return new Loaded(new Progress(), "Your saved data uses an unsupported version. It has been preserved. Saving is disabled for this data folder.");
            }
            Map<Topic, Progress.TopicStats> topics = new EnumMap<>(Topic.class);
            for (Topic t : Topic.values()) topics.put(t, new Progress.TopicStats(number(p, "topic." + t + ".answered"), number(p, "topic." + t + ".correct")));
            List<Attempt> recent = new ArrayList<>();
            int historySize = count(p, "history.size", Progress.HISTORY_LIMIT);
            for (int i = 0; i < historySize; i++) {
                String k = "history." + i + ".";
                recent.add(new Attempt(required(p, k + "id"), Topic.valueOf(required(p, k + "topic")),
                        Difficulty.valueOf(required(p, k + "difficulty")), bool(p, k + "correct"),
                        bool(p, k + "hinted"), bool(p, k + "revealed"), Instant.parse(required(p, k + "at"))));
            }
            List<Question> mistakes = new ArrayList<>();
            int reviewSize = count(p, "review.size", Progress.REVIEW_LIMIT);
            for (int i = 0; i < reviewSize; i++) mistakes.add(readQuestion(p, "review." + i + "."));
            List<QuizResult> quizzes = new ArrayList<>();
            int quizSize = count(p, "quizzes.size", Progress.QUIZ_LIMIT);
            for (int i = 0; i < quizSize; i++) {
                String k = "quizzes." + i + ".";
                quizzes.add(new QuizResult(integer(p, k + "correct"), integer(p, k + "total"),
                        Difficulty.valueOf(required(p, k + "difficulty")), Instant.parse(required(p, k + "at"))));
            }
            return new Loaded(new Progress(new Progress.Snapshot(number(p, "answered"), number(p, "correct"), number(p, "xp"),
                    integer(p, "streak"), integer(p, "bestStreak"), topics, recent, mistakes, quizzes)), "");
        } catch (IOException e) {
            readOnly = true;
            return new Loaded(new Progress(), "Saved progress could not be read. It has been preserved. Saving is disabled for this data folder.");
        } catch (RuntimeException e) {
            try {
                Path backup = file.resolveSibling("progress.corrupt-" + UUID.randomUUID() + ".properties");
                Files.move(file, backup);
                return new Loaded(new Progress(), "The saved progress file was damaged. A backup was kept and a fresh profile is ready.");
            } catch (IOException backupFailed) {
                readOnly = true;
                return new Loaded(new Progress(), "Saved progress was damaged and could not be backed up. It has been preserved. Saving is disabled.");
            }
        }
    }

    public void save(Progress.Snapshot progress) throws IOException {
        if (readOnly) throw new IOException("Saving is disabled to protect unreadable or unsupported saved data.");
        Properties p = new Properties();
        put(p, "schema", SCHEMA); put(p, "answered", progress.answered()); put(p, "correct", progress.correct());
        put(p, "xp", progress.xp()); put(p, "streak", progress.streak()); put(p, "bestStreak", progress.bestStreak());
        for (Topic t : Topic.values()) {
            put(p, "topic." + t + ".answered", progress.topics().get(t).answered());
            put(p, "topic." + t + ".correct", progress.topics().get(t).correct());
        }
        put(p, "history.size", progress.recent().size());
        for (int i = 0; i < progress.recent().size(); i++) {
            Attempt a = progress.recent().get(i); String k = "history." + i + ".";
            put(p, k + "id", a.questionId()); put(p, k + "topic", a.topic()); put(p, k + "difficulty", a.difficulty().name());
            put(p, k + "correct", a.correct()); put(p, k + "hinted", a.hinted()); put(p, k + "revealed", a.revealed()); put(p, k + "at", a.at());
        }
        put(p, "review.size", progress.mistakes().size());
        for (int i = 0; i < progress.mistakes().size(); i++) writeQuestion(p, "review." + i + ".", progress.mistakes().get(i));
        put(p, "quizzes.size", progress.quizzes().size());
        for (int i = 0; i < progress.quizzes().size(); i++) {
            QuizResult q = progress.quizzes().get(i); String k = "quizzes." + i + ".";
            put(p, k + "correct", q.correct()); put(p, k + "total", q.total()); put(p, k + "difficulty", q.difficulty().name()); put(p, k + "at", q.completedAt());
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "progress-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { p.store(writer, "MathPath progress • schema 1"); }
            if (Files.size(temporary) > MAX_BYTES) throw new IOException("Saved progress exceeds the supported size.");
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private Question readQuestion(Properties p, String k) {
        List<String> steps = new ArrayList<>();
        for (int i = 0, n = count(p, k + "steps", 20); i < n; i++) steps.add(required(p, k + "step." + i));
        Map<String, Long> parameters = new HashMap<>();
        for (String name : p.stringPropertyNames()) if (name.startsWith(k + "param.")) parameters.put(name.substring((k + "param.").length()), number(p, name));
        return new Question(required(p, k + "id"), Topic.valueOf(required(p, k + "topic")), Difficulty.valueOf(required(p, k + "difficulty")),
                required(p, k + "kind"), required(p, k + "prompt"), Rational.parse(required(p, k + "answer")), required(p, k + "hint"), steps, parameters);
    }

    private void writeQuestion(Properties p, String k, Question q) {
        put(p, k + "id", q.id()); put(p, k + "topic", q.topic()); put(p, k + "difficulty", q.difficulty().name());
        put(p, k + "kind", q.kind()); put(p, k + "prompt", q.prompt()); put(p, k + "answer", q.answer()); put(p, k + "hint", q.hint());
        put(p, k + "steps", q.steps().size());
        for (int i = 0; i < q.steps().size(); i++) put(p, k + "step." + i, q.steps().get(i));
        q.parameters().forEach((name, value) -> put(p, k + "param." + name, value));
    }

    private static void put(Properties p, String key, Object value) { p.setProperty(key, String.valueOf(value)); }
    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.length() > 4096) throw new IllegalArgumentException("Missing or invalid field: " + key);
        return value;
    }
    private static long number(Properties p, String key) { return Long.parseLong(required(p, key)); }
    private static int integer(Properties p, String key) { return Integer.parseInt(required(p, key)); }
    private static boolean bool(Properties p, String key) {
        String value = required(p, key);
        if (!value.equals("true") && !value.equals("false")) throw new IllegalArgumentException("Invalid boolean.");
        return Boolean.parseBoolean(value);
    }
    private static int count(Properties p, String key, int max) {
        int size = integer(p, key);
        if (size < 0 || size > max) throw new IllegalArgumentException("Invalid collection size.");
        return size;
    }
}
