package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SplashScreen extends JWindow {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 350;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private float fishAnimation = 0.0f;
    private Timer animationTimer;
    private final JPanel mainPanel;

    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Créer et configurer le panel principal
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                // Activer l'antialiasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Fond avec dégradé
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(0, 92, 151),
                    getWidth(), getHeight(), new Color(0, 126, 167)
                );
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

                // Titre principal
                drawGlowingText(g2d, "Gestion Poissonnerie", 
                              new Font("Segoe UI", Font.BOLD, 36),
                              getWidth()/2, 80);

                // Sous-titre
                drawGlowingText(g2d, "Application de gestion commerciale",
                              new Font("Segoe UI", Font.PLAIN, 18),
                              getWidth()/2, 120);

                // Logo animé
                drawAnimatedFishLogo(g2d, getWidth()/2, 180);

                g2d.dispose();
            }

            private void drawGlowingText(Graphics2D g2d, String text, Font font, int x, int y) {
                g2d.setFont(font);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textX = x - textWidth/2;

                // Effet de lueur
                Color glowColor = new Color(255, 255, 255, 50);
                for (int i = 3; i > 0; i--) {
                    g2d.setColor(glowColor);
                    g2d.drawString(text, textX - i, y - i);
                    g2d.drawString(text, textX + i, y - i);
                    g2d.drawString(text, textX - i, y + i);
                    g2d.drawString(text, textX + i, y + i);
                }

                // Texte principal
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, textX, y);
            }

            private void drawAnimatedFishLogo(Graphics2D g2d, int centerX, int centerY) {
                int size = 120;
                float yOffset = (float)(Math.sin(fishAnimation) * 10);
                centerY += yOffset;

                g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(Color.WHITE);

                // Corps du poisson
                int x = centerX - size/2;
                g2d.drawArc(x, centerY - size/3, size/2, size/2, 0, 180);
                g2d.drawArc(x + size/4, centerY - size/3, size/2, size/2, 180, 180);

                // Queue
                int[] xPoints = {x + size/2 + size/4, x + size, x + size/2 + size/4};
                int[] yPoints = {centerY - size/6, centerY, centerY + size/6};
                g2d.drawPolyline(xPoints, yPoints, 3);

                // Œil
                g2d.fillOval(x + size/6, centerY - size/6, size/10, size/10);
            }
        };

        mainPanel.setOpaque(false);
        add(mainPanel);

        // Configuration de la barre de progression
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(WIDTH - 60, 10));
        progressBar.setBackground(new Color(255, 255, 255, 50));
        progressBar.setForeground(new Color(255, 255, 255, 220));
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);

        // Configuration du label de statut
        statusLabel = new JLabel("Démarrage...", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(255, 255, 255, 220));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Panel pour la barre de progression et le statut
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 30, 30));
        bottomPanel.setOpaque(false);
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Configuration de l'animation
        animationTimer = new Timer(50, e -> {
            fishAnimation += 0.1f;
            if (fishAnimation > 2 * Math.PI) {
                fishAnimation = 0;
            }
            repaint();
        });
        animationTimer.start();

        // Rendre la fenêtre arrondie
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 20, 20));
    }

    public void setProgress(int progress, String status) {
        progressBar.setValue(progress);
        statusLabel.setText(status);
    }

    @Override
    public void dispose() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        super.dispose();
    }
}