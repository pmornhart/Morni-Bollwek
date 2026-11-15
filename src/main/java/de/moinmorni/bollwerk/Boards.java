package de.moinmorni.bollwerk;

import de.moinmorni.bollwerk.data.Leaderboard;
import de.moinmorni.bollwerk.game.Fighter;
import de.moinmorni.bollwerk.game.Game;
import de.moinmorni.bollwerk.world.HallPlan;
import de.moinmorni.bollwerk.world.MapType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Leiste rechts (Runde und Wachstube) und die schwebenden Tafeln in der Wachstube. */
public final class Boards {

    private static final int TOP_ROWS = 5;

    private final BollwerkPlugin plugin;
    private final Sidebar sidebar = new Sidebar();
    private final List<Holo> hallHolos = new ArrayList<>();

    Boards(BollwerkPlugin plugin) {
        this.plugin = plugin;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    /** Leiste für alle in einer Runde. */
    public void game(Game g) {
        for (Player p : g.audience()) {
            String lang = plugin.lang(p);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.empty());
            lines.add(Texts.text(lang, "side.map", Placeholder.unparsed("map", Texts.plain(lang, "map." + g.map().id()))));
            lines.add(Texts.text(lang, "side.wave", Placeholder.unparsed("n", String.valueOf(Math.max(1, g.wave())))));
            if (g.phase() == Game.Phase.PREPARE) {
                lines.add(Texts.text(lang, "side.prepare", Placeholder.unparsed("secs", String.valueOf(g.prepareSeconds()))));
            } else {
                lines.add(Texts.text(lang, "side.enemies", Placeholder.unparsed("n", String.valueOf(g.enemyCount()))));
            }
            double frac = g.heart().fraction();
            String colour = frac > 0.5 ? "green" : frac > 0.25 ? "yellow" : "red";
            lines.add(Texts.text(lang, "side.heart", Placeholder.parsed("hp",
                    "<" + colour + ">" + (int) Math.ceil(g.heart().health()) + "</" + colour + ">")));
            lines.add(Component.text(" "));
            Fighter f = g.fighter(p.getUniqueId());
            if (f != null) {
                lines.add(Texts.text(lang, "side.gold", Placeholder.unparsed("n", String.valueOf(f.wallet().balance()))));
                lines.add(Texts.text(lang, "side.kills", Placeholder.unparsed("n", String.valueOf(f.kills()))));
            }
            lines.add(Texts.text(lang, "side.team", Placeholder.unparsed("n", String.valueOf(g.playerCount()))));
            lines.add(Component.text("  "));
            lines.add(Texts.text(lang, "side.site"));
            sidebar.show(p, Texts.text(lang, "side.title"), lines);
        }
    }

    /** Leiste in der Wachstube. */
    public void hall(Player p) {
        String lang = plugin.lang(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "side.hall.players", Placeholder.unparsed("n", String.valueOf(plugin.hall().waiting().size()))));
        int cd = plugin.hall().countdown();
        lines.add(cd >= 0 ? Texts.text(lang, "side.hall.start", Placeholder.unparsed("secs", String.valueOf(cd)))
                : Texts.text(lang, "side.hall.wait"));
        lines.add(Component.text(" "));
        int best = plugin.store().loaded(p.getUniqueId()).map(d -> d.bestOverall()).orElse(0);
        lines.add(Texts.text(lang, "side.hall.best", Placeholder.unparsed("n", String.valueOf(best))));
        lines.add(Component.text("  "));
        lines.add(Texts.text(lang, "side.site"));
        sidebar.show(p, Texts.text(lang, "side.title"), lines);
    }

    public void hallAll() {
        for (UUID u : plugin.hall().waiting()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                hall(p);
            }
        }
    }

    // ---------- Tafeln in der Wachstube ----------

    public void spawnHall() {
        removeHall();
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < HallPlan.TABLES.length; i++) {
            MapType m = maps[i];
            int[] t = HallPlan.TABLES[i];
            Location at = new Location(plugin.world(), t[0] + 0.5, MapType.FLOOR_Y + 3.4, t[1] + 0.5);
            hallHolos.add(Holo.spawn(plugin, at, lang -> table(lang, m), 1f));
        }
        Location board = new Location(plugin.world(), HallPlan.BOARD[0] + 0.5, MapType.FLOOR_Y + 2.6,
                HallPlan.BOARD[1] + 0.2);
        hallHolos.add(Holo.spawn(plugin, board, this::top, 0.9f));
    }

    private Component table(String lang, MapType m) {
        return Texts.text(lang, "board.table", Placeholder.unparsed("map", Texts.plain(lang, "map." + m.id())),
                Placeholder.unparsed("desc", Texts.plain(lang, "board.table.desc." + m.id())),
                Placeholder.unparsed("n", String.valueOf(plugin.board().best(m.id()))));
    }

    private Component top(String lang) {
        Component c = Texts.text(lang, "board.title");
        for (MapType m : MapType.values()) {
            c = c.append(Component.newline()).append(Component.newline())
                    .append(Texts.text(lang, "menu.stats.top", Placeholder.unparsed("map", Texts.plain(lang, "map." + m.id()))));
            List<Leaderboard.Entry> top = plugin.board().top(m.id());
            if (top.isEmpty()) {
                c = c.append(Component.newline()).append(Texts.text(lang, "menu.stats.empty"));
            }
            for (int i = 0; i < Math.min(TOP_ROWS, top.size()); i++) {
                Leaderboard.Entry e = top.get(i);
                c = c.append(Component.newline()).append(Texts.text(lang, "menu.stats.row",
                        Placeholder.unparsed("pos", String.valueOf(i + 1)),
                        Placeholder.unparsed("waves", String.valueOf(e.waves())),
                        Placeholder.unparsed("names", String.join(", ", e.names()))));
            }
        }
        return c;
    }

    public void refreshHall() {
        if (hallHolos.isEmpty()) {
            return;
        }
        MapType[] maps = MapType.values();
        for (int i = 0; i < maps.length && i < hallHolos.size() - 1; i++) {
            MapType m = maps[i];
            hallHolos.get(i).update(lang -> table(lang, m));
        }
        hallHolos.get(hallHolos.size() - 1).update(this::top);
    }

    public void removeHall() {
        hallHolos.forEach(Holo::remove);
        hallHolos.clear();
    }
}
