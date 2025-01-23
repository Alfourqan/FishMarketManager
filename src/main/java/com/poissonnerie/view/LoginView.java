package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import com.poissonnerie.controller.AuthenticationController;
import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface LoginSuccessListener {
    void onLoginSuccess();
}

public class LoginView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private final AuthenticationController authController;
    private final List<LoginSuccessListener> loginSuccessListeners;

    public LoginView() {
        authController = AuthenticationController.getInstance();
        loginSuccessListeners = new ArrayList<>();
        setTitle("Connexion - Application Poissonnerie");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        initializeComponents();
        pack();
        setLocationRelativeTo(null);
    }

    public void addLoginSuccessListener(LoginSuccessListener listener) {
        if (listener != null) {
            loginSuccessListeners.add(listener);
        }
    }

    public void removeLoginSuccessListener(LoginSuccessListener listener) {
        loginSuccessListeners.remove(listener);
    }

    private void fireLoginSuccess() {
        for (LoginSuccessListener listener : loginSuccessListeners) {
            listener.onLoginSuccess();
        }
    }

    private void initializeComponents() {
        // Panel principal avec un padding de 20px
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 245));

        // Logo ou titre
        JLabel titleLabel = new JLabel("GESTION POISSONNERIE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Icône utilisateur
        FontIcon userIcon = FontIcon.of(MaterialDesign.MDI_ACCOUNT_CIRCLE);
        userIcon.setIconSize(64);
        userIcon.setIconColor(new Color(76, 175, 80));
        JLabel iconLabel = new JLabel(userIcon);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Panel pour les champs de saisie
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Champs de saisie stylisés
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        styleTextField(usernameField, "Nom d'utilisateur");
        styleTextField(passwordField, "Mot de passe");

        // Ajout des composants avec GridBagLayout
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Nom d'utilisateur:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Mot de passe:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(passwordField, gbc);

        // Bouton de connexion stylisé
        loginButton = createStyledButton("Se connecter", new Color(76, 175, 80));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Label pour les messages d'état
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Ajout des composants au panel principal
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(iconLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(inputPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(statusLabel);

        // Ajout des écouteurs d'événements
        addEventListeners();

        // Finalisation
        setContentPane(mainPanel);
    }

    private void styleTextField(JTextField textField, String placeholder) {
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        textField.setPreferredSize(new Dimension(200, 35));
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 40));

        // Effet de survol
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void addEventListeners() {
        loginButton.addActionListener(e -> handleLogin());

        // Permet de se connecter en appuyant sur Entrée
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };

        usernameField.addKeyListener(enterKeyAdapter);
        passwordField.addKeyListener(enterKeyAdapter);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs");
            statusLabel.setForeground(Color.RED);
            return;
        }

        if (authController.authenticate(username, password)) {
            statusLabel.setText("Connexion réussie!");
            statusLabel.setForeground(new Color(76, 175, 80));

            // Ouvrir la fenêtre principale après un court délai
            Timer timer = new Timer(1000, e -> {
                dispose(); // Ferme la fenêtre de login
                fireLoginSuccess(); // Notifie les listeners
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            statusLabel.setText("Identifiants incorrects");
            statusLabel.setForeground(Color.RED);
            passwordField.setText("");
        }
    }
}