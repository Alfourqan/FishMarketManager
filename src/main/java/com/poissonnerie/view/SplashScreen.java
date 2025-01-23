package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class SplashScreen extends JWindow {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 350;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private float fishAnimation = 0.0f;
    private Timer animationTimer;
    private final JPanel mainPanel;
    private BufferedImage buffer;
    private float rippleAnimation = 0.0f;
    private Color primaryColor = new Color(0, 92, 151);
    private Color secondaryColor = new Color(0, 126, 167);

    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Double buffering pour une animation plus fluide
        buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        // Créer et configurer le panel principal
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Fond avec dégradé amélioré
                GradientPaint gradient = new GradientPaint(
                    0, 0, primaryColor,
                    getWidth(), getHeight(), secondaryColor
                );
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

                // Effet d'ondulation dans le fond
                drawWaterEffect(g2d);

                // Titre principal avec effet de lueur amélioré
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

                // Création du panneau de progression personnalisé
                int barWidth = getWidth() - 60;
                int barHeight = 8;
                int x = 30;
                int y = getHeight() - barHeight - 30; // Positionnement de la barre en bas

                // Fond de la barre
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(x, y, barWidth, barHeight, barHeight, barHeight);

                // Barre de progression
                if (progressBar != null && progressBar.getValue() > 0) {
                    int fillWidth = (int)(barWidth * (progressBar.getValue() / 100.0));
                    g2d.setColor(new Color(255, 255, 255, 220));
                    g2d.fillRoundRect(x, y, fillWidth, barHeight, barHeight, barHeight);
                }

                g2d.dispose();
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
                        double offset = Math.sin(x/100.0 + rippleAnimation) * Math.cos(y/100.0 + rippleAnimation) * 5;

                        Color rippleColor = new Color(
                            Math.min(255, primaryColor.getRed() + (int)offset * 2),
                            Math.min(255, primaryColor.getGreen() + (int)offset * 2),
                            Math.min(255, primaryColor.getBlue() + (int)offset * 2),
                            50
                        );

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
                    Color glowColor = new Color(1f, 1f, 1f, alpha);
                    g2d.setColor(glowColor);

                    for (int angle = 0; angle < 360; angle += 45) {
                        double rad = Math.toRadians(angle);
                        int offsetX = (int)(Math.cos(rad) * i);
                        int offsetY = (int)(Math.sin(rad) * i);
                        g2d.drawString(text, textX + offsetX, y + offsetY);
                    }
                }

                // Texte principal avec ombre portée
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

                // Configuration du rendu
                g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(Color.WHITE);

                // Corps du poisson avec Path2D pour plus de fluidité
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

                // Queue animée
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

                // Œil avec effet brillant
                g2d.fillOval(centerX - size/3, centerY - size/6, size/10, size/10);
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillOval(centerX - size/3 + 2, centerY - size/6 + 2, size/20, size/20);

                // Bulles d'eau
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
        };

        mainPanel.setOpaque(false);
        add(mainPanel);

        // Initialisation de la barre de progression
        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false); // On cache la barre native, on utilise uniquement sa valeur

        // Configuration du label de statut
        statusLabel = new JLabel("Démarrage...", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(255, 255, 255, 220));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Panel pour la barre de progression et le statut
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 30, 30));
        bottomPanel.setOpaque(false);
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(new JPanel(), BorderLayout.SOUTH); // Placeholder to maintain layout

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Configuration de l'animation
        animationTimer = new Timer(16, e -> {
            fishAnimation += 0.1f;
            rippleAnimation += 0.05f;
            if (fishAnimation > 2 * Math.PI) {
                fishAnimation = 0;
            }
            mainPanel.repaint();
        });
        animationTimer.start();

        // Rendre la fenêtre arrondie
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 20, 20));
    }

    public void setProgress(int progress, String status) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
            statusLabel.setText(status);
            mainPanel.repaint();
        });
    }

    @Override
    public void dispose() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        if (buffer != null) {
            buffer.flush();
            buffer = null;
        }
        super.dispose();
    }
}