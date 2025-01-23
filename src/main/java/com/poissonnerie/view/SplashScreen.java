package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;

public class SplashScreen extends JWindow {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 350;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private float fishAnimation = 0.0f;
    private Timer animationTimer;
    private final JPanel mainPanel;
    private float rippleAnimation = 0.0f;
    private Color primaryColor = new Color(0, 92, 151);
    private Color secondaryColor = new Color(0, 126, 167);

    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        // Créer et configurer le panel principal avec un effet de fond amélioré
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Fond avec dégradé dynamique
                GradientPaint gradient = new GradientPaint(
                    0, 0, primaryColor,
                    getWidth(), getHeight(), secondaryColor
                );
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

                // Effet d'ondulation amélioré
                drawWaterEffect(g2d);

                // Titre principal avec effet de lueur dynamique
                drawGlowingText(g2d, "Gestion Poissonnerie",
                    new Font("Segoe UI", Font.BOLD, 36),
                    getWidth()/2, 80);

                // Sous-titre avec animation de fade
                float alpha = (float)(Math.sin(rippleAnimation * 0.5) * 0.2 + 0.8);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                drawGlowingText(g2d, "Application de gestion commerciale",
                    new Font("Segoe UI", Font.PLAIN, 18),
                    getWidth()/2, 120);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                // Logo animé amélioré
                drawAnimatedFishLogo(g2d, getWidth()/2, 180);

                g2d.dispose();
            }
        };
        mainPanel.setOpaque(false);

        // Barre de progression personnalisée avec animation fluide
        progressBar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fond de la barre avec effet de transparence
                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                // Animation de la barre de progression
                if (getValue() > 0) {
                    // Calcul de la largeur avec effet de lissage
                    float progress = getValue() / 100f;
                    int fillWidth = (int)((getWidth() - 4) * progress);

                    // Dégradé pour la barre de progression
                    GradientPaint progressGradient = new GradientPaint(
                        2, 0, new Color(255, 255, 255, 240),
                        fillWidth, getHeight(), new Color(255, 255, 255, 180)
                    );
                    g2d.setPaint(progressGradient);
                    g2d.fillRoundRect(2, 2, fillWidth, getHeight() - 4, getHeight() - 4, getHeight() - 4);

                    // Effet de brillance
                    g2d.setColor(new Color(255, 255, 255, 60));
                    g2d.fillRoundRect(2, 2, fillWidth, (getHeight() - 4) / 2, 
                                    (getHeight() - 4) / 2, (getHeight() - 4) / 2);
                }

                g2d.dispose();
            }
        };
        progressBar.setOpaque(false);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(WIDTH - 60, 8));

        // Label de statut amélioré
        statusLabel = new JLabel("Démarrage...", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(255, 255, 255, 220));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Configuration du panneau principal
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        // Panel pour la barre de progression et le statut avec espacement optimisé
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 30, 30));
        bottomPanel.setOpaque(false);
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Timer d'animation avec intervalle optimisé
        animationTimer = new Timer(16, e -> {
            fishAnimation += 0.08f;
            rippleAnimation += 0.04f;
            if (fishAnimation > 2 * Math.PI) {
                fishAnimation = 0;
            }
            mainPanel.repaint();
        });
        animationTimer.start();

        // Forme arrondie de la fenêtre
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 20, 20));
    }

    private void drawWaterEffect(Graphics2D g2d) {
        int rows = 5;
        int cols = 8;
        int cellWidth = getWidth() / cols;
        int cellHeight = getHeight() / rows;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double x = j * cellWidth;
                double y = i * cellHeight;
                double offset = Math.sin(x/100.0 + rippleAnimation) * 
                              Math.cos(y/100.0 + rippleAnimation) * 5;

                // Calcul sécurisé des composantes de couleur
                int red = Math.min(255, Math.max(0, primaryColor.getRed() + (int)(offset * 3)));
                int green = Math.min(255, Math.max(0, primaryColor.getGreen() + (int)(offset * 3)));
                int blue = Math.min(255, Math.max(0, primaryColor.getBlue() + (int)(offset * 3)));

                Color rippleColor = new Color(red, green, blue, 50);
                g2d.setColor(rippleColor);
                g2d.fillRect((int)x, (int)y, cellWidth + 1, cellHeight + 1);
            }
        }
    }

    private void drawGlowingText(Graphics2D g2d, String text, Font font, int x, int y) {
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = x - textWidth/2;

        // Effet de lueur amélioré
        float glowIntensity = (float)(Math.sin(rippleAnimation * 0.5) * 0.2 + 0.8);
        for (int i = 5; i > 0; i--) {
            float alpha = glowIntensity * (0.5f - i * 0.1f);
            g2d.setColor(new Color(1f, 1f, 1f, alpha));

            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                int offsetX = (int)(Math.cos(rad) * i);
                int offsetY = (int)(Math.sin(rad) * i);
                g2d.drawString(text, textX + offsetX, y + offsetY);
            }
        }

        // Texte principal avec ombre portée douce
        g2d.setColor(new Color(0, 0, 0, 0.3f));
        g2d.drawString(text, textX + 2, y + 2);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, y);
    }

    private void drawAnimatedFishLogo(Graphics2D g2d, int centerX, int centerY) {
        int size = 120;
        float yOffset = (float)(Math.sin(fishAnimation) * 10);
        float xOffset = (float)(Math.cos(fishAnimation * 0.5) * 5);
        centerY += yOffset;
        centerX += xOffset;

        // Dessin du poisson avec effet de transparence
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(new Color(255, 255, 255, 220));

        // Corps du poisson avec courbes améliorées
        Path2D fishPath = new Path2D.Float();
        fishPath.moveTo(centerX - size/2, centerY);
        fishPath.curveTo(
            centerX - size/3, centerY - size/3,
            centerX + size/3, centerY - size/3,
            centerX + size/2, centerY
        );
        fishPath.curveTo(
            centerX + size/3, centerY + size/3,
            centerX - size/3, centerY + size/3,
            centerX - size/2, centerY
        );
        g2d.draw(fishPath);

        // Queue avec animation fluide
        float tailWag = (float)Math.sin(fishAnimation * 2) * 15;
        int[] xPoints = {
            centerX + size/2,
            centerX + size - 10,
            centerX + size/2
        };
        int[] yPoints = {
            centerY - size/6,
            centerY + (int)tailWag,
            centerY + size/6
        };
        g2d.drawPolyline(xPoints, yPoints, 3);

        // Œil avec effet de brillance
        g2d.fillOval(centerX - size/3, centerY - size/6, size/10, size/10);
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillOval(centerX - size/3 + 2, centerY - size/6 + 2, size/20, size/20);

        // Bulles d'eau animées
        drawBubbles(g2d, centerX, centerY, size);
    }

    private void drawBubbles(Graphics2D g2d, int fishX, int fishY, int fishSize) {
        int numBubbles = 3;
        float bubbleOffset = fishAnimation * 2;

        for (int i = 0; i < numBubbles; i++) {
            float individualOffset = (bubbleOffset + i * 2.0f) % 6.0f;
            int x = fishX - fishSize/2 - 20 - (i * 15);
            int y = (int)(fishY + Math.sin(individualOffset) * 10);
            int size = 8 - (i * 2);

            float alpha = Math.max(0, 1 - individualOffset/6.0f);
            g2d.setColor(new Color(1f, 1f, 1f, alpha));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(x, y, size, size);
        }
    }

    public void setProgress(int progress, String status) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
            statusLabel.setText(status);
            mainPanel.repaint();
            progressBar.repaint();
        });
    }

    @Override
    public void dispose() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        super.dispose();
    }
}