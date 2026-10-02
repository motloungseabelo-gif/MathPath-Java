package com.mathpath;

import com.mathpath.core.LearningService;
import com.mathpath.core.Progress;
import com.mathpath.core.QuestionGenerator;
import com.mathpath.storage.ProgressRepository;
import com.mathpath.ui.MathPathPanel;
import com.mathpath.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/** Optional real-window smoke test using mouse input, keyboard input and the packaged JAR's classes. */
public final class DesktopSmokeTest {
    private DesktopSmokeTest() { }
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Use a desktop or xvfb-run to run this test.");
        Path dir = Files.createTempDirectory("mathpath-desktop-");
        Progress p = new Progress();
        LearningService service = new LearningService(new QuestionGenerator(new Random(81)), p);
        MathPathPanel[] panel = new MathPathPanel[1]; JFrame[] frame = new JFrame[1];
        try {
            UiTest.edt(() -> {
                Theme.install(); panel[0] = new MathPathPanel(p, new ProgressRepository(dir), service, "");
                frame[0] = new JFrame("MathPath desktop verification"); frame[0].setContentPane(panel[0]); frame[0].pack();
                frame[0].setLocation(20, 20); frame[0].setVisible(true);
            });
            Robot robot = new Robot(); robot.setAutoDelay(40); robot.waitForIdle();
            click(robot, panel[0], "practice-ARITHMETIC"); robot.waitForIdle();
            Point answer = location(panel[0], "answer"); robot.mouseMove(answer.x, answer.y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            String correct = service.current().answer().toString();
            for (char c : correct.toCharArray()) {
                int key = c == '-' ? KeyEvent.VK_MINUS : c == '/' ? KeyEvent.VK_SLASH : KeyEvent.VK_0 + c - '0';
                robot.keyPress(key); robot.keyRelease(key);
            }
            robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER); robot.waitForIdle();
            Checks.equal(1L, p.snapshot().correct());
            UiTest.edt(() -> Checks.that(!((JButton) UiTest.find(panel[0], "check-answer")).isEnabled(), "Duplicate grading not disabled"));
            panel[0].flushSaves().get(10, TimeUnit.SECONDS); robot.waitForIdle();
            Rectangle[] bounds = new Rectangle[1]; UiTest.edt(() -> bounds[0] = frame[0].getBounds());
            Path png = Path.of("build/screenshots/desktop.png"); Files.createDirectories(png.getParent());
            ImageIO.write(robot.createScreenCapture(bounds[0]), "png", png.toFile());
            UiTest.edt(() -> { frame[0].setSize(1040, 700); panel[0].navigate("progress"); }); robot.waitForIdle();
            UiTest.edt(() -> { panel[0].navigate("home"); }); robot.waitForIdle();
            System.out.println("PASS Real desktop: mouse navigation, typed answer, Enter submission, saved progress and window resize.");
        } finally {
            if (panel[0] != null) { panel[0].flushSaves().get(10, TimeUnit.SECONDS); panel[0].close(); }
            if (frame[0] != null) SwingUtilities.invokeAndWait(frame[0]::dispose);
            try (var paths = Files.walk(dir)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
    }
    private static void click(Robot robot, Component root, String name) throws Exception {
        Point point = location(root, name); robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
    private static Point location(Component root, String name) throws Exception {
        Point[] point = new Point[1]; UiTest.edt(() -> {
            Component c = UiTest.find(root, name); Checks.that(c != null && c.isShowing(), "Control not showing: " + name);
            Point at = c.getLocationOnScreen(); point[0] = new Point(at.x + c.getWidth() / 2, at.y + c.getHeight() / 2);
        }); return point[0];
    }
}
