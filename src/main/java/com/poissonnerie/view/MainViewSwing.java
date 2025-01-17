package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.swing.FontIcon;
import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import com.poissonnerie.util.PDFGenerator;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

public class MainViewSwing {
    private final JPanel mainPanel;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final ClientController clientController;
    private final CaisseController caisseController;
    private JLabel titleLabel;
    private String currentTitle = "HOME";

    public MainViewSwing() {
        mainPanel = new JPanel(new BorderLayout());
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        venteController = new VenteController();
        produitController = new ProduitController();
        clientController = new ClientController();
        caisseController = new CaisseController();

        initializeComponents();
    }

    private void initializeComponents() {
        JPanel headerPanel = createHeader();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel navigationPanel = createNavigationPanel();
        mainPanel.add(navigationPanel, BorderLayout.WEST);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        addViews();
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(76, 175, 80));
        headerPanel.setPreferredSize(new Dimension(0, 50));

        titleLabel = new JLabel(currentTitle, SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        return headerPanel;
    }

    private void updateTitle(String newTitle) {
        currentTitle = newTitle;
        titleLabel.setText(newTitle);
    }

    private JPanel createNavigationPanel() {
        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBackground(new Color(33, 37, 41));
        navigationPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        navigationPanel.setPreferredSize(new Dimension(200, 0));

        String[] viewNames = {
            "Produits", "Ventes", "Clients", "Factures",
            "Fournisseurs", "Catégories", "Inventaire", "Caisse",
            "Report", "Réglages", "Déconnexion"
        };

        MaterialDesignI[] icons = {
            MaterialDesignI.PACKAGE_VARIANT_CLOSED_OUTLINE,
            MaterialDesignI.CART_OUTLINE,
            MaterialDesignI.ACCOUNT_GROUP_OUTLINE,
            MaterialDesignI.FILE_DOCUMENT_OUTLINE,
            MaterialDesignI.TRUCK_DELIVERY_OUTLINE,
            MaterialDesignI.TAG_MULTIPLE_OUTLINE,
            MaterialDesignI.CLIPBOARD_TEXT_OUTLINE,
            MaterialDesignI.CASH_MULTIPLE,
            MaterialDesignI.CHART_BAR_STACKED,
            MaterialDesignI.COG_OUTLINE,
            MaterialDesignI.LOGOUT_VARIANT
        };

        ButtonGroup buttonGroup = new ButtonGroup();
        navigationPanel.add(Box.createVerticalStrut(10));

        for (int i = 0; i < viewNames.length; i++) {
            JToggleButton navButton = createNavigationButton(viewNames[i], icons[i]);
            final String cardName = viewNames[i];

            if (i < viewNames.length - 1) {
                navButton.addActionListener(e -> {
                    cardLayout.show(contentPanel, cardName);
                    updateTitle(cardName.toUpperCase());
                });
            } else {
                navButton.addActionListener(e -> handleLogout());
            }

            buttonGroup.add(navButton);
            navigationPanel.add(navButton);
            navigationPanel.add(Box.createVerticalStrut(5));

            if (i == 0) {
                navButton.setSelected(true);
                updateTitle(viewNames[i].toUpperCase());
            }
        }

        return navigationPanel;
    }

    private JToggleButton createNavigationButton(String text, MaterialDesignI iconCode) {
        JToggleButton button = new JToggleButton(text);

        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);
        button.setIcon(icon);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(15);
        button.setMargin(new Insets(8, 15, 8, 15));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(170, 40));
        button.setMaximumSize(new Dimension(170, 40));
        button.setMinimumSize(new Dimension(170, 40));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(33, 37, 41));
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.isSelected()) {
                    button.setBackground(new Color(44, 49, 54));
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!button.isSelected()) {
                    button.setBackground(new Color(33, 37, 41));
                }
            }
        });

        button.addChangeListener(e -> {
            if (button.isSelected()) {
                button.setBackground(new Color(76, 175, 80));
            } else {
                button.setBackground(new Color(33, 37, 41));
            }
        });

        return button;
    }

    private void addViews() {
        contentPanel.add(new ProduitViewSwing().getMainPanel(), "Produits");
        contentPanel.add(new VenteViewSwing().getMainPanel(), "Ventes");
        contentPanel.add(new ClientViewSwing().getMainPanel(), "Clients");
        contentPanel.add(new CaisseViewSwing().getMainPanel(), "Caisse");
        contentPanel.add(new InventaireViewSwing().getMainPanel(), "Inventaire");
        contentPanel.add(new FournisseurViewSwing().getMainPanel(), "Fournisseurs");
        contentPanel.add(new ReportViewSwing().getMainPanel(), "Report");

        contentPanel.add(createTemporaryPanel("Factures"), "Factures");
        contentPanel.add(createTemporaryPanel("Catégories"), "Catégories");
        contentPanel.add(createTemporaryPanel("Réglages"), "Réglages");
    }

    private JPanel createTemporaryPanel(String name) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel("Vue " + name + " en construction", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void handleLogout() {
        int response = JOptionPane.showConfirmDialog(
            mainPanel,
            "Voulez-vous vraiment vous déconnecter ?",
            "Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            Window window = SwingUtilities.getWindowAncestor(mainPanel);
            if (window != null) {
                window.dispose();
            }
            System.exit(0);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}