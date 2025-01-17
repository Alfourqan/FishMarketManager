package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SplashScreen extends JWindow {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    public SplashScreen() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Panel principal avec fond personnalisé
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                
                // Activer l'antialiasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dégradé de fond
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(0, 135, 136), 
                    0, getHeight(), new Color(51, 51, 75)
                );
                g2d.setPaint(gradient);
                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

                // Dessiner le titre
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 32));
                g2d.setColor(Color.WHITE);
                String title = "Gestion Poissonnerie";
                FontMetrics fm = g2d.getFontMetrics();
                int titleX = (getWidth() - fm.stringWidth(title)) / 2;
                g2d.drawString(title, titleX, 150);

                // Dessiner une icône stylisée de poisson
                drawStylizedFish(g2d, getWidth() / 2 - 40, 200);

                g2d.dispose();
            }

            private void drawStylizedFish(Graphics2D g2d, int x, int y) {
                g2d.setStroke(new BasicStroke(3f));
                g2d.setColor(Color.WHITE);
                
                // Corps du poisson
                int[] xPoints = {x, x + 80, x + 60, x};
                int[] yPoints = {y, y + 20, y + 40, y};
                g2d.drawPolyline(xPoints, yPoints, 4);
                
                // Œil du poisson
                g2d.fillOval(x + 15, y + 15, 8, 8);
                
                // Nageoire
                g2d.drawLine(x + 40, y + 20, x + 50, y + 10);
                g2d.drawLine(x + 40, y + 20, x + 50, y + 30);
            }
        };
        mainPanel.setLayout(new BorderLayout());

        // Panel inférieur pour la barre de progression et le statut
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setOpaque(false);
        
        // Barre de progression
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(0, 135, 136));
        progressBar.setBackground(new Color(255, 255, 255, 100));
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        
        // Label de statut
        statusLabel = new JLabel("Démarrage...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);

        // Rendre la fenêtre arrondie
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 20, 20));
    }

    public void setProgress(int progress, String status) {
        progressBar.setValue(progress);
        statusLabel.setText(status);
    }
}
