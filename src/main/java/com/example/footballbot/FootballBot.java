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

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String msgText = update.getMessage().getText().trim();
        String[] parts = msgText.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String param = parts.length > 1 ? parts[1] : null;

        switch (command) {
            case "/start" -> {
                String menu = """
⚽ *Bot Calcio Attivo!*

Comandi disponibili:
/ping - Test connessione
/nextmatch "league"
/topteams "league"
/relegation "league"
/form "team" "league"
/today "league"
/last "league"
/leagues
/league "name"
/team "name" ["league"]
/stadium "name"

Esempi:
/nextmatch "Premier League"
/form "Arsenal" "Premier League"
/team "Arsenal"
""";
                send(update, menu);
            }

            case "/ping" -> send(update, "🏓 *Pong!* Connessione attiva.");

            case "/nextmatch" -> {
                String leagueName = extractFirstQuoted(param);
                if (leagueName == null) {
                    send(update, "❌ Usa: /nextmatch \"league\"\nEsempio: /nextmatch \"Premier League\"");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(leagueName);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + leagueName);
                    break;
                }
                send(update, FootballApi.getNextMatch(leagueId));
            }

            case "/topteams" -> {
                String leagueName = extractFirstQuoted(param);
                if (leagueName == null) {
                    send(update, "❌ Usa: /topteams \"league\"\nEsempio: /topteams \"Serie A\"");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(leagueName);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + leagueName);
                    break;
                }
                send(update, FootballApi.getTopTeams(leagueId));
            }

            case "/relegation" -> {
                String leagueName = extractFirstQuoted(param);
                if (leagueName == null) {
                    send(update, "❌ Usa: /relegation \"league\"\nEsempio: /relegation \"La Liga\"");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(leagueName);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + leagueName);
                    break;
                }
                send(update, FootballApi.getRelegation(leagueId));
            }

            case "/form" -> {
                if (param == null) {
                    send(update, "❌ Usa: /form \"team\" [\"league\"]\nEsempi:\n/form \"Arsenal\" \"Premier League\"\n/form \"Arsenal\"");
                    break;
                }

                String teamName = null;
                String leagueName = null;

                try {
                    // Prima coppia di virgolette (obbligatoria - team)
                    int firstStart = param.indexOf('"');
                    int firstEnd = param.indexOf('"', firstStart + 1);

                    if (firstStart == -1 || firstEnd == -1) {
                        send(update, "❌ Usa: /form \"team\" [\"league\"]");
                        break;
                    }

                    teamName = param.substring(firstStart + 1, firstEnd);

                    // Seconda coppia di virgolette (opzionale - league)
                    int secondStart = param.indexOf('"', firstEnd + 1);
                    if (secondStart != -1) {
                        int secondEnd = param.indexOf('"', secondStart + 1);
                        if (secondEnd != -1) {
                            leagueName = param.substring(secondStart + 1, secondEnd);
                        }
                    }
                } catch (Exception e) {
                    send(update, "❌ Formato errato. Usa: /form \"team\" [\"league\"]");
                    break;
                }

                Integer leagueId = null;
                if (leagueName != null) {
                    leagueId = LeagueResolver.resolveLeagueId(leagueName);
                    if (leagueId == null) {
                        send(update, "❌ Lega non trovata: " + leagueName);
                        break;
                    }
                }

                Integer teamId = TeamResolver.resolveTeamId(teamName, leagueId);
                if (teamId == null) {
                    String msg = leagueName != null
                            ? "❌ Squadra non trovata: " + teamName + " nella lega " + leagueName
                            : "❌ Squadra non trovata: " + teamName;
                    send(update, msg);
                    break;
                }

                send(update, FootballApi.getForm(teamId));
            }

            case "/today" -> {
                String leagueName = extractFirstQuoted(param);
                if (leagueName == null) {
                    send(update, "❌ Usa: /today \"league\"\nEsempio: /today \"Bundesliga\"");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(leagueName);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + leagueName);
                    break;
                }
                send(update, FootballApi.getTodayMatches(leagueId));
            }

            case "/last" -> {
                String leagueName = extractFirstQuoted(param);
                if (leagueName == null) {
                    send(update, "❌ Usa: /last \"league\"\nEsempio: /last \"Ligue 1\"");
                    break;
                }
                Integer leagueId = LeagueResolver.resolveLeagueId(leagueName);
                if (leagueId == null) {
                    send(update, "❌ Lega non trovata: " + leagueName);
                    break;
                }
                send(update, FootballApi.getLastMatch(leagueId));
            }

            case "/leagues" -> send(update, FootballApi.getLeagues());

            case "/league" -> {
                String leagueName = extractFirstQuoted(param);
                if (leagueName == null) {
                    send(update, "❌ Usa: /league \"name\"\nEsempio: /league \"Premier League\"");
                    break;
                }
                send(update, FootballApi.getLeagueInfo(leagueName));
            }

            case "/team" -> {
                if (param == null) {
                    send(update, "❌ Usa: /team \"squadra\" [\"lega\"]\nEsempi:\n/team \"Arsenal\" \"Premier League\"\n/team \"Arsenal\"");
                    break;
                }

                String teamName = null;
                String leagueName = null;

                try {
                    int firstQuoteStart = param.indexOf('"');
                    int firstQuoteEnd = param.indexOf('"', firstQuoteStart + 1);

                    if (firstQuoteStart == -1 || firstQuoteEnd == -1) {
                        send(update, "❌ Usa: /team \"squadra\" [\"lega\"]");
                        break;
                    }

                    teamName = param.substring(firstQuoteStart + 1, firstQuoteEnd);

                    // Cerca la seconda coppia di virgolette (opzionale)
                    int secondQuoteStart = param.indexOf('"', firstQuoteEnd + 1);
                    if (secondQuoteStart != -1) {
                        int secondQuoteEnd = param.indexOf('"', secondQuoteStart + 1);
                        if (secondQuoteEnd != -1) {
                            leagueName = param.substring(secondQuoteStart + 1, secondQuoteEnd);
                        }
                    }

                } catch (Exception e) {
                    send(update, "❌ Formato errato. Usa: /team \"squadra\" [\"lega\"]");
                    break;
                }

                Integer leagueId = null;
                if (leagueName != null) {
                    leagueId = LeagueResolver.resolveLeagueId(leagueName);
                    if (leagueId == null) {
                        send(update, "❌ Lega non trovata: " + leagueName);
                        break;
                    }
                }

                Integer teamId = TeamResolver.resolveTeamId(teamName, leagueId);
                if (teamId == null) {
                    String msg = leagueName != null
                            ? "❌ Squadra non trovata: " + teamName + " nella lega " + leagueName
                            : "❌ Squadra non trovata: " + teamName;
                    send(update, msg);
                    break;
                }

                send(update, FootballApi.getTeamInfo(String.valueOf(teamId), leagueName));
            }

            case "/stadium" -> {
                String stadiumName = extractFirstQuoted(param);
                if (stadiumName == null) {
                    send(update, "❌ Usa: /stadium \"name\"\nEsempio: /stadium \"Emirates Stadium\"");
                    break;
                }
                send(update, FootballApi.getStadiumInfo(stadiumName));
            }

            default -> send(update, "❓ Comando non riconosciuto. Usa /start per il menu.");
        }
    }

    /**
     * Estrae il contenuto della prima coppia di virgolette
     */
    private String extractFirstQuoted(String text) {
        if (text == null) return null;

        try {
            int start = text.indexOf('"');
            int end = text.indexOf('"', start + 1);

            if (start == -1 || end == -1) return null;

            return text.substring(start + 1, end);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Estrae il contenuto di due coppie di virgolette
     * @return Array di 2 elementi [primo, secondo] o null se non valido
     */
    private String[] extractTwoQuoted(String text) {
        if (text == null) return null;

        try {
            // Prima coppia
            int firstStart = text.indexOf('"');
            int firstEnd = text.indexOf('"', firstStart + 1);

            if (firstStart == -1 || firstEnd == -1) return null;

            String first = text.substring(firstStart + 1, firstEnd);

            // Seconda coppia
            int secondStart = text.indexOf('"', firstEnd + 1);
            int secondEnd = text.indexOf('"', secondStart + 1);

            if (secondStart == -1 || secondEnd == -1) return null;

            String second = text.substring(secondStart + 1, secondEnd);

            return new String[]{first, second};
        } catch (Exception e) {
            return null;
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