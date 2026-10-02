package com.mathpath.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Small Swing design system with native keyboard focus and real, accessible controls. */
public final class Theme {
    public static final Color BG = new Color(0xF5F6F2);
    public static final Color INK = new Color(0x172B28);
    public static final Color MUTED = new Color(0x5A6C67);
    public static final Color GREEN = new Color(0x166D50);
    public static final Color MINT = new Color(0xE2EDE1);
    public static final Color LINE = new Color(0xDDE4DC);
    public static final Color NAVY = new Color(0x162D2A);
    public static final Color WARM = new Color(0xFFF3D9);
    public static final Color RED = new Color(0x9F3D36);

    private Theme() { }
    public static Font font(int size, boolean bold) { return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size); }

    public static void install() {
        UIManager.put("Label.font", font(14, false));
        UIManager.put("Button.font", font(14, true));
        UIManager.put("ComboBox.font", font(14, false));
        UIManager.put("TextField.font", font(18, false));
        UIManager.put("ToolTip.font", font(13, false));
        UIManager.put("Panel.background", BG);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("ScrollBar.width", 10);
    }

    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold)); label.setForeground(color);
        return label;
    }

    public static JTextArea text(String text, int size, Color color, int rows) {
        JTextArea area = new JTextArea(text, rows, 10);
        area.setFont(font(size, false)); area.setForeground(color); area.setOpaque(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setEditable(false);
        area.setFocusable(false); area.setBorder(null);
        return area;
    }

    public static JPanel plain() { JPanel p = new JPanel(); p.setOpaque(false); return p; }
    public static JPanel card(Color color, int padding) {
        JPanel p = new RoundedPanel(color, 20);
        p.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return p;
    }

    public static JButton button(String text, boolean primary, Runnable action) {
        JButton button = new RoundedButton(text, primary ? GREEN : Color.WHITE, primary ? Color.WHITE : INK);
        button.addActionListener(e -> action.run());
        button.getAccessibleContext().setAccessibleDescription(text);
        return button;
    }

    public static final class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        private final int radius;
        public RoundedPanel(Color fill, int radius) { this.fill = fill; this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill); g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static final class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color fill, ink;
        private boolean hover;
        public RoundedButton(String text, Color fill, Color ink) {
            super(text); this.fill = fill; this.ink = ink;
            setFont(font(14, true)); setForeground(ink);
            setOpaque(false); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20)); setMargin(new Insets(0, 0, 0, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(!isEnabled() ? LINE : hover ? fill.darker() : fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            if (hasFocus()) { g.setStroke(new BasicStroke(2)); g.setColor(ink); g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 10, 10); }
            g.dispose(); super.paintComponent(graphics);
        }
    }
}
