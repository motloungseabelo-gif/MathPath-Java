package com.mathpath;

import com.mathpath.core.Difficulty;
import com.mathpath.core.LearningService;
import com.mathpath.core.Progress;
import com.mathpath.core.QuestionGenerator;
import com.mathpath.core.Topic;
import com.mathpath.storage.ProgressRepository;
import com.mathpath.ui.MathPathPanel;
import com.mathpath.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.mathpath.Checks.equal;
import static com.mathpath.Checks.that;

/** Drives real Swing controls on the EDT and renders the actual application, without a display server. */
public final class UiTest {
    private UiTest() { }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("mathpath-ui-");
        Progress p = new Progress(); ProgressRepository repository = new ProgressRepository(dir);
        LearningService learning = new LearningService(new QuestionGenerator(new Random(91)), p);
        MathPathPanel[] panelRef = new MathPathPanel[1];
        try {
            edt(() -> { Theme.install(); panelRef[0] = new MathPathPanel(p, repository, learning, ""); });
            MathPathPanel panel = panelRef[0];
            edt(() -> {
                render(panel, "build/screenshots/overview.png", 1240, 990);
                render(panel, "build/screenshots/overview-minimum.png", 1040, 700);
                click(panel, "nav-learn");
                for (Topic topic : Topic.values()) { click(panel, "learn-" + topic); that(find(panel, "lesson-practice") != null, "Lesson action missing"); panel.navigate("learn"); }
                click(panel, "learn-FRACTIONS"); render(panel, "build/screenshots/lesson.png", 1240, 930);
                render(panel, "build/screenshots/lesson-minimum.png", 1040, 700);
                click(panel, "lesson-practice");
                equal(Topic.FRACTIONS, learning.current().topic());
                render(panel, "build/screenshots/practice.png", 1240, 850);
                JTextField answer = (JTextField) find(panel, "answer");
                equal("Your answer", answer.getAccessibleContext().getAccessibleName());
                answer.setText("oops"); click(panel, "check-answer");
                equal(0L, p.snapshot().answered());
                that(!((JLabel) find(panel, "validation-error")).getText().isBlank(), "Validation feedback missing");
                answer.setText("1/0"); click(panel, "check-answer"); equal(0L, p.snapshot().answered());
                click(panel, "hint"); answer.setText(learning.current().answer().toString()); answer.postActionEvent();
                equal(1L, p.snapshot().correct()); that(!((JButton) find(panel, "check-answer")).isEnabled(), "Submit was not disabled");
                that(find(panel, "next") != null, "Next action missing after feedback");
                render(panel, "build/screenshots/feedback.png", 1240, 850);
                panel.navigate("home"); panel.navigate("practice");
                that(find(panel, "next") != null, "Completed question lost its next action after navigation");
                click(panel, "next"); click(panel, "reveal"); equal(1, p.snapshot().mistakes().size());
                panel.navigate("review"); click(panel, "start-review");
                answer = (JTextField) find(panel, "answer"); answer.setText(learning.current().answer().toString()); click(panel, "check-answer");
                equal(0, p.snapshot().mistakes().size()); click(panel, "next");
                that(learning.finished(), "Review session did not finish");
                panel.navigate("home"); click(panel, "start-quiz");
                that(find(panel, "hint") == null, "Quiz offered a hint");
                for (int i = 0; i < 10; i++) {
                    ((JTextField) find(panel, "answer")).setText(learning.current().answer().toString());
                    click(panel, "check-answer"); click(panel, "next");
                }
                equal(1, p.snapshot().quizzes().size()); equal(10, p.snapshot().quizzes().get(0).correct());
                panel.navigate("progress"); render(panel, "build/screenshots/progress.png", 1240, 990);
                render(panel, "build/screenshots/progress-minimum.png", 1040, 700);
                panel.navigate("practice");
                @SuppressWarnings("unchecked") JComboBox<Topic> topic = (JComboBox<Topic>) find(panel, "select-topic");
                topic.setSelectedItem(Topic.GEOMETRY);
                ((JComboBox<?>) find(panel, "select-difficulty")).setSelectedIndex(3);
                click(panel, "start-practice"); equal(Topic.GEOMETRY, learning.current().topic()); equal(Difficulty.CHALLENGE, learning.current().difficulty());
            });
            panel.flushSaves().get(10, TimeUnit.SECONDS);
            edt(() -> that(((JLabel) find(panel, "storage-status")).getText().startsWith("Progress saved"), "Save confirmation missing"));
            equal(p.snapshot(), repository.load().progress().snapshot());
            System.out.println("PASS Swing flows: five lessons, input validation, Enter submission, hints, navigation, review, ten-question quiz, settings and saved progress.");
            System.out.println("PASS Actual Swing screens rendered at 1240×850 and 1040×700.");
        } finally {
            if (panelRef[0] != null) { panelRef[0].flushSaves().get(10, TimeUnit.SECONDS); panelRef[0].close(); }
            try (var paths = Files.walk(dir)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
    }

    static Component find(Component root, String name) {
        if (name.equals(root.getName())) return root;
        if (root instanceof Container container) for (Component child : container.getComponents()) {
            Component found = find(child, name); if (found != null) return found;
        }
        return null;
    }
    static void click(Component root, String name) {
        Component found = find(root, name); that(found instanceof JButton, "Button not found: " + name);
        JButton button = (JButton) found; that(button.isEnabled(), "Button disabled: " + name); button.doClick(0);
    }
    static void edt(Checks.Throwing task) throws Exception {
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> { try { task.run(); } catch (Throwable e) { failure[0] = e; } });
        if (failure[0] != null) throw new AssertionError("Swing verification failed", failure[0]);
    }
    static void layout(Container parent) { parent.doLayout(); for (Component child : parent.getComponents()) if (child instanceof Container c) layout(c); }
    static void render(MathPathPanel panel, String file, int width, int height) throws Exception {
        panel.setSize(width, height); for (int i = 0; i < 3; i++) layout(panel);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); panel.printAll(graphics); graphics.dispose();
        Path path = Path.of(file); Files.createDirectories(path.getParent()); ImageIO.write(image, "png", path.toFile());
    }
}
