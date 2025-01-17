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

    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setBackground(new Color(0, 0, 0, 0));

        // Démarrer l'animation
        animationTimer = new Timer(50, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fishAnimation += 0.1f;
                if (fishAnimation > 2 * Math.PI) {
                    fishAnimation = 0;
                }
                repaint();
            }
        });
        animationTimer.start();

        // Panel principal avec fond personnalisé
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                // Activer l'antialiasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Fond avec dégradé animé
                float offset = (float)(Math.sin(fishAnimation) * 0.1f + 0.9f);
                Color startColor = new Color(0, 92, 151);
                Color endColor = new Color(
                    Math.min(255, (int)(0 * offset)),
                    Math.min(255, (int)(126 * offset)),
                    Math.min(255, (int)(167 * offset))
                );

                GradientPaint gradient = new GradientPaint(
                    0, 0, startColor,
                    getWidth(), getHeight(), endColor
                );
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

                // Effet de bordure lumineux animé
                float glowIntensity = (float)(Math.sin(fishAnimation) * 30 + 70);
                g2d.setStroke(new BasicStroke(2f));
                g2d.setColor(new Color(255, 255, 255, (int)glowIntensity));
                g2d.draw(new RoundRectangle2D.Double(1, 1, getWidth()-3, getHeight()-3, 19, 19));

                // Titre principal avec effet de lueur
                drawGlowingText(g2d, "Gestion Poissonnerie", 
                              new Font("Segoe UI", Font.BOLD, 36),
                              getWidth()/2, 80);

                // Sous-titre
                drawGlowingText(g2d, "Application de gestion commerciale",
                              new Font("Segoe UI", Font.PLAIN, 18),
                              getWidth()/2, 120);

                // Logo du poisson animé
                drawAnimatedFishLogo(g2d, getWidth()/2, 180);

                g2d.dispose();
            }

            private void drawGlowingText(Graphics2D g2d, String text, Font font, int x, int y) {
                g2d.setFont(font);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textX = x - textWidth/2;

                // Effet de lueur
                float glowIntensity = (float)(Math.sin(fishAnimation) * 0.3f + 0.7f);
                AlphaComposite alphaComposite = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, glowIntensity
                );
                g2d.setComposite(alphaComposite);

                // Plusieurs couches de lueur
                Color glowColor = new Color(255, 255, 255, 50);
                for (int i = 4; i > 0; i--) {
                    g2d.setColor(glowColor);
                    g2d.drawString(text, textX - i, y - i);
                    g2d.drawString(text, textX + i, y - i);
                    g2d.drawString(text, textX - i, y + i);
                    g2d.drawString(text, textX + i, y + i);
                }

                // Texte principal
                g2d.setComposite(AlphaComposite.SrcOver);
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, textX, y);
            }

            private void drawAnimatedFishLogo(Graphics2D g2d, int centerX, int centerY) {
                int size = 120;
                int x = centerX - size/2;

                // Animation de flottement
                float yOffset = (float)(Math.sin(fishAnimation) * 10);
                centerY += yOffset;

                // Configuration du rendu
                g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Effet de lueur
                float glowIntensity = (float)(Math.sin(fishAnimation) * 0.3f + 0.7f);
                Color glowColor = new Color(255, 255, 255, 40);
                g2d.setColor(glowColor);

                // Plusieurs couches de lueur
                for (int i = 3; i > 0; i--) {
                    drawFishShape(g2d, x + i, centerY + i, size);
                    drawFishShape(g2d, x - i, centerY + i, size);
                }

                // Poisson principal
                g2d.setColor(Color.WHITE);
                drawFishShape(g2d, x, centerY, size);

                // Bulles d'eau animées
                drawBubbles(g2d, x, centerY, size);
            }

            private void drawBubbles(Graphics2D g2d, int x, int y, int size) {
                float bubbleOffset = (float)(Math.cos(fishAnimation * 2) * 5);
                g2d.setColor(new Color(255, 255, 255, 100));

                // Dessiner plusieurs bulles
                for (int i = 0; i < 3; i++) {
                    int bubbleX = x + size/2 + (i * 15);
                    int bubbleY = y - size/4 + (int)(bubbleOffset * (i + 1));
                    int bubbleSize = 6 - i;
                    g2d.fillOval(bubbleX, bubbleY, bubbleSize, bubbleSize);
                }
            }

            private void drawFishShape(Graphics2D g2d, int x, int y, int size) {
                // Corps principal avec animation
                float bodyOffset = (float)(Math.sin(fishAnimation * 2) * 5);
                g2d.drawArc(x, y - size/3, size/2, size/2, 0, 180);
                g2d.drawArc(x + size/4 + (int)bodyOffset, y - size/3, size/2, size/2, 180, 180);

                // Queue animée
                int[] xPoints = {x + size/2 + size/4, x + size + (int)bodyOffset, x + size/2 + size/4};
                int[] yPoints = {y - size/6, y, y + size/6};
                g2d.drawPolyline(xPoints, yPoints, 3);

                // Œil avec effet de brillance
                g2d.fillOval(x + size/6, y - size/6, size/10, size/10);
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillOval(x + size/6 + 2, y - size/6 + 2, size/20, size/20);

                // Nageoire dorsale animée
                float finOffset = (float)(Math.cos(fishAnimation * 2) * 3);
                g2d.drawArc(x + size/4, y - size/3 - (int)finOffset, size/3, size/3, 0, 180);
            }
        };

        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BorderLayout());

        // Panel pour la barre de progression et le statut
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 30, 30));
        bottomPanel.setOpaque(false);

        // Barre de progression personnalisée
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 10));
        progressBar.setStringPainted(false);
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(255, 255, 255, 50));
        progressBar.setForeground(new Color(255, 255, 255, 220));

        // Label de statut avec style moderne
        statusLabel = new JLabel("Démarrage...");
        statusLabel.setForeground(new Color(255, 255, 255, 220));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);

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