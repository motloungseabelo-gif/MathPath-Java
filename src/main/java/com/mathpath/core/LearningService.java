package com.mathpath.core;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Session state machine shared by the GUI and tests; each question can be marked once. */
public final class LearningService {
    public enum Mode { PRACTICE, REVIEW, QUIZ }
    public record Feedback(boolean correct, boolean hinted, boolean revealed, int points, Question question) { }
    public record Summary(Mode mode, int answered, int correct, int points) { }

    private final QuestionGenerator generator;
    private final Progress progress;
    private final Clock clock;
    private final Random shuffleRandom;
    private Mode mode;
    private Topic topic;
    private Difficulty fixedDifficulty;
    private List<Question> questions = List.of();
    private Question current;
    private int index, answeredCount, correctCount, points;
    private boolean answered, hinted, finished;

    public LearningService(QuestionGenerator generator, Progress progress) {
        this(generator, progress, Clock.systemUTC(), new Random());
    }
    public LearningService(QuestionGenerator generator, Progress progress, Clock clock, Random shuffleRandom) {
        this.generator = Objects.requireNonNull(generator);
        this.progress = Objects.requireNonNull(progress);
        this.clock = Objects.requireNonNull(clock);
        this.shuffleRandom = Objects.requireNonNull(shuffleRandom);
    }

    public void startPractice(Topic topic, Difficulty difficulty) {
        this.topic = Objects.requireNonNull(topic);
        fixedDifficulty = difficulty; // null selects adaptive learning.
        reset(Mode.PRACTICE);
        current = generator.generate(topic, difficulty == null ? progress.recommendedDifficulty(topic) : difficulty);
    }

    public void startQuiz(Difficulty difficulty) {
        fixedDifficulty = Objects.requireNonNull(difficulty);
        reset(Mode.QUIZ);
        List<Question> quiz = new ArrayList<>();
        for (Topic t : Topic.values()) for (int i = 0; i < 2; i++) quiz.add(generator.generate(t, difficulty));
        Collections.shuffle(quiz, shuffleRandom);
        questions = List.copyOf(quiz);
        current = questions.get(0);
    }

    public boolean startReview() {
        List<Question> saved = progress.snapshot().mistakes();
        if (saved.isEmpty()) return false;
        reset(Mode.REVIEW);
        questions = saved;
        current = questions.get(0);
        return true;
    }

    private void reset(Mode mode) {
        this.mode = mode;
        current = null;
        questions = List.of(); index = 0; answeredCount = 0; correctCount = 0; points = 0;
        answered = false; hinted = false; finished = false;
    }

    public String hint() {
        ensureOpen();
        if (mode == Mode.QUIZ) throw new IllegalStateException("Hints are available in practice and review.");
        hinted = true;
        return current.hint();
    }

    public Feedback submit(String input) {
        ensureOpen();
        boolean correct = current.answer().accepts(input); // Invalid inputs leave all session state unchanged.
        return mark(correct, false);
    }

    public Feedback reveal() {
        ensureOpen();
        return mark(false, true);
    }

    private Feedback mark(boolean correct, boolean revealed) {
        Attempt attempt = new Attempt(current.id(), current.topic(), current.difficulty(), correct, hinted, revealed, clock.instant());
        progress.record(current, attempt);
        answered = true; answeredCount++;
        if (correct) correctCount++;
        points += attempt.points();
        if (mode == Mode.QUIZ && answeredCount == questions.size()) {
            progress.addQuiz(new QuizResult(correctCount, questions.size(), fixedDifficulty, clock.instant()));
        }
        return new Feedback(correct, hinted, revealed, attempt.points(), current);
    }

    public boolean next() {
        if (current == null || !answered || finished) throw new IllegalStateException("Answer this question before continuing.");
        if (mode == Mode.PRACTICE) {
            current = generator.generate(topic, fixedDifficulty == null ? progress.recommendedDifficulty(topic) : fixedDifficulty);
            index++;
        } else if (++index < questions.size()) current = questions.get(index);
        else { finished = true; return false; }
        answered = false; hinted = false;
        return true;
    }

    private void ensureOpen() {
        if (current == null || answered || finished) throw new IllegalStateException("This question is already complete or no session is active.");
    }

    public Question current() { return current; }
    public Mode mode() { return mode; }
    public int questionNumber() { return index + 1; }
    public int questionCount() { return mode == Mode.PRACTICE ? 0 : questions.size(); }
    public boolean answered() { return answered; }
    public boolean finished() { return finished; }
    public Summary summary() { return new Summary(mode, answeredCount, correctCount, points); }
}
