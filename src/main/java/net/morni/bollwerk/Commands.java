package net.morni.bollwerk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.bollwerk.data.Leaderboard;
import net.morni.bollwerk.data.PlayerData;
import net.morni.bollwerk.game.Game;
import net.morni.bollwerk.game.Shop;
import net.morni.bollwerk.hall.HallMenus;
import net.morni.bollwerk.world.MapType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Alle Befehle. */
final class Commands implements TabExecutor {

    private static final String ADMIN = "morninet.bollwerk.admin";
    private final BollwerkPlugin plugin;

    Commands(BollwerkPlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? plugin.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("bollwerk")) {
            return admin(sender, args);
        }
        if (name.equals("stats")) {
            stats(sender, args);
            return true;
        }
        if (name.equals("top")) {
            top(sender, args);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.msg("de", "only-players"));
            return true;
        }
        String lang = plugin.lang(p);
        switch (name) {
            case "shop" -> {
                Game g = plugin.games().playing(p.getUniqueId());
                if (g != null) {
                    Shop.open(plugin, g, p);
                }
            }
            case "bereit" -> {
                Game g = plugin.games().playing(p.getUniqueId());
                if (g != null) {
                    g.ready(p);
                } else {
                    plugin.hall().toggleReady(p);
                }
            }
            case "karte" -> {
                MapType m = args.length > 0 ? MapType.byId(args[0]) : null;
                if (m == null || !plugin.hall().inHall(p.getUniqueId())) {
                    HallMenus.vote(plugin, p);
                } else {
                    plugin.hall().vote(p, m);
                }
            }
            case "zuschauen" -> spectate(p, args);
            case "verlassen" -> {
                Game g = plugin.games().of(p.getUniqueId());
                if (g == null) {
                    p.sendMessage(Texts.msg(lang, "leave.none"));
                } else {
                    g.leave(p);
                    plugin.hall().enter(p);
                    p.sendMessage(Texts.msg(lang, "leave.done"));
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void spectate(Player p, String[] args) {
        String lang = plugin.lang(p);
        Game g = null;
        if (args.length > 0) {
            MapType m = MapType.byId(args[0]);
            if (m != null) {
                g = plugin.games().on(m);
            } else {
                Player t = Bukkit.getPlayerExact(args[0]);
                if (t == null) {
                    p.sendMessage(Texts.msg(lang, "unknown-player", Placeholder.unparsed("name", args[0])));
                    return;
                }
                g = plugin.games().of(t.getUniqueId());
            }
        } else if (!plugin.games().all().isEmpty()) {
            g = plugin.games().all().iterator().next();
        }
        if (g == null) {
            p.sendMessage(Texts.msg(lang, "spectate.none"));
            return;
        }
        if (g.fighter(p.getUniqueId()) != null) {
            return;
        }
        plugin.spectate(p, g);
    }

    private void stats(CommandSender sender, String[] args) {
        String lang = lang(sender);
        UUID uuid;
        String who;
        if (args.length > 0) {
            Player online = Bukkit.getPlayerExact(args[0]);
            OfflinePlayer op = online != null ? online : Bukkit.getOfflinePlayerIfCached(args[0]);
            if (op == null) {
                sender.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", args[0])));
                return;
            }
            uuid = op.getUniqueId();
            who = op.getName() == null ? args[0] : op.getName();
        } else if (sender instanceof Player p) {
            uuid = p.getUniqueId();
            who = p.getName();
        } else {
            sender.sendMessage(Texts.msg(lang, "only-players"));
            return;
        }
        Optional<PlayerData> d = plugin.store().read(uuid);
        if (d.isEmpty()) {
            sender.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", who)));
            return;
        }
        sender.sendMessage(Texts.msg(lang, "stats.head", Placeholder.unparsed("name", who)));
        HallMenus.statLines(lang, d.get()).forEach(sender::sendMessage);
    }

    private void top(CommandSender sender, String[] args) {
        String lang = lang(sender);
        List<MapType> maps = args.length > 0 && MapType.byId(args[0]) != null ? List.of(MapType.byId(args[0]))
                : Arrays.asList(MapType.values());
        for (MapType m : maps) {
            sender.sendMessage(Texts.msg(lang, "top.head", Placeholder.unparsed("map", Texts.plain(lang, "map." + m.id()))));
            List<Leaderboard.Entry> top = plugin.board().top(m.id());
            if (top.isEmpty()) {
                sender.sendMessage(Texts.text(lang, "menu.stats.empty"));
            }
            for (int i = 0; i < top.size(); i++) {
                Leaderboard.Entry e = top.get(i);
                sender.sendMessage(Texts.text(lang, "menu.stats.row", Placeholder.unparsed("pos", String.valueOf(i + 1)),
                        Placeholder.unparsed("waves", String.valueOf(e.waves())),
                        Placeholder.unparsed("names", String.join(", ", e.names()))));
            }
        }
    }

    /** Verwaltung: info, status, start, welle, herz, stopp, neubauen, neuladen. */
    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "info";
        MapType chosen = args.length > 1 ? MapType.byId(args[args.length - 1]) : null;
        Game g = chosen != null ? plugin.games().on(chosen)
                : plugin.games().all().isEmpty() ? null : plugin.games().all().iterator().next();
        switch (sub) {
            case "info", "status" -> {
                sender.sendMessage(Component.text("Bollwerk: bereit=" + plugin.ready() + " netzwerk="
                        + plugin.net().connected() + " wachstube=" + plugin.hall().waiting().size() + " countdown="
                        + plugin.hall().countdown() + " runden=" + plugin.games().all().size()));
                for (Game x : plugin.games().all()) {
                    sender.sendMessage(Component.text(x.status()));
                }
            }
            case "start" -> {
                MapType m = chosen != null ? chosen : plugin.hall().votes().winner(plugin.games().free());
                if (m == null || plugin.games().on(m) != null) {
                    sender.sendMessage(Component.text("Keine freie Karte."));
                } else {
                    Game started = plugin.hall().start(m);
                    sender.sendMessage(Component.text(started == null ? "Niemand in der Wachstube." : "Gestartet: " + m.id()));
                }
            }
            case "welle" -> {
                if (g == null || args.length < 2) {
                    sender.sendMessage(Component.text("/bollwerk welle <n> [karte]"));
                } else {
                    g.forceWave(parse(args[1], 1));
                    sender.sendMessage(Component.text("Welle " + g.wave() + " gestartet."));
                }
            }
            case "herz" -> {
                if (g == null || args.length < 2) {
                    sender.sendMessage(Component.text("/bollwerk herz <leben> [karte]"));
                } else {
                    g.setHeart(parse(args[1], 0));
                    sender.sendMessage(Component.text("Herz: " + g.heart().health()));
                }
            }
            case "stopp" -> {
                if (g != null) {
                    g.setHeart(0);
                }
            }
            case "neubauen" -> plugin.rebuild(() -> sender.sendMessage(Component.text("Neu gebaut.")));
            case "neuladen" -> {
                plugin.reloadSettings();
                sender.sendMessage(Component.text("Einstellungen neu geladen."));
            }
            default -> sender.sendMessage(Component.text("/bollwerk info|status|start|welle|herz|stopp|neubauen|neuladen"));
        }
        return true;
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            switch (name) {
                case "bollwerk" -> out.addAll(List.of("info", "status", "start", "welle", "herz", "stopp", "neubauen", "neuladen"));
                case "karte", "top" -> Arrays.stream(MapType.values()).forEach(m -> out.add(m.id()));
                case "zuschauen", "stats" -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> {
                }
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
