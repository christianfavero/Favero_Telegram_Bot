package com.example.footballbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TeamResolver {

    // Cache per non richiamare l’API ogni volta
    private static final Map<String, Integer> TEAM_CACHE = new HashMap<>();
    private static boolean loaded = false;

    /**
     * Carica tutte le squadre di una lega
     */
    public static void loadTeams(int leagueId) {
        if (loaded) return;

        String json = FootballApi.makeRequest("/competitions/" + leagueId + "/teams");
        if (json == null) return;

        try {
            JsonArray teams = JsonParser.parseString(json)
                    .getAsJsonObject()
                    .getAsJsonArray("teams");

            for (int i = 0; i < teams.size(); i++) {
                JsonObject t = teams.get(i).getAsJsonObject();
                int id = t.get("id").getAsInt();
                String name = normalize(t.get("name").getAsString());

                TEAM_CACHE.put(name, id);
            }

            loaded = true;
            System.out.println("✅ TeamResolver: caricate " + TEAM_CACHE.size() + " squadre");

        } catch (Exception e) {
            System.err.println("❌ Errore TeamResolver");
            e.printStackTrace();
        }
    }

    /**
     * Restituisce ID squadra partendo dal nome
     */
    public static Integer resolveTeamId(String userInput, int leagueId) {
        if (!loaded) loadTeams(leagueId);

        String input = normalize(userInput);

        // Match diretto
        if (TEAM_CACHE.containsKey(input))
            return TEAM_CACHE.get(input);

        // Match parziale (es: "inter" → "inter milan")
        for (Map.Entry<String, Integer> e : TEAM_CACHE.entrySet()) {
            if (e.getKey().contains(input))
                return e.getValue();
        }

        return null;
    }

    /**
     * Restituisce tutti i nomi delle squadre caricate
     */
    public static Set<String> getAllTeamNames() {
        return TEAM_CACHE.keySet();
    }

    /**
     * Normalizzazione stringhe
     */
    private static String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }
}
