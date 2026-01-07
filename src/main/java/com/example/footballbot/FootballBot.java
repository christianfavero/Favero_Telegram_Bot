package com.example.footballbot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class FootballBot implements LongPollingSingleThreadUpdateConsumer {

    private final OkHttpTelegramClient telegramClient;

    public FootballBot(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    /** Menu iniziale */
    public void start() {
        String menu = """
                ⚽ *Bot Calcio Attivo!*
                Comandi disponibili:
                /start - Mostra questo menu
                /ping - Test connessione
                /nextmatch [league] - Prossima partita
                /topteams [league] - Classifica top squadre
                /relegation [league] - Squadre retrocesse
                /form [team] [league] - Ultimi risultati di una squadra
                /today [league] - Partite di oggi
                /last [league] - Ultima partita giocata
                /leagues - Lista leghe disponibili
                /league [name] - Info su una lega
                /team [name] [league] - Info su una squadra
                /stadium [name] - Info sullo stadio
                """;
        System.out.println(menu); // stampa in console
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String msgText = update.getMessage().getText().trim();
        String[] parts = msgText.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String param = parts.length > 1 ? parts[1] : null;

        switch (command) {
            case "/start" -> send(update, "⚽ *Bot pronto!* Usa i comandi per ottenere info sul calcio.\nUsa /start per il menu completo.");
            case "/ping" -> send(update, "🏓 *Pong!* Connessione attiva.");

            case "/nextmatch" -> {
                if (param == null) {
                    send(update, "❌ Specifica una lega: /nextmatch [league]");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(param);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + param);
                    break;
                }
                send(update, FootballApi.getNextMatch(leagueId));
            }

            case "/topteams" -> {
                if (param == null) {
                    send(update, "❌ Specifica una lega: /topteams [league]");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(param);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + param);
                    break;
                }
                send(update, FootballApi.getTopTeams(leagueId));
            }

            case "/relegation" -> {
                if (param == null) {
                    send(update, "❌ Specifica una lega: /relegation [league]");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(param);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + param);
                    break;
                }
                send(update, FootballApi.getRelegation(leagueId));
            }

            case "/form" -> {
                if (param == null) {
                    send(update, "❌ Specifica una squadra: /form [team] [league]");
                    break;
                }
                String[] teamParts = param.split("\\s+", 2);
                String teamName = teamParts[0];
                Integer leagueId = teamParts.length > 1 ? LeagueResolver.resolveLeagueId(teamParts[1]) : null;
                if (teamParts.length > 1 && leagueId == null) {
                    send(update, "❌ Lega non trovata: " + teamParts[1]);
                    break;
                }

                Integer teamId = TeamResolver.resolveTeamId(teamName, leagueId);
                if (teamId == null) {
                    send(update, "❌ Squadra non trovata: " + teamName);
                    break;
                }

                send(update, FootballApi.getForm(teamId));
            }

            case "/today" -> {
                if (param == null) {
                    send(update, "❌ Specifica una lega: /today [league]");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(param);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + param);
                    break;
                }
                send(update, FootballApi.getTodayMatches(leagueId));
            }

            case "/last" -> {
                if (param == null) {
                    send(update, "❌ Specifica una lega: /last [league]");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(param);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + param);
                    break;
                }
                send(update, FootballApi.getLastMatch(leagueId));
            }

            case "/leagues" -> send(update, FootballApi.getLeagues());

            case "/league" -> {
                if (param == null) {
                    send(update, "❌ Specifica il nome della lega: /league [name]");
                    break;
                }
                send(update, FootballApi.getLeagueInfo(param));
            }

            case "/team" -> {
                if (param == null) {
                    send(update, "❌ Specifica il nome della squadra: /team [name] [league]");
                    break;
                }
                String[] teamParts = param.split("\\s+", 2);
                String teamName = teamParts[0];
                Integer leagueId = teamParts.length > 1 ? LeagueResolver.resolveLeagueId(teamParts[1]) : null;

                Integer teamId = TeamResolver.resolveTeamId(teamName, leagueId);
                if (teamId == null) {
                    send(update, "❌ Squadra non trovata: " + teamName);
                    break;
                }

                send(update, FootballApi.getTeamInfo(String.valueOf(teamId)));
            }

            case "/stadium" -> {
                if (param == null) {
                    send(update, "❌ Specifica il nome dello stadio: /stadium [name]");
                    break;
                }
                send(update, FootballApi.getStadiumInfo(param));
            }

            default -> send(update, "❓ Comando non riconosciuto. Usa /start per il menu.");
        }
    }

    private void send(Update update, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(text)
                .parseMode("Markdown")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
