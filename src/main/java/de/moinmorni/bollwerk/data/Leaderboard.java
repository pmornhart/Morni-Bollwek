package de.moinmorni.bollwerk.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Bestenliste je Karte: die meisten überstandenen Wellen (höher ist besser). Gleich viele Wellen: wer es zuerst
 * geschafft hat, steht vorn. Gespeichert als {@code bestenliste.yml}.
 */
public final class Leaderboard {

    /** Ein Eintrag: Wellen, Namen der Truppe, Zeitpunkt. */
    public record Entry(int waves, List<String> names, long when) {
    }

    public static final int SIZE = 10;

    private final Map<String, List<Entry>> maps = new HashMap<>();

    /** Eintragen; liefert den Platz (1 = ganz vorn) oder 0, wenn es nicht für die Liste reicht. */
    public int add(String map, int waves, List<String> names, long when) {
        if (waves <= 0) {
            return 0;
        }
        List<Entry> list = maps.computeIfAbsent(map, m -> new ArrayList<>());
        int pos = 0;
        while (pos < list.size() && list.get(pos).waves() >= waves) {
            pos++;
        }
        if (pos >= SIZE) {
            return 0;
        }
        list.add(pos, new Entry(waves, List.copyOf(names), when));
        while (list.size() > SIZE) {
            list.remove(list.size() - 1);
        }
        return pos + 1;
    }

    public List<Entry> top(String map) {
        return List.copyOf(maps.getOrDefault(map, List.of()));
    }

    /** Beste Welle einer Karte (0 ohne Eintrag). */
    public int best(String map) {
        List<Entry> l = maps.get(map);
        return l == null || l.isEmpty() ? 0 : l.get(0).waves();
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        maps.forEach((map, list) -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Entry e : list) {
                Map<String, Object> row = new HashMap<>();
                row.put("wellen", e.waves());
                row.put("namen", e.names());
                row.put("zeit", e.when());
                rows.add(row);
            }
            y.set(map, rows);
        });
        return y.saveToString();
    }

    public static Leaderboard fromYaml(String text) {
        Leaderboard b = new Leaderboard();
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        for (String map : y.getKeys(false)) {
            List<Entry> list = new ArrayList<>();
            for (Map<?, ?> row : y.getMapList(map)) {
                Object w = row.get("wellen");
                Object n = row.get("namen");
                Object t = row.get("zeit");
                List<String> names = new ArrayList<>();
                if (n instanceof List<?> l) {
                    l.forEach(o -> names.add(String.valueOf(o)));
                }
                list.add(new Entry(w instanceof Number num ? num.intValue() : 0, List.copyOf(names),
                        t instanceof Number num ? num.longValue() : 0L));
            }
            list.sort((a, c) -> a.waves() != c.waves() ? Integer.compare(c.waves(), a.waves()) : Long.compare(a.when(), c.when()));
            b.maps.put(map, new ArrayList<>(list.subList(0, Math.min(SIZE, list.size()))));
        }
        return b;
    }
}
