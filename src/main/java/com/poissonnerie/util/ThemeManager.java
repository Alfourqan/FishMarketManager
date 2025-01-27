package com.poissonnerie.util;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {
    private static ThemeManager instance;
    private Map<String, Theme> themes;
    private Theme currentTheme;
    
    public static class Theme {
        private final Color primaryColor;
        private final Color secondaryColor;
        private final Color backgroundColor;
        private final Color textColor;
        private final Color accentColor;
        private final Font headerFont;
        private final Font bodyFont;
        
        public Theme(Color primaryColor, Color secondaryColor, Color backgroundColor, 
                    Color textColor, Color accentColor, Font headerFont, Font bodyFont) {
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.accentColor = accentColor;
            this.headerFont = headerFont;
            this.bodyFont = bodyFont;
        }
        
        // Getters
        public Color getPrimaryColor() { return primaryColor; }
        public Color getSecondaryColor() { return secondaryColor; }
        public Color getBackgroundColor() { return backgroundColor; }
        public Color getTextColor() { return textColor; }
        public Color getAccentColor() { return accentColor; }
        public Font getHeaderFont() { return headerFont; }
        public Font getBodyFont() { return bodyFont; }
    }
    
    private ThemeManager() {
        themes = new HashMap<>();
        initializeDefaultThemes();
    }
    
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    private void initializeDefaultThemes() {
        // Thème clair par défaut
        themes.put("LIGHT", new Theme(
            new Color(76, 175, 80),  // Primary (Green)
            new Color(33, 37, 41),   // Secondary (Dark gray)
            Color.WHITE,             // Background
            Color.BLACK,             // Text
            new Color(0, 150, 136),  // Accent
            new Font("Segoe UI", Font.BOLD, 18),
            new Font("Segoe UI", Font.PLAIN, 14)
        ));
        
        // Thème sombre
        themes.put("DARK", new Theme(
            new Color(76, 175, 80),  // Primary (Green)
            new Color(48, 48, 48),   // Secondary (Dark)
            new Color(33, 33, 33),   // Background
            Color.WHITE,             // Text
            new Color(0, 200, 183),  // Accent
            new Font("Segoe UI", Font.BOLD, 18),
            new Font("Segoe UI", Font.PLAIN, 14)
        ));
        
        // Thème bleu professionnel
        themes.put("PROFESSIONAL", new Theme(
            new Color(25, 118, 210), // Primary (Blue)
            new Color(66, 66, 66),   // Secondary
            new Color(245, 245, 245),// Background
            new Color(33, 33, 33),   // Text
            new Color(0, 150, 136),  // Accent
            new Font("Segoe UI", Font.BOLD, 18),
            new Font("Segoe UI", Font.PLAIN, 14)
        ));
        
        currentTheme = themes.get("LIGHT");
    }
    
    public void setTheme(String themeName) {
        Theme newTheme = themes.get(themeName.toUpperCase());
        if (newTheme != null) {
            currentTheme = newTheme;
        }
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    public void addCustomTheme(String name, Theme theme) {
        themes.put(name.toUpperCase(), theme);
    }
    
    public Map<String, Theme> getAvailableThemes() {
        return new HashMap<>(themes);
    }
}
