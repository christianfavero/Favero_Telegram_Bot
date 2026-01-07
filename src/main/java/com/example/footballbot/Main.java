package com.example.footballbot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

public class Main {

    public static void main(String[] args) {
        // 1. Inizializza database
        DatabaseHelper.initDatabase();

        // 2. Mostra menu iniziale
        FootballBot bot = new FootballBot(Config.getBotToken());
        bot.start();

        // 3. Avvia Long Polling
        try (TelegramBotsLongPollingApplication botsApp = new TelegramBotsLongPollingApplication()) {
            botsApp.registerBot(Config.getBotToken(), bot);
            System.out.println("✅ Bot avviato correttamente!");

            // Mantieni il bot in vita
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
