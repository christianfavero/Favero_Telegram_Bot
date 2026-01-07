package com.example.footballbot;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    private static final Properties props = new Properties();

    static {
        try (FileInputStream in = new FileInputStream("config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("❌ config.properties non trovato!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getApiKey() {
        return props.getProperty("API_KEY", "").trim();
    }

    public static String getBotToken() {
        return props.getProperty("BOT_TOKEN", "").trim();
    }
}
