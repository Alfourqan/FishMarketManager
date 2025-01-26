package com.poissonnerie.view;

import com.poissonnerie.model.Produit;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationStockView extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(NotificationStockView.class.getName());
    private final DefaultListModel<String> listModel;
    private final JList<String> notificationList;
    private static NotificationStockView instance;
    private final JLabel titleLabel;
    private int notificationCount = 0;

    private NotificationStockView(Frame owner) {
        super(owner, "Alertes de Stock", false);
        setSize(400, 300);
        setLocationRelativeTo(owner);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(255, 255, 255));

        // Titre avec compteur
        titleLabel = new JLabel("Alertes de Stock (0)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Liste des notifications
        listModel = new DefaultListModel<>();
        notificationList = new JList<>(listModel);
        notificationList.setCellRenderer(new NotificationCellRenderer());
        JScrollPane scrollPane = new JScrollPane(notificationList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(224, 224, 224)));

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        JButton clearButton = new JButton("Effacer tout");
        styleButton(clearButton, new Color(244, 67, 54));
        clearButton.addActionListener(e -> clearNotifications());

        JButton closeButton = new JButton("Fermer");
        styleButton(closeButton, new Color(97, 97, 97));
        closeButton.addActionListener(e -> setVisible(false));

        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);

        // Assemblage
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
    }

    public static NotificationStockView getInstance(Frame owner) {
        if (instance == null) {
            instance = new NotificationStockView(owner);
        }
        return instance;
    }

    public void addNotification(Produit produit) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String message;

            if (produit.getStock() == 0) {
                message = String.format("[%s] ⛔ RUPTURE: %s (Stock: 0)", 
                    timestamp, produit.getNom());
            } else {
                message = String.format("[%s] ⚠️ STOCK BAS: %s (Stock: %d, Seuil: %d)", 
                    timestamp, produit.getNom(), produit.getStock(), produit.getSeuilAlerte());
            }

            listModel.add(0, message);
            notificationCount++;
            updateTitle();

            // Afficher une notification système
            displaySystemNotification(produit);
        });
    }

    private void displaySystemNotification(Produit produit) {
        try {
            String message = produit.getStock() == 0 ? 
                "Rupture de stock pour " + produit.getNom() :
                "Stock bas pour " + produit.getNom() + " (" + produit.getStock() + " restants)";

            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            java.awt.Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/icons/warning.png"));
            TrayIcon trayIcon = new TrayIcon(image, "Alerte Stock");
            trayIcon.setImageAutoSize(true);

            if (!Arrays.asList(tray.getTrayIcons()).contains(trayIcon)) {
                tray.add(trayIcon);
            }

            trayIcon.displayMessage(
                "Alerte Stock",
                message,
                TrayIcon.MessageType.WARNING
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible d'afficher la notification système", e);
        }
    }

    private void clearNotifications() {
        listModel.clear();
        notificationCount = 0;
        updateTitle();
    }

    private void updateTitle() {
        titleLabel.setText("Alertes de Stock (" + notificationCount + ")");
    }

    private class NotificationCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

            // Style
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (!isSelected) {
                label.setBackground(index % 2 == 0 ? 
                    new Color(250, 250, 250) : Color.WHITE);
                label.setForeground(new Color(33, 33, 33));
            }

            return label;
        }
    }
}