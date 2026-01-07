package com.example.footballbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LeagueResolver {
    private static final Map<String, Integer> LEAGUE_CACHE = new HashMap<>();

    static {
        LEAGUE_CACHE.put("premier league", 2021);
        LEAGUE_CACHE.put("serie a", 2019);
        LEAGUE_CACHE.put("la liga", 2014);
        LEAGUE_CACHE.put("bundesliga", 2002);
        LEAGUE_CACHE.put("ligue 1", 2015);
    }

    public static Integer resolveLeagueId(String name) {
        if (name == null) return null;
        return LEAGUE_CACHE.get(name.toLowerCase().trim());
    }

    // ✅ Nuovo metodo per ottenere tutti i nomi delle leghe
    public static Set<String> getAllLeagueNames() {
        return LEAGUE_CACHE.keySet();
    }
}
