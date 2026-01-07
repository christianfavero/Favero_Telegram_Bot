package com.example.footballbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FootballApi {

    private static final String BASE_URL = "https://api.football-data.org/v4";

    // Chiamata generica all'API
    public static String makeRequest(String endpoint) {
        String url = BASE_URL + endpoint;
        String apiKey = Config.getApiKey();
        if (apiKey == null || apiKey.isEmpty())
            return "❌ API key mancante!";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("X-Auth-Token", apiKey.trim());

            try (CloseableHttpResponse response = client.execute(request)) {
                String body = EntityUtils.toString(response.getEntity());
                if (response.getCode() != 200) return "❌ Errore API: " + body;
                return body;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nella connessione API.";
        }
    }

    /** Prossima partita di una lega */
    public static String getNextMatch(int leagueId) {
        String json = makeRequest("/competitions/" + leagueId + "/matches?status=SCHEDULED&limit=5");
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonArray matches = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("matches");
            if (matches.isEmpty()) return "📭 Nessuna partita programmata.";

            JsonObject m = matches.get(0).getAsJsonObject();
            String home = m.getAsJsonObject("homeTeam").get("name").getAsString();
            String away = m.getAsJsonObject("awayTeam").get("name").getAsString();
            String date = m.get("utcDate").getAsString();

            return String.format("⚽ *%s* vs *%s*\n📅 %s", home, away, date);
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare le partite.";
        }
    }

    /** Form ultimi 5 match di una squadra */
    public static String getForm(int teamId) {
        String json = makeRequest("/teams/" + teamId + "/matches?limit=5");
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonArray matches = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("matches");
            if (matches.isEmpty()) return "📭 Nessuna partita recente.";

            StringBuilder sb = new StringBuilder("📊 Ultimi risultati:\n");
            for (int i = 0; i < matches.size(); i++) {
                JsonObject m = matches.get(i).getAsJsonObject();
                String home = m.getAsJsonObject("homeTeam").get("name").getAsString();
                String away = m.getAsJsonObject("awayTeam").get("name").getAsString();
                String score = m.getAsJsonObject("score").get("fullTime").toString();
                sb.append(String.format("⚽ %s vs %s → %s\n", home, away, score));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare i risultati.";
        }
    }

    /** Top teams di una lega */
    public static String getTopTeams(int leagueId) {
        String json = makeRequest("/competitions/" + leagueId + "/standings");
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonArray standings = JsonParser.parseString(json)
                    .getAsJsonObject()
                    .getAsJsonArray("standings");
            if (standings.isEmpty()) return "📭 Nessuna classifica disponibile.";

            JsonArray table = standings.get(0).getAsJsonObject().getAsJsonArray("table");

            StringBuilder sb = new StringBuilder("🏆 Classifica Top Squadre:\n");
            for (int i = 0; i < table.size(); i++) {
                JsonObject t = table.get(i).getAsJsonObject();
                int pos = t.get("position").getAsInt();
                String name = t.getAsJsonObject("team").get("name").getAsString();
                sb.append(String.format("%d. %s\n", pos, name));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare la classifica.";
        }
    }

    /** Squadre retrocesse di una lega */
    public static String getRelegation(int leagueId) {
        String json = makeRequest("/competitions/" + leagueId + "/standings");
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonArray standings = JsonParser.parseString(json)
                    .getAsJsonObject()
                    .getAsJsonArray("standings");
            if (standings.isEmpty()) return "📭 Nessuna classifica disponibile.";

            JsonArray table = standings.get(0).getAsJsonObject().getAsJsonArray("table");
            StringBuilder sb = new StringBuilder("📉 Squadre retrocesse:\n");
            for (int i = table.size() - 3; i < table.size(); i++) {
                JsonObject t = table.get(i).getAsJsonObject();
                String name = t.getAsJsonObject("team").get("name").getAsString();
                sb.append(String.format("%s\n", name));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare la retrocessione.";
        }
    }

    /** Partite di oggi */
    public static String getTodayMatches(int leagueId) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String json = makeRequest("/competitions/" + leagueId + "/matches?dateFrom=" + today + "&dateTo=" + today);
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonArray matches = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("matches");
            if (matches.isEmpty()) return "📭 Nessuna partita oggi.";

            StringBuilder sb = new StringBuilder("📅 Partite oggi:\n");
            for (int i = 0; i < matches.size(); i++) {
                JsonObject m = matches.get(i).getAsJsonObject();
                String home = m.getAsJsonObject("homeTeam").get("name").getAsString();
                String away = m.getAsJsonObject("awayTeam").get("name").getAsString();
                sb.append(String.format("⚽ %s vs %s\n", home, away));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare le partite di oggi.";
        }
    }

    /** Ultima partita giocata */
    public static String getLastMatch(int leagueId) {
        String json = makeRequest("/competitions/" + leagueId + "/matches?status=FINISHED&limit=5");
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonArray matches = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("matches");
            if (matches.isEmpty()) return "📭 Nessuna partita recente.";

            JsonObject m = matches.get(matches.size() - 1).getAsJsonObject();
            String home = m.getAsJsonObject("homeTeam").get("name").getAsString();
            String away = m.getAsJsonObject("awayTeam").get("name").getAsString();
            String score = m.getAsJsonObject("score").get("fullTime").toString();

            return String.format("⚽ *Ultima partita:* %s vs %s → %s", home, away, score);
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare l'ultima partita.";
        }
    }

    /** Lista leghe disponibili */
    public static String getLeagues() {
        return """
                🏟️ Leghe disponibili:
                Premier League
                Serie A
                La Liga
                Bundesliga
                Ligue 1
                """;
    }

    /** Info su una lega */
    public static String getLeagueInfo(String leagueName) {
        Integer id = LeagueResolver.resolveLeagueId(leagueName);
        if (id == null) return "❌ Lega non trovata!";
        String json = makeRequest("/competitions/" + id);
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return String.format("🏟️ %s (%s)\nAnno inizio: %d\nNumero squadre: %d",
                    obj.get("name").getAsString(),
                    obj.get("area").getAsJsonObject().get("name").getAsString(),
                    obj.get("firstSeason").getAsInt(),
                    obj.get("numberOfTeams").getAsInt());
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare le informazioni della lega.";
        }
    }

    /** Info su una squadra */
    public static String getTeamInfo(String teamName) {
        Integer teamId = TeamResolver.resolveTeamId(teamName, 2021); // default Premier League
        if (teamId == null) return "❌ Squadra non trovata!";
        String json = makeRequest("/teams/" + teamId);
        if (json == null) return "❌ Nessuna risposta dall'API.";

        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return String.format("⚽ %s\nStadio: %s\nFondazione: %s",
                    obj.get("name").getAsString(),
                    obj.get("venue").getAsString(),
                    obj.get("founded").getAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Errore nel processare le informazioni della squadra.";
        }
    }

    /** Info su uno stadio */
    public static String getStadiumInfo(String stadiumName) {
        // Nota: l'API non fornisce endpoint diretto per stadi, quindi proviamo a cercare tra tutte le squadre
        StringBuilder sb = new StringBuilder();
        for (String name : TeamResolver.getAllTeamNames()) {
            try {
                Integer teamId = TeamResolver.resolveTeamId(name, 2021);
                if (teamId == null) continue;
                String json = makeRequest("/teams/" + teamId);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String venue = obj.get("venue").getAsString();
                if (venue.equalsIgnoreCase(stadiumName)) {
                    sb.append(String.format("🏟️ %s → Squadra: %s\n", venue, obj.get("name").getAsString()));
                }
            } catch (Exception ignored) {}
        }
        return sb.length() > 0 ? sb.toString() : "❌ Stadio non trovato!";
    }
}
