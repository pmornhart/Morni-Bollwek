package de.moinmorni.bollwerk.data;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Werte einer Person: Spiele, Siege über Gegner, überstandene Wellen, Rettungen, beste Welle je Karte. */
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private int games;
    private int kills;
    private int waves;
    private int revives;
    private final Map<String, Integer> best = new TreeMap<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    public int games() {
        return games;
    }

    public int kills() {
        return kills;
    }

    public int waves() {
        return waves;
    }

    public int revives() {
        return revives;
    }

    public int best(String map) {
        return best.getOrDefault(map, 0);
    }

    /** Beste Welle über alle Karten. */
    public int bestOverall() {
        return best.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /** Ein Spiel zu Ende: Werte hochzählen; liefert {@code true} bei neuer Bestleistung auf dieser Karte. */
    public boolean addGame(String map, int kills, int waves, int revives) {
        this.games++;
        this.kills += Math.max(0, kills);
        this.waves += Math.max(0, waves);
        this.revives += Math.max(0, revives);
        if (waves > best(map)) {
            best.put(map, waves);
            return true;
        }
        return false;
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("spiele", games);
        y.set("kills", kills);
        y.set("wellen", waves);
        y.set("rettungen", revives);
        best.forEach((m, w) -> y.set("beste." + m, w));
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.games = y.getInt("spiele");
        d.kills = y.getInt("kills");
        d.waves = y.getInt("wellen");
        d.revives = y.getInt("rettungen");
        ConfigurationSection s = y.getConfigurationSection("beste");
        if (s != null) {
            for (String k : s.getKeys(false)) {
                d.best.put(k, s.getInt(k));
            }
        }
        return d;
    }
}
