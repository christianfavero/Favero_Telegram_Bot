package com.example.footballbot;

import java.sql.*;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:footballbot.db";

    // Inizializza il database creando la tabella users
    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    chat_id INTEGER PRIMARY KEY,
                    username TEXT,
                    last_command TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            System.out.println("✅ Database inizializzato con successo!");

        } catch (SQLException e) {
            System.err.println("❌ Errore durante l'inizializzazione del database:");
            e.printStackTrace();
        }
    }

    // Aggiunge un utente se non esiste
    public static void addUser(long chatId, String username) {
        String sql = "INSERT OR IGNORE INTO users(chat_id, username) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, username);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Errore durante l'aggiunta dell'utente " + chatId);
            e.printStackTrace();
        }
    }

    // Aggiorna l'ultimo comando eseguito dall'utente
    public static void updateLastCommand(long chatId, String command) {
        String sql = "UPDATE users SET last_command = ? WHERE chat_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, command);
            stmt.setLong(2, chatId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Errore durante l'aggiornamento del comando per " + chatId);
            e.printStackTrace();
        }
    }

    // Conteggio totale utenti
    public static int getTotalUsers() {
        String sql = "SELECT COUNT(*) FROM users";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Errore durante il conteggio degli utenti:");
            e.printStackTrace();
        }

        return 0;
    }

    // Controlla se un utente esiste già
    public static boolean userExists(long chatId) {
        String sql = "SELECT 1 FROM users WHERE chat_id = ? LIMIT 1";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("❌ Errore durante la verifica dell'utente " + chatId);
            e.printStackTrace();
        }

        return false;
    }
}
