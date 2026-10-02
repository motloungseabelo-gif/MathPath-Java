package com.mathpath;

import com.mathpath.core.LearningService;
import com.mathpath.core.QuestionGenerator;
import com.mathpath.storage.ProgressRepository;
import com.mathpath.ui.MathPathPanel;
import com.mathpath.ui.Theme;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Application entry point. */
public final class MathPath {
    private MathPath() { }
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("MathPath needs a graphical desktop. Run scripts/test.sh for headless verification.");
            System.exit(1);
        }
        ProgressRepository repository = ProgressRepository.userDefault();
        ProgressRepository.Loaded loaded = repository.load();
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            MathPathPanel panel = new MathPathPanel(loaded.progress(), repository,
                    new LearningService(new QuestionGenerator(), loaded.progress()), loaded.warning());
            JFrame frame = new JFrame("MathPath — Learn math. Build confidence.");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setContentPane(panel); frame.setMinimumSize(new Dimension(1040, 700)); frame.pack(); frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { frame.setEnabled(false); panel.finishSaving(frame::dispose); }
            });
            frame.setVisible(true);
        });
    }
}
