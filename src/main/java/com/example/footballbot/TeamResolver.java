package com.example.footballbot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TeamResolver {

    // Cache per non richiamare l'API ogni volta
    // Chiave: "nome_normalizzato" o "nome_normalizzato_leagueId"
    private static final Map<String, Integer> TEAM_CACHE = new HashMap<>();

    // Tiene traccia delle leghe già caricate
    private static final Map<Integer, Boolean> LOADED_LEAGUES = new HashMap<>();

    /**
     * Carica tutte le squadre di una lega specifica
     */
    public static void loadTeams(int leagueId) {
        if (LOADED_LEAGUES.getOrDefault(leagueId, false)) {
            System.out.println("⏭️ TeamResolver: lega " + leagueId + " già caricata");
            return;
        }

        String json = FootballApi.makeRequest("/competitions/" + leagueId + "/teams");
        if (json == null) {
            System.err.println("❌ TeamResolver: nessuna risposta per lega " + leagueId);
            return;
        }

        try {
            JsonArray teams = JsonParser.parseString(json)
                    .getAsJsonObject()
                    .getAsJsonArray("teams");

            for (int i = 0; i < teams.size(); i++) {
                JsonObject t = teams.get(i).getAsJsonObject();
                int id = t.get("id").getAsInt();
                String name = normalize(t.get("name").getAsString());

                // Salva con chiave composta: "nome_leagueId" per ricerca specifica
                String keyWithLeague = name + "_" + leagueId;
                TEAM_CACHE.put(keyWithLeague, id);

                // Salva anche senza lega per ricerca globale (solo se non esiste già)
                if (!TEAM_CACHE.containsKey(name)) {
                    TEAM_CACHE.put(name, id);
                }
            }

            LOADED_LEAGUES.put(leagueId, true);
            System.out.println("✅ TeamResolver: caricate " + teams.size() + " squadre per lega " + leagueId);

        } catch (Exception e) {
            System.err.println("❌ Errore TeamResolver per lega " + leagueId);
            e.printStackTrace();
        }
    }

    /**
     * Carica tutte le squadre di tutte le leghe disponibili
     */
    public static void loadAllTeams() {
        System.out.println("🔄 Caricamento di tutte le squadre...");

        // Ottiene tutti gli ID delle leghe disponibili
        for (String leagueName : LeagueResolver.getAllLeagueNames()) {
            Integer leagueId = LeagueResolver.resolveLeagueId(leagueName);
            if (leagueId != null) {
                loadTeams(leagueId);
            }
        }

        System.out.println("✅ Caricate squadre da " + LOADED_LEAGUES.size() + " leghe");
    }

    /**
     * Restituisce ID squadra partendo dal nome
     *
     * @param userInput Nome della squadra inserito dall'utente
     * @param leagueId ID della lega (può essere null per cercare in tutte)
     * @return ID della squadra o null se non trovata
     */
    public static Integer resolveTeamId(String userInput, Integer leagueId) {
        String input = normalize(userInput);

        // Se la lega è specificata, cerca solo in quella lega
        if (leagueId != null) {
            // Carica la lega se non è già stata caricata
            if (!LOADED_LEAGUES.getOrDefault(leagueId, false)) {
                loadTeams(leagueId);
            }

            // Match esatto con lega specifica
            String keyWithLeague = input + "_" + leagueId;
            if (TEAM_CACHE.containsKey(keyWithLeague)) {
                return TEAM_CACHE.get(keyWithLeague);
            }

            // Match parziale nella lega specifica
            for (Map.Entry<String, Integer> e : TEAM_CACHE.entrySet()) {
                String key = e.getKey();
                if (key.endsWith("_" + leagueId) && key.startsWith(input)) {
                    return e.getValue();
                }
            }

            // Match parziale più permissivo (contiene)
            for (Map.Entry<String, Integer> e : TEAM_CACHE.entrySet()) {
                String key = e.getKey();
                if (key.endsWith("_" + leagueId) && key.contains(input)) {
                    return e.getValue();
                }
            }
        } else {
            // Nessuna lega specificata: cerca in tutte le leghe
            if (LOADED_LEAGUES.isEmpty()) {
                loadAllTeams();
            }

            // Match esatto globale
            if (TEAM_CACHE.containsKey(input)) {
                return TEAM_CACHE.get(input);
            }

            // Match parziale globale (solo chiavi senza "_leagueId")
            for (Map.Entry<String, Integer> e : TEAM_CACHE.entrySet()) {
                String key = e.getKey();
                if (!key.contains("_") && key.startsWith(input)) {
                    return e.getValue();
                }
            }

            // Match parziale più permissivo
            for (Map.Entry<String, Integer> e : TEAM_CACHE.entrySet()) {
                String key = e.getKey();
                if (!key.contains("_") && key.contains(input)) {
                    return e.getValue();
                }
            }
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
     * Pulisce la cache (utile per test o refresh)
     */
    public static void clearCache() {
        TEAM_CACHE.clear();
        LOADED_LEAGUES.clear();
        System.out.println("🗑️ Cache TeamResolver pulita");
    }

    /**
     * Normalizzazione stringhe per matching case-insensitive
     */
    private static String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }
}
