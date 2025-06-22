package com.chess.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SecureConfig {
    
    private static final String CONFIG_FILE = "chess.properties";
    private static final String DEFAULT_CONFIG_FILE = "chess.default.properties";
    private static SecureConfig instance;
    private final Properties properties;
    
    private SecureConfig() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    public static synchronized SecureConfig getInstance() {
        if (instance == null) {
            instance = new SecureConfig();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        // Try to load from user config file first
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream input = new FileInputStream(configPath.toFile())) {
                properties.load(input);
                System.out.println("Loaded configuration from " + CONFIG_FILE);
                return;
            } catch (IOException e) {
                System.err.println("Error loading config file: " + e.getMessage());
            }
        }
        
        // Fall back to default configuration from resources
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                System.out.println("Loaded default configuration");
            } else {
                // Set hardcoded defaults as last resort
                setDefaults();
                System.out.println("Using hardcoded default configuration");
            }
        } catch (IOException e) {
            System.err.println("Error loading default config: " + e.getMessage());
            setDefaults();
        }
    }
    
    private void setDefaults() {
        // Database configuration
        properties.setProperty("db.url", "jdbc:sqlite:chess_games.db");
        properties.setProperty("db.driver", "org.sqlite.JDBC");
        properties.setProperty("db.username", "");
        properties.setProperty("db.password", "");
        
        // Network configuration
        properties.setProperty("network.max.connections", "10");
        properties.setProperty("network.timeout", "30000");
        
        // Security configuration
        properties.setProperty("security.max.message.size", "1048576"); // 1MB
        properties.setProperty("security.enable.logging", "true");
        
        // Game configuration
        properties.setProperty("game.default.search.depth", "6");
        properties.setProperty("game.max.search.depth", "12");
    }
    
    public String getDatabaseUrl() {
        return properties.getProperty("db.url", "jdbc:sqlite:chess_games.db");
    }
    
    public String getDatabaseDriver() {
        return properties.getProperty("db.driver", "org.sqlite.JDBC");
    }
    
    public String getDatabaseUsername() {
        return properties.getProperty("db.username", "");
    }
    
    public String getDatabasePassword() {
        return properties.getProperty("db.password", "");
    }
    
    public int getMaxConnections() {
        return Integer.parseInt(properties.getProperty("network.max.connections", "10"));
    }
    
    public int getNetworkTimeout() {
        return Integer.parseInt(properties.getProperty("network.timeout", "30000"));
    }
    
    public long getMaxMessageSize() {
        return Long.parseLong(properties.getProperty("security.max.message.size", "1048576"));
    }
    
    public boolean isSecurityLoggingEnabled() {
        return Boolean.parseBoolean(properties.getProperty("security.enable.logging", "true"));
    }
    
    public int getDefaultSearchDepth() {
        return Integer.parseInt(properties.getProperty("game.default.search.depth", "6"));
    }
    
    public int getMaxSearchDepth() {
        return Integer.parseInt(properties.getProperty("game.max.search.depth", "12"));
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    // Validate file paths to prevent directory traversal
    public static String sanitizeFilePath(String filePath) {
        if (filePath == null) {
            throw new SecurityException("File path cannot be null");
        }
        
        // Remove dangerous path components
        String sanitized = filePath.replaceAll("\\.\\.", "")
                                  .replaceAll("[\\/\\\\]+", "/")
                                  .replaceAll("^/+", "");
        
        // Ensure path is within allowed directories
        if (sanitized.contains("..") || sanitized.startsWith("/") || sanitized.contains("\\")) {
            throw new SecurityException("Invalid file path: " + filePath);
        }
        
        return sanitized;
    }
    
    // Validate image file extensions
    public static boolean isValidImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".gif") || 
               lowerCase.endsWith(".png") || 
               lowerCase.endsWith(".jpg") || 
               lowerCase.endsWith(".jpeg");
    }
}
