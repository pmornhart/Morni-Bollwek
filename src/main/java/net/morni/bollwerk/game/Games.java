package net.morni.bollwerk.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.morni.bollwerk.BollwerkPlugin;
import net.morni.bollwerk.world.MapType;
import org.bukkit.entity.Player;

/** Alle laufenden Runden (höchstens eine je Karte). */
public final class Games {

    private final BollwerkPlugin plugin;
    private final Map<MapType, Game> running = new EnumMap<>(MapType.class);

    public Games(BollwerkPlugin plugin) {
        this.plugin = plugin;
    }

    public Game start(MapType map, List<Player> players) {
        Game g = new Game(plugin, map, players);
        running.put(map, g);
        plugin.stateChanged();
        return g;
    }

    public Collection<Game> all() {
        return running.values();
    }

    public Game on(MapType map) {
        return running.get(map);
    }

    /** Karten ohne laufende Runde. */
    public Set<MapType> free() {
        Set<MapType> out = EnumSet.allOf(MapType.class);
        out.removeAll(running.keySet());
        return out;
    }

    /** Runde, in der jemand mitspielt (auch gerade nicht verbunden) oder zuschaut. */
    public Game of(UUID uuid) {
        for (Game g : running.values()) {
            if (g.fighter(uuid) != null || g.spectating(uuid)) {
                return g;
            }
        }
        return null;
    }

    /** Runde, in der jemand mitspielt (nicht zuschauend). */
    public Game playing(UUID uuid) {
        for (Game g : running.values()) {
            if (g.fighter(uuid) != null) {
                return g;
            }
        }
        return null;
    }

    public void tick() {
        for (Game g : new ArrayList<>(running.values())) {
            boolean alive;
            try {
                alive = g.tick();
            } catch (RuntimeException e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Fehler in der Runde " + g.map().id()
                        + " – sie wird beendet", e);
                g.close();
                alive = false;
            }
            if (!alive) {
                running.remove(g.map());
                plugin.stateChanged();
                plugin.boards().refreshHall();
            }
        }
    }

    public void abortAll() {
        for (Game g : new ArrayList<>(running.values())) {
            g.close();
        }
        running.clear();
    }

    public int fighting() {
        return running.values().stream().mapToInt(Game::playerCount).sum();
    }
}
