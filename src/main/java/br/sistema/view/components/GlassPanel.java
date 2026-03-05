package br.sistema.view.components;

import javax.swing.*;
import java.awt.*;

public class GlassPanel extends JPanel {
    public GlassPanel() {
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = 25; int s = 12;
        g2.setColor(new Color(0, 0, 0, 5));
        g2.fillRoundRect(8, 8, getWidth() - s, getHeight() - s, arc, arc);
        g2.setColor(new Color(255, 255, 255, 240));
        g2.fillRoundRect(0, 0, getWidth() - s, getHeight() - s, arc, arc);
        g2.setColor(new Color(255, 255, 255, 255));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(0, 0, getWidth() - s, getHeight() - s, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}