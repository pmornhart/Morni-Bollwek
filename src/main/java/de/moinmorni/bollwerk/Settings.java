package de.moinmorni.bollwerk;

import org.bukkit.configuration.file.FileConfiguration;

/** Werte aus der config.yml (Erklärungen stehen dort). */
public record Settings(
        String world,
        int hallCountdown,
        int hallMinPlayers,
        int maxPlayers,
        int firstPrepare,
        int prepare,
        int afterSeconds,
        int startGold,
        int heartHealth,
        int downSeconds,
        int reviveSeconds) {

    static Settings load(FileConfiguration c) {
        return new Settings(
                c.getString("welt", "bollwerk"),
                clamp(c.getInt("wachstube.countdown-sekunden", 30), 3, 300),
                clamp(c.getInt("wachstube.mindestens-spieler", 2), 1, 6),
                clamp(c.getInt("runde.hoechstens-spieler", 6), 1, 6),
                clamp(c.getInt("runde.erster-aufbau-sekunden", 30), 3, 300),
                clamp(c.getInt("runde.aufbau-sekunden", 25), 3, 300),
                clamp(c.getInt("runde.nachspiel-sekunden", 10), 1, 60),
                clamp(c.getInt("runde.startgeld", 50), 0, 100000),
                clamp(c.getInt("runde.herz-leben", 400), 1, 100000),
                clamp(c.getInt("runde.am-boden-sekunden", 25), 3, 300),
                clamp(c.getInt("runde.retten-sekunden", 3), 1, 30));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
