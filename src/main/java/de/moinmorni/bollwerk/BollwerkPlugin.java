package de.moinmorni.bollwerk;

import de.moinmorni.bollwerk.data.Leaderboard;
import de.moinmorni.bollwerk.data.PlayerData;
import de.moinmorni.bollwerk.data.Store;
import de.moinmorni.bollwerk.game.Game;
import de.moinmorni.bollwerk.game.Games;
import de.moinmorni.bollwerk.hall.Hall;
import de.moinmorni.bollwerk.listen.GameListener;
import de.moinmorni.bollwerk.listen.GuardListener;
import de.moinmorni.bollwerk.listen.PlayerListener;
import de.moinmorni.bollwerk.menu.MenuListener;
import de.moinmorni.bollwerk.net.Network;
import de.moinmorni.bollwerk.world.Construction;
import de.moinmorni.bollwerk.world.MapType;
import de.moinmorni.bollwerk.world.VoidGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Morni Bollwerk: verteidigt das Dorf gegen Welle um Welle – zwischen den Wellen aufrüsten und bauen. 1–6 Leute
 * zusammen, zwei Karten (Talsperre Morni-Dorf, Klosterhof). Spricht mit dem MoinMorni-Netzwerk, wenn es da ist, und
 * läuft sonst allein.
 */
public final class BollwerkPlugin extends JavaPlugin {

    private static final String ADMIN = "moinmorni.bollwerk.admin";
    private static final long SECOND = 20L;
    private static final long LANGUAGE_CHECK = 40L;
    private static final long STATE_REFRESH = 20L * 60;
    private static final long BOARD_REFRESH = 20L * 30;
    private static BollwerkPlugin instance;

    private Settings settings;
    private Network net;
    private Store store;
    private Leaderboard board;
    private World world;
    private Construction construction;
    private Hall hall;
    private Games games;
    private Boards boards;
    private volatile boolean ready;
    private boolean stateQueued;
    private String lastState = "";
    private int lastPlayers = -1;
    private final Map<UUID, String> lastLang = new HashMap<>();

    public static BollwerkPlugin instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        Items.init(this);
        net = Network.connect(this);
        getLogger().info(net.connected()
                ? "Mit dem MoinMorni-Netzwerk verbunden (Server „" + net.serverName() + "“)."
                : "Kein Netzwerk-Plugin: Bollwerk läuft allein (deutsche Texte, Statistik nur lokal).");
        store = new Store(getLogger(), getDataFolder().toPath());
        board = loadBoard();
        world = createWorld(settings.world());
        boards = new Boards(this);
        hall = new Hall(this);
        games = new Games(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new GameListener(this), this);
        pm.registerEvents(new GuardListener(this), this);
        Commands commands = new Commands(this);
        for (String name : List.of("bollwerk", "shop", "bereit", "karte", "zuschauen", "verlassen", "stats", "top")) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(commands);
                c.setTabCompleter(commands);
            }
        }

        construction = new Construction(this, world);
        construction.ensure(false, () -> {
            ready = true;
            getLogger().info("Bollwerk ist bereit: " + MapType.values().length + " Karten.");
        });
        boards.spawnHall();

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, games::tick, 1L, 1L);
        scheduler.runTaskTimer(this, () -> {
            hall.tick();
            boards.hallAll();
        }, SECOND, SECOND);
        scheduler.runTaskTimer(this, this::checkLanguages, LANGUAGE_CHECK, LANGUAGE_CHECK);
        scheduler.runTaskTimer(this, () -> reportState(true), STATE_REFRESH, STATE_REFRESH);
        scheduler.runTaskTimer(this, boards::refreshHall, BOARD_REFRESH, BOARD_REFRESH);

        for (Player p : getServer().getOnlinePlayers()) {
            data(p);
            hall.enter(p);
        }
        stateChanged();
    }

    @Override
    public void onDisable() {
        if (games != null) {
            games.abortAll();
        }
        if (boards != null) {
            boards.removeHall();
        }
        Holo.removeAll();
        if (store != null) {
            if (board != null) {
                store.write(boardFile(), board.toYaml());
            }
            store.close();
        }
        instance = null;
    }

    private World createWorld(String name) {
        World w = getServer().getWorld(name);
        if (w == null) {
            w = new WorldCreator(name).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (w == null) {
            throw new IllegalStateException("Welt " + name + " ließ sich nicht anlegen");
        }
        w.setGameRule(GameRules.ADVANCE_TIME, false);
        w.setGameRule(GameRules.ADVANCE_WEATHER, false);
        w.setGameRule(GameRules.SPAWN_MOBS, false);
        w.setGameRule(GameRules.SPAWN_MONSTERS, false);
        w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        w.setGameRule(GameRules.SPAWN_PATROLS, false);
        w.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        w.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        w.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRules.MOB_GRIEFING, false);
        w.setGameRule(GameRules.BLOCK_DROPS, false);
        w.setGameRule(GameRules.ENTITY_DROPS, false);
        w.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, true);
        w.setTime(6000L);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.NORMAL);
        w.setSpawnLocation(new org.bukkit.Location(w, 0.5, MapType.FLOOR_Y + 1, 8.5));
        return w;
    }

    private Path boardFile() {
        return getDataFolder().toPath().resolve("bestenliste.yml");
    }

    private Leaderboard loadBoard() {
        Path f = boardFile();
        if (Files.isRegularFile(f)) {
            try {
                return Leaderboard.fromYaml(Files.readString(f, StandardCharsets.UTF_8));
            } catch (IOException | IllegalArgumentException e) {
                getLogger().warning("bestenliste.yml ließ sich nicht lesen: " + e.getMessage());
            }
        }
        return new Leaderboard();
    }

    public void saveBoard() {
        store.write(boardFile(), board.toYaml());
        boards.refreshHall();
    }

    private void checkLanguages() {
        for (Player p : getServer().getOnlinePlayers()) {
            String lang = lang(p);
            String before = lastLang.put(p.getUniqueId(), lang);
            if (before != null && !before.equals(lang)) {
                Holo.refresh(this, p, lang);
                if (hall.inHall(p.getUniqueId())) {
                    hall.items(p, lang);
                }
            }
        }
    }

    void forgetLanguage(UUID uuid) {
        lastLang.remove(uuid);
    }

    // ---------- Zugriff ----------

    public Settings settings() {
        return settings;
    }

    public Network net() {
        return net;
    }

    public Store store() {
        return store;
    }

    public Leaderboard board() {
        return board;
    }

    public World world() {
        return world;
    }

    public Hall hall() {
        return hall;
    }

    public Games games() {
        return games;
    }

    public Boards boards() {
        return boards;
    }

    public boolean ready() {
        return ready;
    }

    public String lang(Player p) {
        return net.language(p.getUniqueId());
    }

    public PlayerData data(Player p) {
        return store.get(p.getUniqueId(), p.getName());
    }

    public boolean builder(Player p) {
        return p.getGameMode() == org.bukkit.GameMode.CREATIVE && p.hasPermission(ADMIN);
    }

    /** Aus der Wachstube einer Runde zuschauen. */
    public void spectate(Player p, Game g) {
        hall.leave(p.getUniqueId());
        g.spectate(p);
    }

    public void stateChanged() {
        if (stateQueued) {
            return;
        }
        stateQueued = true;
        getServer().getScheduler().runTask(this, () -> {
            stateQueued = false;
            reportState(false);
        });
    }

    private void reportState(boolean force) {
        String state = games.all().isEmpty() ? "waiting" : "running";
        int players = getServer().getOnlinePlayers().size();
        if (force || !state.equals(lastState) || players != lastPlayers) {
            lastState = state;
            lastPlayers = players;
            net.state(state, players);
        }
    }

    void rebuild(Runnable done) {
        if (construction.running()) {
            return;
        }
        ready = false;
        games.abortAll();
        construction.ensure(true, () -> {
            ready = true;
            done.run();
        });
        boards.spawnHall();
        for (Player p : getServer().getOnlinePlayers()) {
            hall.enter(p);
        }
    }

    void reloadSettings() {
        reloadConfig();
        settings = Settings.load(getConfig());
    }
}
