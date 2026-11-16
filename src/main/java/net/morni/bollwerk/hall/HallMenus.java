package net.morni.bollwerk.hall;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.bollwerk.BollwerkPlugin;
import net.morni.bollwerk.Items;
import net.morni.bollwerk.Texts;
import net.morni.bollwerk.data.Leaderboard;
import net.morni.bollwerk.data.PlayerData;
import net.morni.bollwerk.game.Game;
import net.morni.bollwerk.menu.Menu;
import net.morni.bollwerk.world.MapType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Menüs der Wachstube: Karte wählen (oder zuschauen) und Bestenliste. */
public final class HallMenus {

    private HallMenus() {
    }

    public static void vote(BollwerkPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.vote.title"));
        MapType[] maps = MapType.values();
        int[] slots = {11, 15};
        for (int i = 0; i < maps.length && i < slots.length; i++) {
            MapType m = maps[i];
            Game running = plugin.games().on(m);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "map." + m.id() + ".desc"));
            lore.add(Texts.text(lang, "menu.vote.votes", Placeholder.unparsed("n", String.valueOf(plugin.hall().votes().count(m)))));
            lore.add(Texts.text(lang, "menu.vote.best", Placeholder.unparsed("n", String.valueOf(plugin.board().best(m.id())))));
            boolean mine = plugin.hall().votes().voteOf(p.getUniqueId()) == m;
            if (running != null) {
                lore.add(Texts.text(lang, "menu.vote.busy", Placeholder.unparsed("n", String.valueOf(Math.max(1, running.wave())))));
            } else {
                lore.add(Texts.text(lang, mine ? "menu.vote.mine" : "menu.vote.click"));
            }
            Material icon = m == MapType.TALSPERRE ? Material.STONE_BRICKS : Material.BELL;
            menu.set(slots[i], Items.glow(Items.named(icon, 1, Texts.text(lang, "map." + m.id()).color(
                    net.kyori.adventure.text.format.TextColor.color(Texts.EMERALD)), lore), mine), q -> {
                        q.closeInventory();
                        Game g = plugin.games().on(m);
                        if (g != null) {
                            plugin.spectate(q, g);
                        } else {
                            plugin.hall().vote(q, m);
                        }
                    });
        }
        menu.set(22, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static void stats(BollwerkPlugin plugin, Player p) {
        String lang = plugin.lang(p);
        Menu menu = new Menu(3, Texts.text(lang, "menu.stats.title"));
        PlayerData d = plugin.data(p);
        menu.set(10, Items.named(Material.PLAYER_HEAD, 1, Texts.text(lang, "menu.stats.me"), statLines(lang, d)), null);
        int slot = 13;
        for (MapType m : MapType.values()) {
            List<Component> lore = new ArrayList<>();
            List<Leaderboard.Entry> top = plugin.board().top(m.id());
            if (top.isEmpty()) {
                lore.add(Texts.text(lang, "menu.stats.empty"));
            }
            for (int i = 0; i < Math.min(10, top.size()); i++) {
                Leaderboard.Entry e = top.get(i);
                lore.add(Texts.text(lang, "menu.stats.row", Placeholder.unparsed("pos", String.valueOf(i + 1)),
                        Placeholder.unparsed("waves", String.valueOf(e.waves())),
                        Placeholder.unparsed("names", String.join(", ", e.names()))));
            }
            menu.set(slot, Items.named(m == MapType.TALSPERRE ? Material.STONE_BRICKS : Material.BELL, 1,
                    Texts.text(lang, "menu.stats.top", Placeholder.unparsed("map", Texts.plain(lang, "map." + m.id()))), lore), null);
            slot += 2;
        }
        menu.set(22, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
        menu.open(p);
    }

    public static List<Component> statLines(String lang, PlayerData d) {
        return Texts.lines(lang, "menu.stats.lines", Placeholder.unparsed("games", String.valueOf(d.games())),
                Placeholder.unparsed("kills", String.valueOf(d.kills())), Placeholder.unparsed("waves", String.valueOf(d.waves())),
                Placeholder.unparsed("revives", String.valueOf(d.revives())),
                Placeholder.unparsed("best", String.valueOf(d.bestOverall())));
    }
}
