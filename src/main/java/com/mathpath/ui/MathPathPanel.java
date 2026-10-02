package com.mathpath.ui;

import com.mathpath.core.Difficulty;
import com.mathpath.core.LearningService;
import com.mathpath.core.Lessons;
import com.mathpath.core.Progress;
import com.mathpath.core.Question;
import com.mathpath.core.Topic;
import com.mathpath.storage.ProgressRepository;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Responsive, scrollable desktop views. All UI mutations run on Swing's event dispatch thread. */
public final class MathPathPanel extends JPanel implements AutoCloseable {
    private static final long serialVersionUID = 1L;
    private final Progress progress;
    private final ProgressRepository repository;
    private final LearningService learning;
    private final ExecutorService saves = Executors.newSingleThreadExecutor(r -> new Thread(r, "mathpath-save"));
    private final JPanel main = Theme.plain();
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private final JLabel status = Theme.label("Ready • Progress stays on this device.", 12, false, Theme.MUTED);
    private LearningService.Feedback lastFeedback;
    private String activeScreen = "home";

    public MathPathPanel(Progress progress, ProgressRepository repository, LearningService learning, String warning) {
        this.progress = progress; this.repository = repository; this.learning = learning;
        setLayout(new BorderLayout()); setBackground(Theme.BG);
        setPreferredSize(new Dimension(1240, 850));
        add(sidebar(), BorderLayout.WEST);
        JPanel stage = Theme.plain(); stage.setLayout(new BorderLayout());
        main.setLayout(new BorderLayout());
        stage.add(main, BorderLayout.CENTER);
        JPanel foot = Theme.plain(); foot.setLayout(new BorderLayout());
        foot.setBorder(BorderFactory.createEmptyBorder(7, 30, 12, 30));
        status.setName("storage-status"); foot.add(status, BorderLayout.CENTER);
        stage.add(foot, BorderLayout.SOUTH); add(stage, BorderLayout.CENTER);
        if (!warning.isBlank()) { status.setText(warning); status.setForeground(Theme.RED); }
        navigate("home");
    }

    private JPanel sidebar() {
        JPanel p = new JPanel(); p.setBackground(Theme.NAVY); p.setLayout(new BorderLayout());
        p.setPreferredSize(new Dimension(218, 800));
        p.setBorder(BorderFactory.createEmptyBorder(29, 19, 24, 19));
        JPanel top = vertical();
        JPanel brand = Theme.plain(); brand.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel logo = Theme.label("m.", 25, true, new Color(0xDAEBC8));
        logo.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        brand.add(logo); brand.add(Theme.label("MathPath", 22, true, Color.WHITE));
        add(top, brand); gap(top, 8);
        JLabel subtitle = Theme.label("MAKE PROGRESS, DAILY.", 10, true, new Color(0x9EBCB1));
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0)); add(top, subtitle); gap(top, 38);
        nav(top, "home", "01   Overview"); nav(top, "learn", "02   Lessons");
        nav(top, "practice", "03   Practice"); nav(top, "review", "04   Mistake review"); nav(top, "progress", "05   Progress");
        p.add(top, BorderLayout.NORTH);
        JPanel footer = vertical();
        JTextArea motto = Theme.text("A little practice.\nA lot of possibility.", 15, new Color(0xD8E5DA), 2);
        add(footer, motto); gap(footer, 23);
        add(footer, Theme.label("●  Offline & ready", 12, false, new Color(0xBBDDAD))); gap(footer, 8);
        add(footer, Theme.label("MathPath  /  Version 1.0", 10, false, new Color(0x91ABA0)));
        p.add(footer, BorderLayout.SOUTH);
        return p;
    }

    private void nav(JPanel parent, String key, String label) {
        JButton b = Theme.button(label, false, () -> navigate(key));
        b.setFont(Theme.font(13, true));
        b.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        b.setName("nav-" + key); b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); b.setPreferredSize(new Dimension(180, 46));
        navigation.put(key, b); add(parent, b); gap(parent, 8);
    }

    public void navigate(String screen) {
        activeScreen = screen;
        navigation.forEach((key, button) -> button.setEnabled(!key.equals(screen)));
        switch (screen) {
            case "home" -> overview();
            case "learn" -> lessonPicker();
            case "practice" -> {
                if (learning.current() != null && !learning.finished()) exercise(); else practicePicker();
            }
            case "review" -> review();
            case "progress" -> progressView();
            default -> throw new IllegalArgumentException("Unknown view: " + screen);
        }
    }

    private void display(JPanel view) {
        view.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        JScrollPane scroll = new JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null); scroll.getViewport().setBackground(Theme.BG); scroll.getVerticalScrollBar().setUnitIncrement(24);
        main.removeAll(); main.add(scroll, BorderLayout.CENTER); main.revalidate(); main.repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private JPanel page(String eyebrow, String title, String subtitle) {
        JPanel view = new ScrollPage();
        view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
        add(view, Theme.label(eyebrow.toUpperCase(), 11, true, Theme.GREEN)); gap(view, 10);
        JTextArea heading = Theme.text(title, 30, Theme.INK, title.length() > 50 ? 2 : 1);
        heading.setFont(Theme.font(30, true));
        add(view, heading); gap(view, 8);
        add(view, Theme.text(subtitle, 14, Theme.MUTED, 2)); gap(view, 20);
        return view;
    }

    private void overview() {
        Progress.Snapshot s = progress.snapshot();
        JPanel view = page("Your learning space", "Your next step starts here.", "Build confidence one question at a time. Pick a topic, try a hint, and keep moving.");
        JPanel hero = Theme.card(Theme.MINT, 23); hero.setLayout(new BorderLayout(10, 0));
        JPanel copy = vertical();
        add(copy, Theme.label("UNDERSTAND IT. THEN OWN IT.", 10, true, Theme.GREEN)); gap(copy, 13);
        add(copy, Theme.label("Make math make sense.", 26, true, Theme.INK)); gap(copy, 9);
        add(copy, Theme.text("Short lessons. Helpful hints. Real progress.\nYour path to a more confident you starts with one question.", 14, Theme.INK, 2)); gap(copy, 17);
        JButton begin = named(Theme.button("Start practicing  →", true, this::practicePicker), "start-from-home");
        add(copy, begin); hero.add(copy, BorderLayout.CENTER); hero.add(new MathArt(), BorderLayout.EAST);
        add(view, hero); gap(view, 18);
        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0)); metrics.setOpaque(false);
        metrics.add(metric("Questions answered", String.valueOf(s.answered()), "Every attempt is a step forward."));
        metrics.add(metric("Accuracy", s.answered() == 0 ? "—" : s.accuracy() + "%", "Correct answers across all sessions."));
        metrics.add(metric("Learning points", String.valueOf(s.xp()), "Build confidence, earn points."));
        add(view, metrics); gap(view, 24);
        JPanel lower = new JPanel(new GridLayout(1, 2, 18, 0)); lower.setOpaque(false);
        JPanel topics = vertical(); add(topics, Theme.label("Find your focus", 19, true, Theme.INK)); gap(topics, 12);
        for (Topic topic : Topic.values()) { add(topics, topicRow(topic, () -> startPractice(topic, null), "practice-" + topic)); gap(topics, 8); }
        lower.add(topics);
        JPanel right = vertical();
        JPanel quiz = Theme.card(Theme.NAVY, 23); quiz.setLayout(new BorderLayout());
        JPanel quizCopy = vertical();
        add(quizCopy, Theme.label("THE MIXED QUIZ", 10, true, new Color(0xBBDDAD))); gap(quizCopy, 15);
        add(quizCopy, Theme.label("Put it all together.", 23, true, Color.WHITE)); gap(quizCopy, 10);
        add(quizCopy, Theme.text("10 questions. All five topics.\nNo timer, no pressure. Just you and what you know. Hints stay off in quiz mode.", 14, new Color(0xD2E0D5), 4)); gap(quizCopy, 16);
        JComboBox<Difficulty> choice = new JComboBox<>(Difficulty.values()); choice.setName("quiz-difficulty"); choice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        add(quizCopy, choice); gap(quizCopy, 10);
        add(quizCopy, named(Theme.button("Take the quiz  →", false, () -> startQuiz((Difficulty) choice.getSelectedItem())), "start-quiz"));
        quiz.add(quizCopy); add(right, quiz); gap(right, 14);
        JPanel tip = Theme.card(Theme.WARM, 18); tip.setLayout(new BorderLayout());
        tip.add(Theme.text("A mistake is useful information. Save it, understand it, and try again when you are ready.", 14, Theme.INK, 3));
        add(right, tip); lower.add(right); add(view, lower); display(view);
    }

    private JPanel metric(String label, String value, String note) {
        JPanel p = Theme.card(Color.WHITE, 18); p.setLayout(new BorderLayout(0, 5));
        p.add(Theme.label(label.toUpperCase(), 10, true, Theme.MUTED), BorderLayout.NORTH);
        p.add(Theme.label(value, 28, true, Theme.INK), BorderLayout.CENTER);
        p.add(Theme.label(note, 10, false, Theme.MUTED), BorderLayout.SOUTH);
        return p;
    }

    private JPanel topicRow(Topic topic, Runnable action, String name) {
        JPanel p = Theme.card(Color.WHITE, 10); p.setLayout(new BorderLayout(12, 0));
        JLabel symbol = Theme.label(topic.symbol(), 22, true, Theme.GREEN); symbol.setHorizontalAlignment(SwingConstants.CENTER);
        symbol.setPreferredSize(new Dimension(38, 42)); p.add(symbol, BorderLayout.WEST);
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 3)); labels.setOpaque(false);
        labels.add(Theme.label(topic.title(), 14, true, Theme.INK)); labels.add(Theme.label(topic.description(), 10, false, Theme.MUTED));
        p.add(labels, BorderLayout.CENTER);
        JButton go = named(Theme.button("→", false, action), name);
        go.getAccessibleContext().setAccessibleName("Open " + topic.title()); p.add(go, BorderLayout.EAST);
        return p;
    }

    private void lessonPicker() {
        JPanel view = page("Learn the basics", "First understand. Then practice.", "Five short lessons to turn a formula into something that makes sense.");
        for (Topic t : Topic.values()) {
            add(view, topicRow(t, () -> lesson(t), "learn-" + t)); gap(view, 12);
        }
        gap(view, 12); add(view, Theme.text("You can return to these lessons at any time. Your progress is saved after each completed answer.", 14, Theme.MUTED, 2));
        display(view);
    }

    private void lesson(Topic topic) {
        Lessons.Lesson lesson = Lessons.forTopic(topic);
        JPanel view = page("Lesson " + topic.number() + " / " + topic.title(), lesson.rule(), topic.description());
        JPanel concept = Theme.card(Color.WHITE, 25); concept.setLayout(new BorderLayout(0, 12));
        concept.add(Theme.label("The idea", 19, true, Theme.INK), BorderLayout.NORTH);
        concept.add(Theme.text(lesson.explanation(), 17, Theme.INK, 6), BorderLayout.CENTER);
        add(view, concept); gap(view, 18);
        JPanel worked = Theme.card(Theme.MINT, 25); worked.setLayout(new BorderLayout(0, 14));
        worked.add(Theme.label("Let's work through it", 19, true, Theme.GREEN), BorderLayout.NORTH);
        JPanel steps = vertical(); add(steps, Theme.text(lesson.example(), 22, Theme.INK, 2)); gap(steps, 10);
        for (int i = 0; i < lesson.steps().size(); i++) { add(steps, Theme.text((i + 1) + ".  " + lesson.steps().get(i), 16, Theme.INK, 2)); gap(steps, 5); }
        worked.add(steps, BorderLayout.CENTER); add(view, worked); gap(view, 18);
        add(view, Theme.text(lesson.tip(), 15, Theme.MUTED, 2)); gap(view, 18);
        JPanel actions = flow(); actions.add(named(Theme.button("Practice " + topic.title() + "  →", true, () -> startPractice(topic, null)), "lesson-practice"));
        actions.add(Theme.button("All lessons", false, this::lessonPicker)); add(view, actions); display(view);
    }

    private void practicePicker() {
        activeScreen = "practice"; navigation.forEach((k, b) -> b.setEnabled(!k.equals("practice")));
        JPanel view = page("Practice", "Meet yourself where you are.", "Choose a topic and a level. Adaptive mode adjusts after six answers at the same level.");
        JPanel card = Theme.card(Color.WHITE, 28); card.setLayout(new BorderLayout());
        JPanel settings = vertical();
        add(settings, Theme.label("What would you like to work on?", 22, true, Theme.INK)); gap(settings, 22);
        JComboBox<Topic> topic = new JComboBox<>(Topic.values()); topic.setName("select-topic");
        topic.setRenderer((list, value, index, selected, focus) -> {
            JLabel l = Theme.label(value == null ? "" : value.title(), 15, false, Theme.INK);
            l.setOpaque(true); l.setBackground(selected ? Theme.MINT : Color.WHITE); l.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12)); return l;
        });
        add(settings, Theme.label("Topic", 13, true, Theme.MUTED)); gap(settings, 6); add(settings, topic); gap(settings, 20);
        JComboBox<String> difficulty = new JComboBox<>(new String[]{"Adaptive", "Starter", "Builder", "Challenge"}); difficulty.setName("select-difficulty");
        add(settings, Theme.label("Level", 13, true, Theme.MUTED)); gap(settings, 6); add(settings, difficulty); gap(settings, 22);
        add(settings, Theme.text("Adaptive starts gently. Five independent correct answers in the latest six at one level move you up. Two or fewer correct answers move you down. Hints help you learn and earn half points.", 15, Theme.MUTED, 4)); gap(settings, 24);
        add(settings, named(Theme.button("Start practice  →", true, () -> startPractice((Topic) topic.getSelectedItem(),
                difficulty.getSelectedIndex() == 0 ? null : Difficulty.values()[difficulty.getSelectedIndex() - 1])), "start-practice"));
        card.add(settings); add(view, card); display(view);
    }

    private void startPractice(Topic topic, Difficulty difficulty) {
        learning.startPractice(topic, difficulty); lastFeedback = null; activeScreen = "practice";
        navigation.forEach((k, b) -> b.setEnabled(!k.equals("practice"))); exercise();
    }

    private void startQuiz(Difficulty difficulty) {
        learning.startQuiz(difficulty); lastFeedback = null; activeScreen = "practice";
        navigation.forEach((k, b) -> b.setEnabled(!k.equals("practice"))); exercise();
    }

    private void exercise() {
        Question q = learning.current();
        String mode = learning.mode() == LearningService.Mode.QUIZ ? "Mixed quiz" : learning.mode() == LearningService.Mode.REVIEW ? "Mistake review" : "Topic practice";
        String count = learning.questionCount() == 0 ? "Question " + learning.questionNumber()
                : "Question " + learning.questionNumber() + " of " + learning.questionCount();
        JPanel view = page(mode + " / " + count, "Take it one step at a time.", "Think it through. You can answer with a number, a decimal, or a fraction.");
        if (learning.questionCount() > 0) {
            JProgressBar bar = new JProgressBar(0, learning.questionCount()); bar.setValue(learning.summary().answered());
            bar.setForeground(Theme.GREEN); bar.setBackground(Theme.LINE); bar.setBorderPainted(false);
            bar.setPreferredSize(new Dimension(500, 6)); add(view, bar); gap(view, 16);
        }
        JPanel problem = Theme.card(Color.WHITE, 28); problem.setLayout(new BorderLayout(0, 20));
        JPanel body = vertical();
        add(body, Theme.label(q.topic().title().toUpperCase() + "  /  " + q.difficulty(), 11, true, Theme.GREEN)); gap(body, 22);
        JTextArea prompt = Theme.text(q.prompt(), 27, Theme.INK, q.prompt().length() > 55 ? 3 : 2); prompt.setName("question-prompt"); add(body, prompt); gap(body, 14);
        add(body, Theme.text("Enter just the value. Use an exact fraction or round a decimal to 2 places. Decimal commas work too.", 13, Theme.MUTED, 2)); gap(body, 18);
        JLabel answerLabel = Theme.label("Your answer", 14, true, Theme.INK); add(body, answerLabel); gap(body, 8);
        JTextField answer = new JTextField(); answer.setName("answer"); answer.setFont(Theme.font(23, false));
        answer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.LINE, 2), BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        answer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55)); answerLabel.setLabelFor(answer);
        answer.getAccessibleContext().setAccessibleName("Your answer");
        answer.getAccessibleContext().setAccessibleDescription("Enter a number or fraction, without units. Press Enter to check your answer.");
        add(body, answer); gap(body, 10);
        JLabel error = Theme.label(" ", 13, false, Theme.RED); error.setName("validation-error"); add(body, error); gap(body, 10);
        JTextArea hintText = Theme.text("", 14, Theme.GREEN, 2); hintText.setName("hint-text"); hintText.setVisible(false);
        JPanel actions = flow();
        JPanel feedback = vertical(); feedback.setName("feedback");
        JButton[] controls = new JButton[3];
        Runnable grade = () -> {
            try {
                lastFeedback = learning.submit(answer.getText()); persist();
                renderFeedback(feedback, lastFeedback); answer.setEnabled(false);
                for (JButton control : controls) if (control != null) control.setEnabled(false);
                error.setText(" "); view.revalidate();
                SwingUtilities.invokeLater(() -> feedback.scrollRectToVisible(new Rectangle(0, 0, feedback.getWidth(), feedback.getPreferredSize().height)));
            } catch (IllegalArgumentException invalid) { error.setText(invalid.getMessage()); answer.requestFocusInWindow(); }
        };
        controls[0] = named(Theme.button("Check answer  →", true, grade), "check-answer"); answer.addActionListener(e -> grade.run()); actions.add(controls[0]);
        if (learning.mode() != LearningService.Mode.QUIZ) {
            controls[1] = named(Theme.button("Give me a hint", false, () -> {
                hintText.setText(learning.hint()); hintText.setVisible(true); view.revalidate();
            }), "hint"); actions.add(controls[1]);
        }
        controls[2] = named(Theme.button(learning.mode() == LearningService.Mode.QUIZ ? "Skip question" : "Show solution", false, () -> {
            lastFeedback = learning.reveal(); persist(); renderFeedback(feedback, lastFeedback);
            answer.setEnabled(false); for (JButton control : controls) if (control != null) control.setEnabled(false);
            error.setText(" "); view.revalidate();
            SwingUtilities.invokeLater(() -> feedback.scrollRectToVisible(new Rectangle(0, 0, feedback.getWidth(), feedback.getPreferredSize().height)));
        }), "reveal"); actions.add(controls[2]);
        add(body, actions); gap(body, 12); add(body, hintText);
        problem.add(body); add(view, problem); gap(view, 17); add(view, feedback); gap(view, 17);
        JPanel bottom = flow(); bottom.add(Theme.button("Choose another topic", false, this::practicePicker));
        add(view, bottom);
        if (learning.answered()) {
            answer.setEnabled(false); for (JButton control : controls) if (control != null) control.setEnabled(false);
            if (lastFeedback != null) renderFeedback(feedback, lastFeedback);
        }
        display(view); SwingUtilities.invokeLater(answer::requestFocusInWindow);
    }

    private void renderFeedback(JPanel container, LearningService.Feedback feedback) {
        container.removeAll();
        JPanel card = Theme.card(feedback.correct() ? Theme.MINT : Theme.WARM, 24); card.setLayout(new BorderLayout(0, 14));
        JPanel copy = vertical();
        String title = feedback.correct() ? "You got it!  +" + feedback.points() + " points"
                : feedback.revealed() ? "Let's understand the solution." : "Good try. Let's work through it.";
        add(copy, Theme.label(title, 21, true, Theme.INK)); gap(copy, 9);
        if (feedback.hinted()) { add(copy, Theme.label("You used a hint. Keep practicing to solve it independently.", 12, false, Theme.MUTED)); gap(copy, 8); }
        add(copy, Theme.label("Answer: " + feedback.question().answerLabel(), 18, true, Theme.GREEN)); gap(copy, 12);
        for (int i = 0; i < feedback.question().steps().size(); i++) {
            add(copy, Theme.text((i + 1) + ".  " + feedback.question().steps().get(i), 15, Theme.INK, 2)); gap(copy, 3);
        }
        if (!feedback.correct()) { add(copy, Theme.label("Saved to mistake review. Try it again when you are ready.", 12, false, Theme.MUTED)); gap(copy, 12); }
        boolean last = learning.questionCount() > 0 && learning.questionNumber() == learning.questionCount();
        add(copy, named(Theme.button(last ? "See results  →" : "Next question  →", true, () -> {
            if (learning.next()) { lastFeedback = null; exercise(); } else results();
        }), "next"));
        card.add(copy); add(container, card);
        container.setMaximumSize(new Dimension(Integer.MAX_VALUE, container.getPreferredSize().height));
        container.revalidate(); container.repaint();
    }

    private void review() {
        Progress.Snapshot s = progress.snapshot();
        JPanel view = page("Mistake review", s.mistakes().isEmpty() ? "A fresh start. A clear path." : "Turn a mistake into a breakthrough.",
                "Questions stay here until you solve them without a hint. You can revisit the lesson first.");
        if (s.mistakes().isEmpty()) {
            JPanel empty = Theme.card(Theme.MINT, 30); empty.setLayout(new BorderLayout(0, 20));
            empty.add(Theme.text("Your review queue is clear. Practice a topic or take the mixed quiz to discover what to focus on next.", 20, Theme.INK, 3));
            empty.add(Theme.button("Start practicing  →", true, this::practicePicker), BorderLayout.SOUTH); add(view, empty);
        } else {
            add(view, named(Theme.button("Review " + s.mistakes().size() + " questions  →", true, () -> {
                if (learning.startReview()) { lastFeedback = null; activeScreen = "practice";
                    navigation.forEach((k, b) -> b.setEnabled(!k.equals("practice"))); exercise(); }
            }), "start-review")); gap(view, 22);
            for (Question q : s.mistakes()) {
                JPanel p = Theme.card(Color.WHITE, 18); p.setLayout(new BorderLayout(10, 10));
                p.add(Theme.label(q.topic().title() + " / " + q.difficulty(), 12, true, Theme.GREEN), BorderLayout.NORTH);
                p.add(Theme.text(q.prompt(), 17, Theme.INK, 3), BorderLayout.CENTER);
                p.add(Theme.button("Read lesson", false, () -> lesson(q.topic())), BorderLayout.EAST);
                add(view, p); gap(view, 10);
            }
        }
        display(view);
    }

    private void results() {
        LearningService.Summary result = learning.summary();
        JPanel view = page("Session complete", result.correct() == result.answered() ? "Look at you go." : "Progress comes from showing up.",
                "You completed the session. Your answers and review questions are ready. Check the save status below.");
        JPanel metrics = new JPanel(new GridLayout(1, 3, 16, 0)); metrics.setOpaque(false);
        metrics.add(metric("Correct answers", result.correct() + " / " + result.answered(), "One step closer to understanding."));
        metrics.add(metric("Session accuracy", Math.round(100.0 * result.correct() / result.answered()) + "%", "Review the tricky ones next."));
        metrics.add(metric("Points earned", String.valueOf(result.points()), "Your effort adds up."));
        add(view, metrics); gap(view, 24);
        JPanel tip = Theme.card(Theme.MINT, 27); tip.setLayout(new BorderLayout(0, 20));
        tip.add(Theme.text(progress.snapshot().mistakes().isEmpty() ? "Your mistake queue is clear. Try the next difficulty or explore another topic."
                : "You have " + progress.snapshot().mistakes().size() + " questions in your review queue. Work through them without hints to clear them.", 20, Theme.INK, 3));
        JPanel actions = flow(); actions.add(Theme.button("Your progress", true, () -> navigate("progress")));
        actions.add(Theme.button("Mistake review", false, () -> navigate("review"))); tip.add(actions, BorderLayout.SOUTH);
        add(view, tip); gap(view, 20); add(view, Theme.button("Back to overview", false, () -> navigate("home"))); display(view);
    }

    private void progressView() {
        Progress.Snapshot s = progress.snapshot();
        JPanel view = page("Your progress", "Small steps add up.", "See where you are growing and choose what to focus on next. Accuracy includes hinted answers; streaks count independent answers.");
        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0)); metrics.setOpaque(false);
        metrics.add(metric("Total answered", String.valueOf(s.answered()), "Lifetime practice and quiz attempts."));
        metrics.add(metric("Best streak", String.valueOf(s.bestStreak()), "Correct answers without hints."));
        metrics.add(metric("To revisit", String.valueOf(s.mistakes().size()), "Questions in your review queue."));
        add(view, metrics); gap(view, 26); add(view, Theme.label("Your topic breakdown", 21, true, Theme.INK)); gap(view, 13);
        for (Topic t : Topic.values()) {
            Progress.TopicStats stats = s.topics().get(t);
            JPanel p = Theme.card(Color.WHITE, 18); p.setLayout(new BorderLayout(16, 8));
            JPanel labels = Theme.plain(); labels.setLayout(new BorderLayout());
            labels.add(Theme.label(t.symbol() + "   " + t.title(), 16, true, Theme.INK), BorderLayout.WEST);
            labels.add(Theme.label(stats.answered() == 0 ? "Ready when you are" : stats.correct() + " / " + stats.answered() + " correct · " + stats.accuracy() + "%", 13, false, Theme.MUTED), BorderLayout.EAST);
            p.add(labels, BorderLayout.NORTH);
            JProgressBar bar = new JProgressBar(0, 100); bar.setValue(stats.accuracy()); bar.setForeground(Theme.GREEN); bar.setBackground(Theme.LINE); bar.setBorderPainted(false);
            bar.getAccessibleContext().setAccessibleName(t.title() + " accuracy"); p.add(bar, BorderLayout.CENTER);
            add(view, p); gap(view, 12);
        }
        gap(view, 14); add(view, Theme.label("Recent mixed quizzes", 21, true, Theme.INK)); gap(view, 13);
        if (s.quizzes().isEmpty()) add(view, Theme.text("Your first quiz result will appear here. A quiz has two questions from each topic.", 15, Theme.MUTED, 2));
        else for (int i = s.quizzes().size() - 1; i >= 0; i--) {
            var q = s.quizzes().get(i);
            JPanel row = Theme.card(Color.WHITE, 16); row.setLayout(new BorderLayout());
            String date = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault()).format(q.completedAt());
            row.add(Theme.label(date + "  ·  " + q.difficulty(), 14, false, Theme.INK), BorderLayout.WEST);
            row.add(Theme.label(q.correct() + " / " + q.total(), 17, true, Theme.GREEN), BorderLayout.EAST); add(view, row); gap(view, 8);
        }
        display(view);
    }

    private void persist() {
        Progress.Snapshot snapshot = progress.snapshot();
        status.setText("Saving your progress…"); status.setForeground(Theme.MUTED);
        saves.execute(() -> {
            try {
                repository.save(snapshot);
                SwingUtilities.invokeLater(() -> { status.setText("Progress saved • Your learning stays on this device."); status.setForeground(Theme.MUTED); });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> { status.setText("Progress could not be saved. Check folder permissions. This session is still in memory.");
                    status.setForeground(Theme.RED); status.setToolTipText(e.getMessage()); });
            }
        });
    }

    /** Allows tests and window shutdown to wait for the serialized save queue without blocking Swing. */
    public CompletableFuture<Void> flushSaves() {
        CompletableFuture<Void> flushed = new CompletableFuture<>(); saves.execute(() -> flushed.complete(null)); return flushed;
    }
    public void finishSaving(Runnable after) { saves.execute(() -> SwingUtilities.invokeLater(after)); saves.shutdown(); }
    @Override public void close() { saves.shutdown(); }
    public String activeScreen() { return activeScreen; }

    private static final class ScrollPage extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        ScrollPage() { setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(24, visible.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static JPanel vertical() { JPanel p = Theme.plain(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p; }
    private static JPanel flow() { JPanel p = Theme.plain(); p.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0)); return p; }
    private static void add(JPanel parent, JComponent child) {
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension pref = child.getPreferredSize(); child.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height)); parent.add(child);
    }
    private static void gap(JPanel p, int height) { p.add(Box.createVerticalStrut(height)); }
    private static <T extends JComponent> T named(T component, String name) { component.setName(name); return component; }
}
