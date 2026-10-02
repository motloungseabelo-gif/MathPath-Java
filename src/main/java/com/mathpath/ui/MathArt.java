package com.mathpath.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/** Original Java2D illustration: no network, fonts or image downloads needed. */
public final class MathArt extends JComponent {
    private static final long serialVersionUID = 1L;
    public MathArt() { setPreferredSize(new Dimension(255, 170)); setMinimumSize(new Dimension(150, 120)); }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(getWidth() / 255.0, getHeight() / 170.0);
        g.setStroke(new BasicStroke(1.3f));
        g.setColor(new Color(0xBECFBC));
        for (int x = 10; x < 255; x += 20) for (int y = 8; y < 170; y += 20) g.fillOval(x, y, 2, 2);
        g.setColor(new Color(0xA7BDA4)); g.drawOval(45, 10, 150, 150); g.drawOval(63, 28, 114, 114);
        g.setColor(Theme.NAVY); g.fillRoundRect(92, 46, 80, 80, 22, 22);
        g.setColor(new Color(0xD6EBC8)); g.setFont(Theme.font(37, true)); g.drawString("x²", 113, 97);
        AffineTransform original = g.getTransform();
        g.rotate(-0.16, 47, 44); g.setColor(Color.WHITE); g.fillRoundRect(17, 20, 62, 54, 13, 13);
        g.setColor(Theme.GREEN); g.setFont(Theme.font(28, true)); g.drawString("+", 37, 56);
        g.setTransform(original);
        g.setColor(new Color(0xEFBE67)); g.fillOval(181, 85, 56, 56);
        g.setColor(Theme.NAVY); g.setFont(Theme.font(27, true)); g.drawString("π", 198, 122);
        g.setColor(Theme.GREEN); g.fillOval(75, 140, 9, 9);
        g.drawLine(26, 118, 65, 118); g.drawLine(46, 98, 46, 138);
        g.dispose();
    }
}
