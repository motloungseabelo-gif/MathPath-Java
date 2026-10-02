package com.mathpath.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Single-learner local progress. Lifetime totals are separate from bounded recent history. */
public final class Progress {
    public static final int HISTORY_LIMIT = 500;
    public static final int REVIEW_LIMIT = 100;
    public static final int QUIZ_LIMIT = 20;

    public record TopicStats(long answered, long correct) {
        public TopicStats {
            if (answered < 0 || correct < 0 || correct > answered) throw new IllegalArgumentException("Invalid topic totals.");
        }
        public int accuracy() { return answered == 0 ? 0 : (int) Math.round(100.0 * correct / answered); }
    }

    public record Snapshot(long answered, long correct, long xp, int streak, int bestStreak,
                           Map<Topic, TopicStats> topics, List<Attempt> recent,
                           List<Question> mistakes, List<QuizResult> quizzes) {
        public Snapshot {
            if (answered < 0 || correct < 0 || correct > answered || xp < 0 || streak < 0
                    || streak > correct || bestStreak < streak || bestStreak > correct) {
                throw new IllegalArgumentException("Invalid progress totals.");
            }
            topics = Map.copyOf(topics);
            recent = List.copyOf(recent);
            mistakes = List.copyOf(mistakes);
            quizzes = List.copyOf(quizzes);
            if (recent.size() > HISTORY_LIMIT || mistakes.size() > REVIEW_LIMIT || quizzes.size() > QUIZ_LIMIT) {
                throw new IllegalArgumentException("Progress history exceeds its limit.");
            }
            long topicAnswered = 0, topicCorrect = 0;
            for (Topic t : Topic.values()) {
                TopicStats stat = Objects.requireNonNull(topics.get(t));
                topicAnswered = Math.addExact(topicAnswered, stat.answered());
                topicCorrect = Math.addExact(topicCorrect, stat.correct());
            }
            if (topicAnswered != answered || topicCorrect != correct || recent.size() > answered) {
                throw new IllegalArgumentException("Progress totals do not match topic history.");
            }
            if (mistakes.stream().map(Question::id).distinct().count() != mistakes.size()) {
                throw new IllegalArgumentException("Duplicate review questions.");
            }
        }
        public int accuracy() { return answered == 0 ? 0 : (int) Math.round(100.0 * correct / answered); }
    }

    private long answered, correct, xp;
    private int streak, bestStreak;
    private final EnumMap<Topic, TopicStats> topics = new EnumMap<>(Topic.class);
    private final List<Attempt> recent = new ArrayList<>();
    private final LinkedHashMap<String, Question> mistakes = new LinkedHashMap<>();
    private final List<QuizResult> quizzes = new ArrayList<>();

    public Progress() { for (Topic t : Topic.values()) topics.put(t, new TopicStats(0, 0)); }

    public Progress(Snapshot saved) {
        answered = saved.answered(); correct = saved.correct(); xp = saved.xp();
        streak = saved.streak(); bestStreak = saved.bestStreak();
        topics.putAll(saved.topics()); recent.addAll(saved.recent()); quizzes.addAll(saved.quizzes());
        for (Question q : saved.mistakes()) mistakes.put(q.id(), q);
    }

    public synchronized void record(Question question, Attempt attempt) {
        if (!question.id().equals(attempt.questionId()) || question.topic() != attempt.topic()
                || question.difficulty() != attempt.difficulty()) throw new IllegalArgumentException("Attempt does not match the question.");
        answered++;
        if (attempt.correct()) correct++;
        xp += attempt.points();
        streak = attempt.correct() && !attempt.hinted() ? streak + 1 : 0;
        bestStreak = Math.max(streak, bestStreak);
        TopicStats stats = topics.get(question.topic());
        topics.put(question.topic(), new TopicStats(stats.answered() + 1, stats.correct() + (attempt.correct() ? 1 : 0)));
        recent.add(attempt);
        if (recent.size() > HISTORY_LIMIT) recent.remove(0);
        if (attempt.correct() && !attempt.hinted()) mistakes.remove(question.id());
        else if (!attempt.correct()) {
            mistakes.put(question.id(), question);
            if (mistakes.size() > REVIEW_LIMIT) mistakes.remove(mistakes.keySet().iterator().next());
        }
    }

    public synchronized void addQuiz(QuizResult result) {
        quizzes.add(result);
        if (quizzes.size() > QUIZ_LIMIT) quizzes.remove(0);
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(answered, correct, xp, streak, bestStreak, topics, recent, new ArrayList<>(mistakes.values()), quizzes);
    }

    /** At least six consecutive attempts at the same level are needed before an adaptive change. */
    public synchronized Difficulty recommendedDifficulty(Topic topic) {
        List<Attempt> matching = recent.stream().filter(a -> a.topic() == topic).toList();
        if (matching.isEmpty()) return Difficulty.STARTER;
        Difficulty current = matching.get(matching.size() - 1).difficulty();
        int count = 0, independent = 0, successes = 0;
        for (int i = matching.size() - 1; i >= 0 && count < 6; i--) {
            Attempt attempt = matching.get(i);
            if (attempt.difficulty() != current) break;
            count++;
            if (attempt.correct()) successes++;
            if (attempt.correct() && !attempt.hinted()) independent++;
        }
        if (count < 6) return current;
        if (independent >= 5 && current != Difficulty.CHALLENGE) return Difficulty.values()[current.ordinal() + 1];
        if (successes <= 2 && current != Difficulty.STARTER) return Difficulty.values()[current.ordinal() - 1];
        return current;
    }
}
