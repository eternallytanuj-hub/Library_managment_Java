package com.library.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized application configuration reader.
 * Loads key-value pairs from {@code application.properties} (or test properties) on the classpath
 * and provides typed accessor methods with safe defaults.
 */
public final class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }

    private AppConfig() {
        // Utility class: prevent instantiation
    }

    public static synchronized void loadProperties() {
        // Check for test properties first, then default application.properties
        String[] candidateFiles = {"application-test.properties", "test-application.properties", "application.properties"};
        boolean loaded = false;
        for (String file : candidateFiles) {
            try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(file)) {
                if (is != null) {
                    PROPERTIES.load(is);
                    LOGGER.info("Successfully loaded configuration from classpath: " + file);
                    loaded = true;
                    break;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load properties from " + file, e);
            }
        }
        if (!loaded) {
            LOGGER.warning("No properties file found on classpath. Falling back to default settings.");
        }
    }

    public static synchronized void reload(String resourcePath) {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                PROPERTIES.clear();
                PROPERTIES.load(is);
                LOGGER.info("Successfully reloaded configuration from " + resourcePath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to reload properties from " + resourcePath, e);
        }
    }

    public static synchronized void setProperty(String key, String value) {
        if (value == null) {
            PROPERTIES.remove(key);
        } else {
            PROPERTIES.setProperty(key, value);
        }
    }

    public static String getProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static int getInt(String key, int defaultValue) {
        String val = getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid integer for property key ''{0}'': {1}. Using default {2}",
                    new Object[]{key, val, defaultValue});
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    public static double getDouble(String key, double defaultValue) {
        String val = getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid double for property key ''{0}'': {1}. Using default {2}",
                    new Object[]{key, val, defaultValue});
            return defaultValue;
        }
    }
}
