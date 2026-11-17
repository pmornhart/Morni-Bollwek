package de.moinmorni.bollwerk.game;

import de.moinmorni.bollwerk.BollwerkPlugin;
import de.moinmorni.bollwerk.Holo;
import de.moinmorni.bollwerk.Items;
import de.moinmorni.bollwerk.Settings;
import de.moinmorni.bollwerk.Texts;
import de.moinmorni.bollwerk.Ui;
import de.moinmorni.bollwerk.logic.DefenceType;
import de.moinmorni.bollwerk.logic.Economy;
import de.moinmorni.bollwerk.logic.EnemyType;
import de.moinmorni.bollwerk.logic.Heart;
import de.moinmorni.bollwerk.logic.Offer;
import de.moinmorni.bollwerk.logic.Targeting;
import de.moinmorni.bollwerk.logic.WavePlan;
import de.moinmorni.bollwerk.world.MapLayout;
import de.moinmorni.bollwerk.world.MapType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Eine Runde auf einer Karte: Aufbauphase, Welle, Aufbauphase … bis das Herz fällt. Läuft im Takt des Servers
 * ({@link #tick()} jeden Tick).
 *
 * <p>Verbindung weg mitten in der Runde: Der Platz bleibt {@code rueckkehr-sekunden} frei, die anderen verteidigen
 * weiter. Kommt die Person rechtzeitig zurück, steht sie genau dort, wo sie war, mit Ausrüstung und Leben (lag sie am
 * Boden, ist sie raus wie bisher). Sind alle weg, hält die Runde an (Gegner eingefroren). Danach gilt es wie bisher.
 */
public final class Game {

    public enum Phase {
        PREPARE, WAVE, OVER
    }

    /** Markierung aller Gegner (für Befehle wie {@code /kill @e[tag=bollwerk_enemy]}). */
    public static final String ENEMY_TAG = "bollwerk_enemy";
    public static final String ARROW_TAG = "bollwerk_tower_arrow";
    /** Markierung der Krämerin. */
    public static final String SHOP_TAG = "bollwerk_shop";
    private static final int SECOND = 20;
    private static final int AI_EVERY = 10;
    private static final int BOARD_EVERY = 10;
    private static final double STEP_REACHED = 2.2;
    private static final double PLAYER_CHASE = 6;
    private static final double WALL_REACH = 2.8;
    private static final double BREAKER_REACH = 3.6;
    private static final double REVIVE_REACH = 2.6;
    private static final int STUCK_LIMIT = 16;
    private static final int HEART_LOW_PERCENT = 25;

    private final BollwerkPlugin plugin;
    private final MapType map;
    private final MapLayout layout;
    private final World world;
    private final Settings settings;
    private final Map<UUID, Fighter> fighters = new LinkedHashMap<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final Map<UUID, Enemy> enemies = new LinkedHashMap<>();
    private final Map<Integer, Defence> defences = new HashMap<>();
    private final Map<Integer, Holo> markers = new HashMap<>();
    private final Deque<EnemyType> toSpawn = new ArrayDeque<>();
    private final Heart heart;
    private final LangBar heartBar = new LangBar(BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10);
    private final LangBar waveBar = new LangBar(BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    private final long started = System.currentTimeMillis();
    private Phase phase = Phase.PREPARE;
    private int phaseTicks;
    private int wave;
    private int cleared;
    private WavePlan plan;
    private int spawnCooldown;
    private int laneCursor;
    private long tick;
    private int towerShots;
    private int towerKills;
    private int stuckTeleports;
    private final int[] heartArrivals = new int[8];
    private int waveTotal;
    private boolean heartWarned;
    private boolean lowWarned;
    private EnemyType bossType;
    private final Map<Integer, double[]> spotProgress = new HashMap<>();
    private org.bukkit.entity.Villager shopkeeper;
    /** Verbindung weg: Platz frei bis … (Millisekunden) und Stand beim Gehen. */
    private final Map<UUID, Away> away = new LinkedHashMap<>();
    /** Alle weg: angehalten (Gegner eingefroren, keine Zeit läuft). */
    private boolean paused;

    private record Away(long until, PlayerState state) {
    }

    Game(BollwerkPlugin plugin, MapType map, List<Player> players) {
        this.plugin = plugin;
        this.map = map;
        this.layout = map.layout();
        this.world = plugin.world();
        this.settings = plugin.settings();
        this.heart = new Heart(settings.heartHealth());
        for (MapLayout.Spot s : layout.spots()) {
            if (s.lane() >= 0) {
                MapLayout.Lane lane = layout.lanes().get(s.lane());
                spotProgress.put(s.id(), new double[] {lane.progressAt(s.x() + 0.5, s.z() + 0.5)});
            }
        }
        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                world.getChunkAt((map.originX() >> 4) + cx, (map.originZ() >> 4) + cz).addPluginChunkTicket(plugin);
            }
        }
        for (MapLayout.Spot s : layout.spots()) {
            marker(s);
        }
        phaseTicks = settings.firstPrepare() * SECOND;
        Location shop = loc(layout.shop());
        shop.setYaw(layout.spawnYaw());
        shopkeeper = world.spawn(shop, org.bukkit.entity.Villager.class, v -> {
            v.addScoreboardTag(SHOP_TAG);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setPersistent(false);
            v.setProfession(org.bukkit.entity.Villager.Profession.FARMER);
            v.customName(Component.text("⚖ Krämerin · Shop", net.kyori.adventure.text.format.TextColor.color(Texts.GOLD)));
            v.setCustomNameVisible(true);
        });
        for (Player p : players) {
            Fighter f = new Fighter(p.getUniqueId(), p.getName(), settings.startGold());
            fighters.put(p.getUniqueId(), f);
            place(p, f, true);
            plugin.net().playing(p, true);
            p.sendMessage(Texts.msg(plugin.lang(p), "game.welcome", Placeholder.unparsed("heart", heartName(plugin.lang(p))),
                    Placeholder.unparsed("secs", String.valueOf(settings.firstPrepare()))));
        }
        updateBars();
    }

    // ---------- Zugriff ----------

    public MapType map() {
        return map;
    }

    public Phase phase() {
        return phase;
    }

    public int wave() {
        return wave;
    }

    public int cleared() {
        return cleared;
    }

    public Heart heart() {
        return heart;
    }

    public Map<UUID, Fighter> fighters() {
        return fighters;
    }

    public Fighter fighter(UUID uuid) {
        return fighters.get(uuid);
    }

    public boolean spectating(UUID uuid) {
        return spectators.contains(uuid);
    }

    public int enemyCount() {
        return enemies.size() + toSpawn.size();
    }

    public boolean isEnemy(Entity e) {
        return enemies.containsKey(e.getUniqueId());
    }

    public int playerCount() {
        return (int) fighters.values().stream().filter(Fighter::online).count();
    }

    String heartName(String lang) {
        return Texts.plain(lang, "heart." + map.id());
    }

    Location loc(double x, double y, double z) {
        return new Location(world, map.originX() + x, MapType.FLOOR_Y + y, map.originZ() + z);
    }

    Location loc(MapLayout.Point p) {
        return loc(p.x(), p.y(), p.z());
    }

    Location heartLoc() {
        return loc(layout.heart());
    }

    /** Wo man nach einem (eigentlich unmöglichen) Tod wieder auftaucht. */
    public Location respawnPoint() {
        return spawnLoc();
    }

    Location spawnLoc() {
        Location l = loc(layout.spawn());
        l.setYaw(layout.spawnYaw());
        return l;
    }

    /** Für /bollwerk status (Tests, Verwaltung). */
    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "karte=%s phase=%s welle=%d geschafft=%d herz=%.0f/%.0f gegner=%d spawn=%d "
                + "bauten=%d turm_schuesse=%d turm_kills=%d festgesteckt=%d angehalten=%s weg=%d", map.id(), phase, wave,
                cleared, heart.health(), heart.max(), enemies.size(), toSpawn.size(), defences.size(), towerShots, towerKills,
                stuckTeleports, paused, away.size()));
        for (int i = 0; i < layout.lanes().size(); i++) {
            sb.append(" am_herz_").append(layout.lanes().get(i).id()).append('=').append(heartArrivals[i]);
        }
        for (Defence d : defences.values()) {
            sb.append(String.format(Locale.ROOT, " bau%d=%s:%d:%.0f:%d", d.spot.id(), d.type.id(), d.level, d.health, d.uses));
        }
        for (Fighter f : fighters.values()) {
            sb.append(String.format(Locale.ROOT, " %s=%s:%d:%d:%d", f.name(), f.state(), f.wallet().balance(), f.kills(),
                    f.revives()));
        }
        return sb.toString();
    }

    // ---------- Spieler ----------

    /** Auf die Karte setzen (Start, Rückkehr, neue Aufbauphase). */
    private void place(Player p, Fighter f, boolean starter) {
        String lang = plugin.lang(p);
        p.teleport(spawnLoc());
        p.setGameMode(GameMode.ADVENTURE);
        p.setFlying(false);
        p.setAllowFlight(false);
        p.setInvulnerable(false);
        p.setPose(Pose.STANDING, false);
        p.clearActivePotionEffects();
        AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(10);
        p.setFireTicks(0);
        Kit.give(p, f, lang, starter);
        Kit.buttons(p, lang, phase == Phase.PREPARE);
        f.state(Fighter.State.ALIVE);
        heartBar.show(p, lang);
        waveBar.show(p, lang);
        Holo.refresh(plugin, p, lang);
    }

    /**
     * Wiederkommen nach Verbindungsabbruch oder als Nachzügler aus der Party. Ist der Platz noch frei und stand die
     * Person beim Gehen, geht es genau dort weiter (Ort, Ausrüstung, Leben); sonst wie bisher (Aufbauphase: auf die
     * Karte, Welle: zuschauen bis zur nächsten Aufbauphase).
     */
    public void rejoin(Player p, boolean late) {
        Fighter f = fighters.get(p.getUniqueId());
        Away back = away.remove(p.getUniqueId());
        if (back != null) {
            plugin.net().holdSlot(p.getUniqueId(), 0);
        }
        plugin.net().playing(p, true);
        if (back != null && f != null && f.state() == Fighter.State.ALIVE && phase != Phase.OVER) {
            f.online(true);
            String lang = plugin.lang(p);
            back.state().restore(p);
            p.setFallDistance(0);
            Kit.buttons(p, lang, phase == Phase.PREPARE);
            heartBar.show(p, lang);
            waveBar.show(p, lang);
            Holo.refresh(plugin, p, lang);
            Ui.title(p, Texts.text(lang, "game.rejoin.title"), Texts.text(lang, "game.rejoin.sub"), 2500);
            p.sendMessage(Texts.msg(lang, "game.rejoin"));
            Fighter me = f;
            broadcastExcept(p, l -> Texts.msg(l, "game.returned", Placeholder.unparsed("player", me.name())));
            return;
        }
        if (f == null) {
            int gold = settings.startGold() + cleared * Economy.waveBonus(Math.max(1, cleared)) / 2;
            f = new Fighter(p.getUniqueId(), p.getName(), gold);
            fighters.put(p.getUniqueId(), f);
            Fighter nf = f;
            broadcast(lang -> Texts.msg(lang, "game.late", Placeholder.unparsed("player", nf.name())));
        }
        f.online(true);
        if (phase == Phase.PREPARE) {
            place(p, f, late || p.getInventory().isEmpty());
        } else {
            out(p, f, false);
            if (back != null) {
                p.sendMessage(Texts.msg(plugin.lang(p), "game.rejoin.out"));
            }
        }
    }

    /** Verbindung weg: mitten in der Runde bleibt der Platz {@code rueckkehr-sekunden} frei (am Boden = raus wie bisher). */
    public void quit(Player p) {
        Fighter f = fighters.get(p.getUniqueId());
        spectators.remove(p.getUniqueId());
        if (f != null) {
            int seconds = plugin.settings().returnSeconds();
            if (seconds > 0 && phase != Phase.OVER && f.online() && !away.containsKey(p.getUniqueId())) {
                away.put(p.getUniqueId(), new Away(System.currentTimeMillis() + seconds * 1000L, PlayerState.of(p)));
                plugin.net().holdSlot(p.getUniqueId(), seconds);
                broadcastExcept(p, lang -> Texts.msg(lang, "game.away", Placeholder.unparsed("player", f.name()),
                        Placeholder.unparsed("secs", String.valueOf(seconds))));
            }
            f.online(false);
            if (f.state() == Fighter.State.DOWNED) {
                f.state(Fighter.State.OUT);
            }
        }
        heartBar.hide(p);
        waveBar.hide(p);
    }

    /** Frist um: der Platz ist nicht mehr frei – danach wie bisher (später zurück: Aufbauphase oder zuschauen). */
    private void checkAway() {
        if (away.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (UUID u : List.copyOf(away.keySet())) {
            Away a = away.get(u);
            if (a == null || a.until() > now) {
                continue;
            }
            away.remove(u);
            plugin.net().holdSlot(u, 0);
            Fighter f = fighters.get(u);
            if (f != null) {
                broadcast(lang -> Texts.msg(lang, "game.away.gone", Placeholder.unparsed("player", f.name())));
            }
        }
    }

    /** Runde vorbei: niemandem mehr einen Platz frei halten. */
    private void releaseAway() {
        for (UUID u : List.copyOf(away.keySet())) {
            away.remove(u);
            plugin.net().holdSlot(u, 0);
        }
    }

    /** Alle weg: Gegner einfrieren. */
    private void pause() {
        if (paused) {
            return;
        }
        paused = true;
        for (Enemy e : enemies.values()) {
            e.mob.setAI(false);
        }
    }

    /** Jemand ist zurück: Gegner wieder los. */
    private void resume() {
        if (!paused) {
            return;
        }
        paused = false;
        if (phase == Phase.OVER) {
            return;
        }
        for (Enemy e : enemies.values()) {
            e.mob.setAI(true);
        }
    }

    /** Angehalten, weil alle weg sind (für /bollwerk status und Tests)? */
    public boolean paused() {
        return paused;
    }

    /** Freiwillig gehen (/verlassen): zählt als gespielt. */
    public void leave(Player p) {
        Fighter f = fighters.remove(p.getUniqueId());
        spectators.remove(p.getUniqueId());
        if (away.remove(p.getUniqueId()) != null) {
            plugin.net().holdSlot(p.getUniqueId(), 0);
        }
        plugin.net().playing(p, false);
        heartBar.hide(p);
        waveBar.hide(p);
        if (f != null) {
            report(f, false);
            broadcast(lang -> Texts.msg(lang, "game.left", Placeholder.unparsed("player", f.name())));
        }
    }

    public void spectate(Player p) {
        spectators.add(p.getUniqueId());
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(heartLoc().add(0, 12, 8).setDirection(new Vector(0, -0.8, -1)));
        String lang = plugin.lang(p);
        heartBar.show(p, lang);
        waveBar.show(p, lang);
        Holo.refresh(plugin, p, lang);
        p.sendMessage(Texts.msg(lang, "spectate.start", Placeholder.unparsed("map", Texts.plain(lang, "map." + map.id()))));
    }

    public List<UUID> spectators() {
        return spectators;
    }

    /** Alle, die gerade etwas von dieser Runde sehen (online). */
    public List<Player> audience() {
        List<Player> out = new ArrayList<>();
        for (UUID u : fighters.keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && fighters.get(u).online()) {
                out.add(p);
            }
        }
        for (UUID u : spectators) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    void broadcast(Function<String, Component> msg) {
        for (Player p : audience()) {
            p.sendMessage(msg.apply(plugin.lang(p)));
        }
    }

    private void broadcastExcept(Player except, Function<String, Component> msg) {
        for (Player p : audience()) {
            if (!p.equals(except)) {
                p.sendMessage(msg.apply(plugin.lang(p)));
            }
        }
    }

    private void titleAll(String main, String sub, TagResolver... r) {
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, main, r), sub == null ? Component.empty() : Texts.text(lang, sub, r), 2500);
        }
    }

    private void soundAll(String key, float pitch) {
        for (Player p : audience()) {
            Ui.sound(p, key, pitch);
        }
    }

    // ---------- Takt ----------

    /** Jeden Tick. Liefert {@code false}, wenn die Runde vorbei und aufgeräumt ist. */
    public boolean tick() {
        tick++;
        checkAway();
        if (fighters.values().stream().noneMatch(Fighter::online) && phase != Phase.OVER) {
            if (!away.isEmpty()) {
                pause(); // alle weg, aber jemand darf noch zurückkommen: die Runde wartet
                return true;
            }
            broadcast(lang -> Texts.msg(lang, "game.abandoned"));
            gameOver(true);
        }
        resume();
        switch (phase) {
            case PREPARE -> tickPrepare();
            case WAVE -> tickWave();
            case OVER -> {
                if (--phaseTicks <= 0) {
                    close();
                    return false;
                }
            }
            default -> {
            }
        }
        if (phase != Phase.OVER) {
            tickFighters();
        }
        if (tick % BOARD_EVERY == 0) {
            updateBars();
            plugin.boards().game(this);
        }
        return true;
    }

    private void tickPrepare() {
        phaseTicks--;
        long online = fighters.values().stream().filter(Fighter::online).count();
        long ready = fighters.values().stream().filter(f -> f.online() && f.ready()).count();
        if (phaseTicks > 3 * SECOND && online > 0 && ready >= online) {
            phaseTicks = 3 * SECOND;
        }
        if (phaseTicks > 0 && phaseTicks <= 3 * SECOND && phaseTicks % SECOND == 0) {
            soundAll("minecraft:block.note_block.hat", 1.2f);
        }
        if (phaseTicks <= 0) {
            startWave(wave + 1);
        }
        tickBuildings(false);
    }

    /** Welle starten (auch /bollwerk welle n). */
    public void startWave(int n) {
        wave = Math.max(1, n);
        int players = (int) Math.max(1, fighters.values().stream().filter(Fighter::online).count());
        plan = WavePlan.compose(wave, players);
        toSpawn.clear();
        toSpawn.addAll(plan.enemies());
        waveTotal = toSpawn.size();
        spawnCooldown = SECOND;
        phase = Phase.WAVE;
        heartWarned = false;
        bossType = EnemyType.bossFor(wave);
        fighters.values().forEach(f -> f.ready(false));
        for (Player p : audience()) {
            if (fighters.containsKey(p.getUniqueId())) {
                Kit.buttons(p, plugin.lang(p), false);
            }
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Shop) {
                p.closeInventory();
            }
        }
        titleAll("game.wave.title", "game.wave.sub", Placeholder.unparsed("n", String.valueOf(wave)),
                Placeholder.unparsed("count", String.valueOf(waveTotal)),
                Placeholder.unparsed("lanes", String.valueOf(layout.lanes().size())));
        soundAll("minecraft:event.raid.horn", 1f);
        if (bossType != null) {
            EnemyType b = bossType;
            broadcast(lang -> Texts.msg(lang, "boss.coming", Placeholder.unparsed("name", Texts.plain(lang, "enemy." + b.id()))));
        }
        for (EnemyType t : EnemyType.values()) {
            if (!t.boss() && t.firstWave() == wave && t.firstWave() > 1) {
                broadcast(lang -> Texts.msg(lang, "enemy.new", Placeholder.unparsed("name", Texts.plain(lang, "enemy." + t.id())),
                        Placeholder.unparsed("desc", Texts.plain(lang, "enemy." + t.id() + ".desc"))));
            }
        }
        plugin.stateChanged();
    }

    private void tickWave() {
        if (!toSpawn.isEmpty() && --spawnCooldown <= 0) {
            EnemyType t = toSpawn.pollFirst();
            int lane = t.boss() ? 0 : laneCursor++ % layout.lanes().size();
            spawn(t, lane, null, 1);
            spawnCooldown = plan.spawnTicks();
        }
        if (tick % AI_EVERY == 0) {
            for (Enemy e : List.copyOf(enemies.values())) {
                think(e);
                if (phase != Phase.WAVE) {
                    return;
                }
            }
        }
        tickBuildings(true);
        if (toSpawn.isEmpty() && enemies.isEmpty()) {
            waveCleared();
        }
    }

    private void waveCleared() {
        cleared = wave;
        phase = Phase.PREPARE;
        phaseTicks = settings.prepare() * SECOND;
        lowWarned = false;
        int bonus = Economy.waveBonus(wave);
        for (Fighter f : fighters.values()) {
            f.wallet().earn(bonus);
            f.ready(false);
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null || !f.online()) {
                continue;
            }
            if (f.state() != Fighter.State.ALIVE) {
                place(p, f, false);
                p.sendMessage(Texts.msg(plugin.lang(p), "game.respawn"));
            } else {
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                p.setHealth(max == null ? 20 : max.getValue());
                Kit.buttons(p, plugin.lang(p), true);
            }
        }
        titleAll("game.cleared.title", "game.cleared.sub", Placeholder.unparsed("n", String.valueOf(wave)),
                Placeholder.unparsed("amount", String.valueOf(bonus)),
                Placeholder.unparsed("secs", String.valueOf(settings.prepare())));
        soundAll("minecraft:ui.toast.challenge_complete", 1f);
        plugin.getLogger().info("Bollwerk " + map.id() + ": Welle " + wave + " geschafft.");
    }

    // ---------- Gegner ----------

    /** Gegner erscheinen lassen (am Anfang eines Wegs oder bei {@code at}). */
    Mob spawn(EnemyType t, int lane, Location at, int next) {
        MapLayout.Lane l = layout.lanes().get(lane);
        Location where = at != null ? at : loc(l.start());
        Class<? extends Mob> cls = switch (t) {
            case ARMBRUSTSCHUETZE -> Pillager.class;
            case BRECHER -> Vindicator.class;
            case LAWINENRIESE -> Ravager.class;
            case NEBELHEXE -> Witch.class;
            default -> Zombie.class;
        };
        double scale = plan == null ? 1 : plan.damageScale();
        double hp = plan == null ? t.health() : plan.health(t);
        Mob mob = world.spawn(where, cls, m -> {
            m.addScoreboardTag(ENEMY_TAG);
            m.addScoreboardTag("bollwerk_" + t.id());
            m.setRemoveWhenFarAway(false);
            m.setPersistent(false);
            m.setCanPickupItems(false);
            setBase(m, Attribute.MAX_HEALTH, hp, false);
            m.setHealth(hp);
            setBase(m, Attribute.MOVEMENT_SPEED, t.speed(), true);
            setBase(m, Attribute.FOLLOW_RANGE, 48, false);
            setBase(m, Attribute.ATTACK_DAMAGE, scale, true);
            if (t == EnemyType.SCHILDTRAEGER || t.boss()) {
                setBase(m, Attribute.KNOCKBACK_RESISTANCE, t.boss() ? 0.9 : 0.5, false);
            }
            if (t == EnemyType.LAWINENRIESE) {
                setBase(m, Attribute.SCALE, 1.25, false);
            }
            if (m instanceof Zombie z) {
                z.setShouldBurnInDay(false);
                z.setBaby(t == EnemyType.FLINKER);
            }
            if (m instanceof org.bukkit.entity.Raider r) {
                r.setCanJoinRaid(false);
                r.setPatrolLeader(false);
            }
            if (t.boss()) {
                m.customName(Component.text(Texts.plain("de", "enemy." + t.id())).color(
                        net.kyori.adventure.text.format.TextColor.color(t == EnemyType.LAWINENRIESE ? 0xDDEEFF : 0xB07CFF)));
                m.setCustomNameVisible(true);
            }
            dress(m, t);
        });
        Enemy e = new Enemy(mob, t, lane, scale);
        e.next = Math.max(1, Math.min(l.path().size() - 1, next));
        enemies.put(mob.getUniqueId(), e);
        return mob;
    }

    private static void setBase(Mob m, Attribute a, double value, boolean multiply) {
        AttributeInstance inst = m.getAttribute(a);
        if (inst != null) {
            inst.setBaseValue(multiply ? inst.getBaseValue() * value : value);
        }
    }

    private static void dress(Mob m, EnemyType t) {
        EntityEquipment eq = m.getEquipment();
        eq.setItemInMainHandDropChance(0);
        eq.setItemInOffHandDropChance(0);
        eq.setHelmetDropChance(0);
        eq.setChestplateDropChance(0);
        eq.setLeggingsDropChance(0);
        eq.setBootsDropChance(0);
        switch (t) {
            case PLUENDERER -> {
                eq.setItemInMainHand(new ItemStack(Material.IRON_AXE));
                eq.setHelmet(leather(Material.LEATHER_HELMET, 0x6B4423));
                eq.setChestplate(leather(Material.LEATHER_CHESTPLATE, 0x3B2A1A));
            }
            case FLINKER -> {
                eq.setItemInMainHand(new ItemStack(Material.WOODEN_SWORD));
                eq.setHelmet(leather(Material.LEATHER_HELMET, 0x5A7D2A));
            }
            case SCHILDTRAEGER -> {
                eq.setItemInMainHand(new ItemStack(Material.STONE_SWORD));
                eq.setItemInOffHand(new ItemStack(Material.SHIELD));
                eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                eq.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            }
            case BRECHER -> eq.setItemInMainHand(new ItemStack(Material.IRON_AXE));
            default -> {
            }
        }
    }

    private static ItemStack leather(Material m, int rgb) {
        ItemStack item = new ItemStack(m);
        item.editMeta(LeatherArmorMeta.class, meta -> meta.setColor(Color.fromRGB(rgb)));
        return item;
    }

    /** Fortschritt eines Gegners auf seinem Weg (0 … 1). */
    private double progress(Enemy e) {
        Location l = e.mob.getLocation();
        return layout.lanes().get(e.lane).progressAt(l.getX() - map.originX(), l.getZ() - map.originZ());
    }

    /** Was tut ein Gegner als Nächstes? (alle {@link #AI_EVERY} Ticks) */
    private void think(Enemy e) {
        Mob m = e.mob;
        if (!m.isValid() || m.isDead()) {
            enemies.remove(m.getUniqueId());
            return;
        }
        e.attackCooldown = Math.max(0, e.attackCooldown - AI_EVERY);
        Location here = m.getLocation();
        MapLayout.Lane lane = layout.lanes().get(e.lane);
        if (here.getY() < MapType.FLOOR_Y - 30) {
            m.teleport(loc(lane.path().get(e.next)));
            return;
        }
        if (e.type == EnemyType.NEBELHEXE) {
            e.summonCooldown -= AI_EVERY;
            if (e.summonCooldown <= 0) {
                e.summonCooldown = 8 * SECOND;
                for (int i = 0; i < 2; i++) {
                    spawn(EnemyType.FLINKER, e.lane, here.clone().add(i - 0.5, 0, 0.5), e.next);
                }
                broadcast(lang -> Texts.msg(lang, "boss.summon", Placeholder.unparsed("name", Texts.plain(lang, "enemy.nebelhexe"))));
            }
        }
        // 1. Spieler in der Nähe? Nahkämpfer gehen auf sie los – aber nicht seitlich um eine stehende Palisade herum:
        //    Wer dahinter steht, ist geschützt, bis sie zerschlagen ist.
        double prog = progress(e);
        Defence wall = blockingWall(e, prog);
        Player victim = nearestStanding(here, e.type.ranged() ? 12 : PLAYER_CHASE);
        if (victim != null && wall != null && behind(victim, wall, lane)) {
            victim = null;
        }
        if (victim != null && !e.type.wallBreaker()) {
            if (m.getTarget() != victim) {
                m.setTarget(victim);
            }
            return;
        }
        if (m.getTarget() != null && (victim == null || e.type.wallBreaker())) {
            m.setTarget(null);
        }
        // 2. Bauten im Weg: Palisade auf dem eigenen Weg vor einem, Brecher nehmen jeden Bau in der Nähe.
        Defence target = wall;
        if (e.type.wallBreaker()) {
            Defence near = nearestDefence(here, BREAKER_REACH);
            if (near != null) {
                target = near;
            }
        }
        if (target != null && flat(here, target.center()) <= (target == wall ? WALL_REACH : BREAKER_REACH)) {
            m.getPathfinder().stopPathfinding();
            m.lookAt(target.center().add(0, 1, 0));
            if (e.attackCooldown == 0) {
                e.attackCooldown = SECOND;
                hitDefence(target, DefenceType.hit(e.type, e.damageScale), m);
            }
            e.lastPos = here;
            e.stuckChecks = 0;
            return;
        }
        // 3. Am Herzen? Zuschlagen bzw. schießen.
        double toHeart = flat(here, heartLoc());
        double reach = e.type.ranged() ? MapLayout.RANGED_REACH : layout.heartReach();
        boolean atEnd = e.next == lane.path().size() - 1 && flat(here, loc(lane.path().get(e.next))) < 1.5;
        if (toHeart <= reach || atEnd) {
            m.getPathfinder().stopPathfinding();
            m.lookAt(heartLoc().add(0, 1, 0));
            if (!e.arrived) {
                e.arrived = true;
                heartArrivals[e.lane]++;
            }
            if (e.attackCooldown == 0) {
                e.attackCooldown = e.type.ranged() ? 2 * SECOND : SECOND;
                hitHeart(e);
            }
            e.lastPos = here;
            e.stuckChecks = 0;
            return;
        }
        // 4. Laufen: zum nächsten Wegpunkt (oder vor die Palisade).
        while (e.next < lane.path().size() - 1 && flat(here, loc(lane.path().get(e.next))) < STEP_REACHED) {
            e.next++;
        }
        Location goal;
        if (wall != null) {
            double front = spotProgress.get(wall.spot.id())[0] * lane.length() - 1.6;
            goal = loc(lane.pointAt(front));
        } else {
            goal = loc(lane.path().get(e.next));
        }
        m.getPathfinder().moveTo(goal, 1.0);
        if (here.distanceSquared(e.lastPos) < 0.09) {
            if (++e.stuckChecks >= STUCK_LIMIT) {
                m.teleport(goal);
                e.stuckChecks = 0;
                stuckTeleports++;
            }
        } else {
            e.stuckChecks = 0;
        }
        e.lastPos = here;
    }

    private static double flat(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private Player nearestStanding(Location at, double range) {
        Player best = null;
        double bestD = range * range;
        for (Fighter f : fighters.values()) {
            if (f.state() != Fighter.State.ALIVE || !f.online()) {
                continue;
            }
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null || p.getWorld() != at.getWorld()) {
                continue;
            }
            double d = p.getLocation().distanceSquared(at);
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** Steht der Spieler (auf dem Weg gesehen) hinter der Palisade? */
    private boolean behind(Player p, Defence wall, MapLayout.Lane lane) {
        Location l = p.getLocation();
        return lane.progressAt(l.getX() - map.originX(), l.getZ() - map.originZ()) > spotProgress.get(wall.spot.id())[0];
    }

    /** Erste stehende Palisade auf dem Weg vor dem Gegner. */
    private Defence blockingWall(Enemy e, double progress) {
        Defence best = null;
        double bestP = 2;
        for (Defence d : defences.values()) {
            if (d.type != DefenceType.PALISADE || d.spot.lane() != e.lane) {
                continue;
            }
            double p = spotProgress.get(d.spot.id())[0];
            if (p + 0.01 >= progress && p < bestP) {
                bestP = p;
                best = d;
            }
        }
        return best;
    }

    private Defence nearestDefence(Location at, double range) {
        Defence best = null;
        double bestD = range;
        for (Defence d : defences.values()) {
            double dist = flat(at, d.center());
            if (dist <= bestD) {
                bestD = dist;
                best = d;
            }
        }
        return best;
    }

    private void hitDefence(Defence d, double damage, Mob by) {
        by.swingMainHand();
        d.health -= damage;
        world.playSound(d.center(), "minecraft:entity.zombie.attack_wooden_door", 0.8f, 1f);
        world.spawnParticle(Particle.BLOCK, d.center().add(0, 1, 0), 12, 0.6, 0.6, 0.6,
                Material.SPRUCE_PLANKS.createBlockData());
        if (d.health <= 0) {
            destroy(d);
        } else {
            label(d);
        }
    }

    private void hitHeart(Enemy e) {
        double dmg = e.type.heartDamage() * e.damageScale;
        if (e.type.ranged()) {
            Location from = e.mob.getEyeLocation();
            Vector dir = heartLoc().add(0, 1.5, 0).toVector().subtract(from.toVector()).normalize();
            world.spawn(from.add(dir), Arrow.class, a -> {
                a.setVelocity(dir.multiply(1.6));
                a.setDamage(0);
                a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                a.addScoreboardTag(ARROW_TAG);
                a.setPersistent(false);
            });
        } else {
            e.mob.swingMainHand();
        }
        world.spawnParticle(Particle.DAMAGE_INDICATOR, heartLoc().add(0, 1.5, 0), 4, 0.6, 0.4, 0.6, 0.1);
        world.playSound(heartLoc(), map == MapType.KLOSTERHOF ? "minecraft:block.bell.use"
                : "minecraft:entity.zombie.attack_wooden_door", 0.6f, map == MapType.KLOSTERHOF ? 0.6f : 0.8f);
        damageHeart(dmg);
    }

    /** Verwaltung: laufende Welle abbrechen und Welle {@code n} sofort schicken. */
    public void forceWave(int n) {
        if (phase == Phase.OVER) {
            return;
        }
        enemies.values().forEach(e -> e.mob.remove());
        enemies.clear();
        toSpawn.clear();
        cleared = Math.max(cleared, n - 1);
        startWave(n);
    }

    /** Verwaltung: Leben des Herzens setzen (0 = es fällt). */
    public void setHeart(double value) {
        if (value <= 0) {
            damageHeart(heart.health());
        } else {
            heart.set(value);
        }
    }

    /** Schaden am Herzen (auch /bollwerk herz). */
    public void damageHeart(double dmg) {
        if (phase == Phase.OVER) {
            return;
        }
        boolean fell = heart.damage(dmg);
        if (!heartWarned) {
            heartWarned = true;
            broadcast(lang -> Texts.msg(lang, "game.heart.hit"));
        }
        if (!lowWarned && heart.fraction() * 100 <= HEART_LOW_PERCENT && !fell) {
            lowWarned = true;
            broadcast(lang -> Texts.msg(lang, "game.heart.low", Placeholder.unparsed("hp", String.valueOf((int) Math.ceil(heart.health())))));
            soundAll("minecraft:block.anvil.land", 0.6f);
        }
        if (fell) {
            gameOver(false);
        }
    }

    /** Ein Gegner ist gestorben (Ereignis). {@code killer}: Spieler oder null. */
    public void enemyDied(Entity entity, Player killer) {
        Enemy e = enemies.remove(entity.getUniqueId());
        if (e == null) {
            return;
        }
        int reward = Economy.killReward(e.type, Math.max(1, wave));
        Fighter f = killer == null ? null : fighters.get(killer.getUniqueId());
        if (f != null) {
            f.addKill();
            f.wallet().earn(reward);
            killer.sendActionBar(Texts.text(plugin.lang(killer), "game.kill", Placeholder.unparsed("n", String.valueOf(reward)),
                    Placeholder.unparsed("name", Texts.plain(plugin.lang(killer), "enemy." + e.type.id()))));
            Ui.sound(killer, "minecraft:entity.experience_orb.pickup", 1.2f);
        } else {
            if (tick - e.towerHitTick <= SECOND) {
                towerKills++;
            }
            int online = Math.max(1, (int) fighters.values().stream().filter(Fighter::online).count());
            int share = Economy.share(reward, online);
            fighters.values().stream().filter(Fighter::online).forEach(x -> x.wallet().earn(share));
        }
        if (e.type.boss()) {
            broadcast(lang -> Texts.msg(lang, "boss.down", Placeholder.unparsed("name", Texts.plain(lang, "enemy." + e.type.id()))));
            soundAll("minecraft:ui.toast.challenge_complete", 0.8f);
        }
    }

    // ---------- Bauten ----------

    private void marker(MapLayout.Spot s) {
        Location at = loc(s.x() + 0.5, s.y() + 1.6, s.z() + 0.5);
        String key = s.kind() == DefenceType.Spot.LANE ? "spot.marker.lane" : "spot.marker.side";
        markers.put(s.id(), Holo.spawn(plugin, at, lang -> Texts.text(lang, key), 0.8f));
    }

    private void label(Defence d) {
        Function<String, Component> text = lang -> Texts.text(lang, "defence." + d.type.id())
                .append(Component.text(" " + "★".repeat(d.level) + "☆".repeat(DefenceType.MAX_LEVEL - d.level),
                        net.kyori.adventure.text.format.TextColor.color(Texts.GOLD)))
                .append(Component.newline())
                .append(Component.text(Ui.bar(d.health / d.maxHealth(), 10) + " " + (int) Math.ceil(d.health),
                        net.kyori.adventure.text.format.TextColor.color(d.health / d.maxHealth() > 0.5 ? Texts.EMERALD : 0xE0533D)));
        if (d.label == null) {
            double top = d.type == DefenceType.PFEILTURM ? (d.level >= 2 ? 7 : 6) : 3.6;
            d.label = Holo.spawn(plugin, loc(d.spot.x() + 0.5, d.spot.y() + top, d.spot.z() + 0.5), text, 0.7f);
        } else {
            d.label.update(text);
        }
    }

    /** Bauplatz zu einem angeklickten Block (oder leer). */
    public Optional<MapLayout.Spot> spotNear(Block b, double range) {
        double rx = b.getX() + 0.5 - map.originX();
        double rz = b.getZ() + 0.5 - map.originZ();
        MapLayout.Spot best = null;
        double bestD = range;
        for (MapLayout.Spot s : layout.spots()) {
            double d = Math.hypot(s.x() + 0.5 - rx, s.z() + 0.5 - rz);
            if (d <= bestD) {
                bestD = d;
                best = s;
            }
        }
        return Optional.ofNullable(best);
    }

    Defence defenceAt(Block b) {
        for (Defence d : defences.values()) {
            if (d.contains(b)) {
                return d;
            }
        }
        return spotNear(b, 1.6).map(s -> defences.get(s.id())).orElse(null);
    }

    /** Für die Verwaltung: Stufe des Baus auf einem Platz (0 = leer). */
    public int defenceLevel(int spot) {
        Defence d = defences.get(spot);
        return d == null ? 0 : d.level;
    }

    /**
     * Rechtsklick auf einen Block der Karte: mit Bauplan bauen, sonst das Menü eines Baus öffnen. Liefert
     * {@code true}, wenn es um uns ging (dann wird der Klick sonst nicht verwendet).
     */
    public boolean rightClick(Player p, Block b, ItemStack item) {
        Fighter f = fighters.get(p.getUniqueId());
        if (f == null || f.state() != Fighter.State.ALIVE || b == null) {
            return false;
        }
        String lang = plugin.lang(p);
        String tag = Items.kitTag(item);
        if (tag != null && tag.startsWith("plan:")) {
            DefenceType type = DefenceType.byId(tag.substring("plan:".length()));
            if (type == null) {
                return true;
            }
            Optional<MapLayout.Spot> spot = spotNear(b, 2.6);
            if (spot.isEmpty()) {
                p.sendMessage(Texts.msg(lang, "defence.no-spot"));
                return true;
            }
            String error = build(p, spot.get(), type);
            if (error != null) {
                String where = Texts.plain(lang, type.spot() == DefenceType.Spot.LANE ? "spot.lane" : "spot.side");
                p.sendMessage(Texts.msg(lang, error, Placeholder.unparsed("where", where)));
                Ui.sound(p, "minecraft:entity.villager.no", 1f);
            } else {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (tag.equals(Items.kitTag(hand))) {
                    hand.setAmount(hand.getAmount() - 1);
                    p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
                }
            }
            return true;
        }
        Defence d = defenceAt(b);
        if (d != null) {
            DefenceMenu.open(this, f, d, p, lang);
            return true;
        }
        return false;
    }

    /** Bau setzen; liefert einen Fehler-Schlüssel oder null. */
    public String build(Player p, MapLayout.Spot spot, DefenceType type) {
        if (phase == Phase.OVER) {
            return "shop.closed";
        }
        if (spot.kind() != type.spot()) {
            return "defence.wrong-spot";
        }
        if (defences.containsKey(spot.id())) {
            return "defence.taken";
        }
        Defence d = new Defence(world, map.originX(), map.originZ(), MapType.FLOOR_Y, spot, type);
        d.build();
        defences.put(spot.id(), d);
        Holo m = markers.remove(spot.id());
        if (m != null) {
            m.remove();
        }
        label(d);
        world.playSound(d.center(), "minecraft:block.wood.place", 1f, 0.8f);
        world.spawnParticle(Particle.HAPPY_VILLAGER, d.center().add(0, 1, 0), 20, 1, 1, 1);
        broadcast(lang -> Texts.msg(lang, "defence.built", Placeholder.unparsed("player", p.getName()),
                Placeholder.unparsed("name", Texts.plain(lang, "defence." + type.id()))));
        return null;
    }

    /** Ausbauen; liefert einen Fehler-Schlüssel oder null. */
    String upgrade(Player p, Fighter f, Defence d) {
        int price = d.type.upgradePrice(d.level);
        if (price < 0) {
            return "menu.defence.max";
        }
        if (!f.wallet().pay(price)) {
            return "shop.poor";
        }
        double missing = d.maxHealth() - d.health;
        d.level++;
        d.health = d.maxHealth() - Math.max(0, missing);
        d.build();
        label(d);
        world.playSound(d.center(), "minecraft:block.anvil.use", 1f, 1.2f);
        broadcast(lang -> Texts.msg(lang, "defence.upgraded", Placeholder.unparsed("player", p.getName()),
                Placeholder.unparsed("name", Texts.plain(lang, "defence." + d.type.id())),
                Placeholder.unparsed("n", String.valueOf(d.level))));
        return null;
    }

    int repairPrice(Defence d) {
        return DefenceType.repairPrice(d.maxHealth() - d.health);
    }

    String repair(Fighter f, Defence d) {
        int price = repairPrice(d);
        if (price <= 0) {
            return "shop.nothing-to-repair";
        }
        if (!f.wallet().pay(price)) {
            return "shop.poor";
        }
        d.health = d.maxHealth();
        label(d);
        return null;
    }

    int repairAllPrice() {
        return defences.values().stream().mapToInt(this::repairPrice).sum();
    }

    String repairAll(Fighter f) {
        int price = repairAllPrice();
        if (price <= 0) {
            return "shop.nothing-to-repair";
        }
        if (!f.wallet().pay(price)) {
            return "shop.poor";
        }
        defences.values().forEach(d -> {
            d.health = d.maxHealth();
            label(d);
        });
        return null;
    }

    String patchHeart(Player p, Fighter f) {
        if (heart.health() >= heart.max()) {
            return "shop.nothing-to-repair";
        }
        if (!f.wallet().pay(Offer.HERZ_FLICKEN.price())) {
            return "shop.poor";
        }
        double got = heart.repair(Offer.HEART_PATCH);
        broadcast(lang -> Texts.msg(lang, "game.heart.patched", Placeholder.unparsed("player", p.getName()),
                Placeholder.unparsed("n", String.valueOf((int) got))));
        return null;
    }

    private void destroy(Defence d) {
        defences.remove(d.spot.id());
        d.restore();
        if (d.label != null) {
            d.label.remove();
        }
        world.playSound(d.center(), "minecraft:entity.generic.explode", 0.7f, 1.2f);
        world.spawnParticle(Particle.EXPLOSION, d.center().add(0, 1, 0), 2);
        broadcast(lang -> Texts.msg(lang, "defence.destroyed", Placeholder.unparsed("name", Texts.plain(lang, "defence." + d.type.id()))));
        marker(d.spot);
    }

    /** Türme schießen, Kessel brodeln, Banner heilen. */
    private void tickBuildings(boolean fighting) {
        for (Defence d : List.copyOf(defences.values())) {
            if (--d.cooldown > 0) {
                continue;
            }
            d.cooldown = d.type.interval(d.level);
            switch (d.type) {
                case PFEILTURM -> {
                    if (fighting) {
                        shoot(d);
                    } else {
                        d.cooldown = 1;
                    }
                }
                case KESSELFALLE -> {
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, d.center().add(0, 2, 0), 2, 1.5, 0.2, 1.5, 0.01);
                    if (fighting) {
                        scald(d);
                    }
                }
                case HEILBANNER -> heal(d);
                default -> d.cooldown = Integer.MAX_VALUE;
            }
        }
    }

    private List<Targeting.Candidate> candidates() {
        List<Targeting.Candidate> out = new ArrayList<>();
        for (Enemy e : enemies.values()) {
            if (!e.mob.isValid()) {
                continue;
            }
            Location l = e.mob.getLocation();
            out.add(new Targeting.Candidate(e, l.getX(), l.getY(), l.getZ(), progress(e)));
        }
        return out;
    }

    private void shoot(Defence d) {
        Location muzzle = d.muzzle();
        Optional<Targeting.Candidate> pick = Targeting.pick(muzzle.getX(), muzzle.getY() - 4, muzzle.getZ(),
                d.type.range(d.level), candidates());
        if (pick.isEmpty()) {
            d.cooldown = 5;
            return;
        }
        Enemy e = (Enemy) pick.get().id();
        Location aim = e.mob.getEyeLocation();
        Vector dir = aim.toVector().subtract(muzzle.toVector());
        double dist = dir.length();
        dir.normalize();
        world.spawn(muzzle.clone().add(dir.clone().multiply(0.8)), Arrow.class, a -> {
            a.setVelocity(dir.clone().multiply(Math.min(3.0, 1.2 + dist / 10)).add(new Vector(0, 0.05, 0)));
            a.setDamage(0);
            a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            a.addScoreboardTag(ARROW_TAG);
            a.setPersistent(false);
        });
        world.playSound(muzzle, "minecraft:entity.arrow.shoot", 0.8f, 1.1f);
        towerShots++;
        d.uses++;
        e.towerHitTick = tick;
        e.mob.damage(Targeting.arrowDamage(d.type, d.level, e.type));
    }

    private void scald(Defence d) {
        double r = d.type.range(d.level);
        Location c = d.center();
        for (Targeting.Candidate cand : Targeting.within(c.getX(), c.getY(), c.getZ(), r, candidates())) {
            Enemy e = (Enemy) cand.id();
            e.mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * SECOND, d.type.slowAmplifier(d.level), false, true));
            e.towerHitTick = tick;
            d.uses++;
            e.mob.damage(d.type.power(d.level));
            world.spawnParticle(Particle.BUBBLE_POP, e.mob.getLocation().add(0, 1, 0), 6, 0.3, 0.4, 0.3, 0.05);
        }
    }

    private void heal(Defence d) {
        double r = d.type.range(d.level);
        Location c = d.center();
        world.spawnParticle(Particle.HEART, c.clone().add(0, 3, 0), 1, 0.3, 0.2, 0.3);
        for (Fighter f : fighters.values()) {
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null || f.state() != Fighter.State.ALIVE || p.getLocation().distance(c) > r) {
                continue;
            }
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            double cap = max == null ? 20 : max.getValue();
            if (p.getHealth() < cap) {
                d.uses++;
            }
            p.setHealth(Math.min(cap, p.getHealth() + d.type.power(d.level)));
        }
        if (d.type.healsHeart(d.level) && phase == Phase.WAVE) {
            heart.repair(d.type.power(d.level));
        }
    }

    // ---------- Am Boden, retten ----------

    /** Tödlicher Treffer: statt zu sterben liegt man am Boden. */
    public void down(Player p) {
        Fighter f = fighters.get(p.getUniqueId());
        if (f == null || f.state() != Fighter.State.ALIVE) {
            return;
        }
        f.state(Fighter.State.DOWNED);
        f.downTicks(settings.downSeconds() * SECOND);
        p.setHealth(Math.min(p.getHealth(), 4));
        p.setFireTicks(0);
        p.clearActivePotionEffects();
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 5, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 10, false, false));
        p.setPose(Pose.SWIMMING, true);
        for (Enemy e : enemies.values()) {
            if (e.mob.getTarget() == p) {
                e.mob.setTarget(null);
            }
        }
        String lang = plugin.lang(p);
        Ui.title(p, Texts.text(lang, "down.title"), Texts.text(lang, "down.sub"), 3000);
        Ui.sound(p, "minecraft:entity.player.hurt", 0.6f);
        broadcast(l -> Texts.msg(l, "down.chat", Placeholder.unparsed("player", p.getName())));
        long standing = fighters.values().stream().filter(x -> x.online() && x.state() == Fighter.State.ALIVE).count();
        if (standing == 0) {
            f.downTicks(Math.min(f.downTicks(), 5 * SECOND));
        }
    }

    private void tickFighters() {
        for (Fighter f : fighters.values()) {
            Player p = Bukkit.getPlayer(f.uuid());
            if (p == null || !f.online()) {
                continue;
            }
            if (f.state() == Fighter.State.ALIVE) {
                if (tick % SECOND == 0) {
                    p.setFoodLevel(20);
                }
                if (p.getLocation().getY() < MapType.FLOOR_Y - 25) {
                    p.teleport(spawnLoc());
                    p.setFallDistance(0);
                }
                continue;
            }
            if (f.state() != Fighter.State.DOWNED) {
                continue;
            }
            f.downTicks(f.downTicks() - 1);
            Player helper = null;
            for (Fighter g : fighters.values()) {
                Player q = Bukkit.getPlayer(g.uuid());
                if (g == f || q == null || g.state() != Fighter.State.ALIVE || !q.isSneaking()
                        || q.getWorld() != p.getWorld() || q.getLocation().distance(p.getLocation()) > REVIVE_REACH) {
                    continue;
                }
                helper = q;
                break;
            }
            int need = settings.reviveSeconds() * SECOND;
            if (helper != null) {
                int progress = helper.getUniqueId().equals(f.reviver()) ? f.reviveTicks() + 1 : 1;
                f.reviveTicks(progress, helper.getUniqueId());
                if (progress % 5 == 0) {
                    String bar = Ui.bar(progress / (double) need, 10);
                    helper.sendActionBar(Texts.text(plugin.lang(helper), "down.reviving",
                            Placeholder.unparsed("player", p.getName()), Placeholder.unparsed("bar", bar)));
                }
                if (progress >= need) {
                    revive(p, f, helper);
                }
                continue;
            } else if (f.reviveTicks() > 0) {
                f.reviveTicks(0, null);
            }
            if (f.downTicks() % SECOND == 0) {
                p.sendActionBar(Texts.text(plugin.lang(p), "down.bar",
                        Placeholder.unparsed("secs", String.valueOf(Math.max(0, f.downTicks() / SECOND)))));
            }
            if (f.downTicks() <= 0) {
                out(p, f, true);
            }
        }
    }

    private void revive(Player p, Fighter f, Player helper) {
        f.state(Fighter.State.ALIVE);
        p.clearActivePotionEffects();
        p.setPose(Pose.STANDING, false);
        p.setHealth(Math.min(8, p.getHealth() + 6));
        Fighter h = fighters.get(helper.getUniqueId());
        if (h != null) {
            h.addRevive();
        }
        Ui.sound(p, "minecraft:entity.player.levelup", 1.4f);
        Ui.sound(helper, "minecraft:entity.player.levelup", 1.4f);
        broadcast(lang -> Texts.msg(lang, "down.revived", Placeholder.unparsed("player", p.getName()),
                Placeholder.unparsed("helper", helper.getName())));
    }

    /** Ausgeschieden: zuschauen bis zur nächsten Aufbauphase. */
    private void out(Player p, Fighter f, boolean announce) {
        f.state(Fighter.State.OUT);
        p.clearActivePotionEffects();
        p.setPose(Pose.STANDING, false);
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(heartLoc().add(0, 10, 6).setDirection(new Vector(0, -0.7, -1)));
        String lang = plugin.lang(p);
        heartBar.show(p, lang);
        waveBar.show(p, lang);
        if (announce) {
            p.sendMessage(Texts.msg(lang, "down.out.self"));
            broadcast(l -> Texts.msg(l, "down.out", Placeholder.unparsed("player", f.name())));
        }
    }

    public boolean downed(UUID uuid) {
        Fighter f = fighters.get(uuid);
        return f != null && f.state() == Fighter.State.DOWNED;
    }

    /** /bereit bzw. Knopf „Welle starten“. */
    public void ready(Player p) {
        Fighter f = fighters.get(p.getUniqueId());
        if (f == null || phase != Phase.PREPARE || f.ready()) {
            return;
        }
        f.ready(true);
        long online = fighters.values().stream().filter(Fighter::online).count();
        long ready = fighters.values().stream().filter(x -> x.online() && x.ready()).count();
        broadcast(lang -> Texts.msg(lang, "game.ready", Placeholder.unparsed("player", p.getName()),
                Placeholder.unparsed("n", String.valueOf(ready)), Placeholder.unparsed("total", String.valueOf(online))));
    }

    // ---------- Ende ----------

    private void gameOver(boolean abandoned) {
        if (phase == Phase.OVER) {
            return;
        }
        phase = Phase.OVER;
        phaseTicks = settings.afterSeconds() * SECOND;
        paused = false;
        releaseAway();
        for (Player p : audience()) {
            plugin.net().playing(p, false);
        }
        for (Enemy e : enemies.values()) {
            e.mob.setAI(false);
            e.mob.setInvulnerable(true);
        }
        int score = cleared;
        List<String> names = fighters.values().stream().map(Fighter::name).toList();
        int pos = plugin.board().add(map.id(), score, names, System.currentTimeMillis());
        plugin.saveBoard();
        if (!abandoned) {
            titleAll("game.over.title", "game.over.sub", Placeholder.unparsed("n", String.valueOf(score)));
            soundAll("minecraft:entity.wither.death", 0.8f);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, heartLoc().add(0, 2, 0), 1);
            for (Player p : audience()) {
                String lang = plugin.lang(p);
                TagResolver[] r = {Placeholder.unparsed("heart", heartName(lang)), Placeholder.unparsed("n", String.valueOf(score)),
                    Placeholder.unparsed("pos", String.valueOf(pos))};
                p.sendMessage(Texts.msg(lang, pos > 0 ? "game.over.chat" : "game.over.chat.plain", r));
                p.sendMessage(Texts.msg(lang, "game.back", Placeholder.unparsed("secs", String.valueOf(settings.afterSeconds()))));
            }
        }
        for (Fighter f : fighters.values()) {
            report(f, true);
        }
        plugin.getLogger().info("Bollwerk " + map.id() + ": vorbei nach " + score + " Wellen (" + names + ", "
                + (System.currentTimeMillis() - started) / 1000 + " s).");
        plugin.stateChanged();
    }

    /** Statistik melden (Netzwerk + lokal). */
    private void report(Fighter f, boolean tellBest) {
        var data = plugin.store().get(f.uuid(), f.name());
        boolean best = data.addGame(map.id(), f.kills(), cleared, f.revives());
        plugin.store().save(data);
        plugin.net().stats(f.uuid(), Map.of("games", 1L, "kills", (long) f.kills(), "waves", (long) cleared,
                "revives", (long) f.revives()), Map.of("best_wave", (long) cleared));
        Player p = Bukkit.getPlayer(f.uuid());
        if (best && tellBest && p != null && cleared > 0) {
            p.sendMessage(Texts.msg(plugin.lang(p), "game.over.best", Placeholder.unparsed("n", String.valueOf(cleared))));
        }
    }

    /** Aufräumen und alle zurück in die Wachstube. */
    public void close() {
        releaseAway();
        if (phase != Phase.OVER) {
            phase = Phase.OVER;
            for (Fighter f : fighters.values()) {
                report(f, false);
            }
        }
        for (Enemy e : enemies.values()) {
            e.mob.remove();
        }
        enemies.clear();
        toSpawn.clear();
        for (Defence d : defences.values()) {
            d.restore();
            if (d.label != null) {
                d.label.remove();
            }
        }
        defences.clear();
        markers.values().forEach(Holo::remove);
        markers.clear();
        if (shopkeeper != null) {
            shopkeeper.remove();
        }
        for (Entity ent : world.getEntitiesByClass(Arrow.class)) {
            if (ent.getScoreboardTags().contains(ARROW_TAG)) {
                ent.remove();
            }
        }
        for (Player p : audience()) {
            heartBar.hide(p);
            waveBar.hide(p);
            p.setPose(Pose.STANDING, false);
            plugin.net().playing(p, false);
            plugin.hall().enter(p);
        }
        fighters.clear();
        spectators.clear();
        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                world.getChunkAt((map.originX() >> 4) + cx, (map.originZ() >> 4) + cz).removePluginChunkTicket(plugin);
            }
        }
    }

    // ---------- Anzeigen ----------

    private void updateBars() {
        float frac = (float) heart.fraction();
        BossBar.Color hc = frac > 0.5f ? BossBar.Color.GREEN : frac > 0.25f ? BossBar.Color.YELLOW : BossBar.Color.RED;
        heartBar.update(lang -> Texts.text(lang, "game.heart.bar", Placeholder.unparsed("heart", heartName(lang)),
                Placeholder.unparsed("hp", String.valueOf((int) Math.ceil(heart.health()))),
                Placeholder.unparsed("max", String.valueOf((int) heart.max()))), frac, hc);
        if (phase == Phase.PREPARE) {
            int total = (wave == 0 ? settings.firstPrepare() : settings.prepare()) * SECOND;
            waveBar.update(lang -> Texts.text(lang, "game.prepare.bar",
                    Placeholder.unparsed("secs", String.valueOf((phaseTicks + SECOND - 1) / SECOND)),
                    Placeholder.unparsed("n", String.valueOf(wave + 1))), phaseTicks / (float) total, BossBar.Color.BLUE);
        } else if (phase == Phase.WAVE) {
            int left = enemyCount();
            waveBar.update(lang -> Texts.text(lang, "game.wave.bar", Placeholder.unparsed("n", String.valueOf(wave)),
                    Placeholder.unparsed("left", String.valueOf(left))),
                    waveTotal == 0 ? 0 : left / (float) Math.max(left, waveTotal), bossType != null ? BossBar.Color.PURPLE : BossBar.Color.RED);
        }
    }

    /** Sekunden bis zur nächsten Welle (Aufbau) oder -1. */
    public int prepareSeconds() {
        return phase == Phase.PREPARE ? (phaseTicks + SECOND - 1) / SECOND : -1;
    }

    /** Gegenstand zum Kit-Knopf? (Shop, Welle starten) */
    public static String button(ItemStack item) {
        return Items.kitTag(item);
    }
}
