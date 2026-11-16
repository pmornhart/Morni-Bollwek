package net.morni.bollwerk.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Zusammensetzung einer Welle – rein rechnerisch (ohne Server), darum gut zu prüfen. Gleiche Welle und gleiche
 * Spielerzahl ergeben immer dieselbe Welle.
 *
 * <ul>
 *   <li>Budget: {@code (4 + 2,5 × Welle) × (1 + 0,6 × (Spieler − 1))}, höchstens {@link #MAX_ENEMIES} Gegner.</li>
 *   <li>Neue Gegnerart: in ihrer ersten Welle kommt sie mindestens einmal (zum Kennenlernen).</li>
 *   <li>Boss-Welle (jede fünfte): Boss plus Begleitung mit 60 % des Budgets.</li>
 *   <li>Leben: {@code × (1 + 0,10 × (Welle − 1) + 0,20 × (Spieler − 1))}, Schaden {@code × (1 + 0,06 × (Welle − 1))}.</li>
 * </ul>
 *
 * @param wave        Nummer der Welle (ab 1)
 * @param players     Spielerzahl (1–6)
 * @param enemies     Gegner in Spawn-Reihenfolge (Boss zuletzt)
 * @param healthScale Faktor für Leben
 * @param damageScale Faktor für Schaden (Spieler, Herz, Bauten)
 * @param spawnTicks  Abstand zwischen zwei Gegnern (Ticks)
 */
public record WavePlan(int wave, int players, List<EnemyType> enemies, double healthScale, double damageScale,
        int spawnTicks) {

    public static final int MAX_PLAYERS = 6;
    public static final int MAX_ENEMIES = 60;
    private static final double BOSS_ESCORT = 0.6;
    private static final int MIN_SPAWN_TICKS = 8;
    private static final int START_SPAWN_TICKS = 30;

    /** Welle ausrechnen. */
    public static WavePlan compose(int wave, int players) {
        int w = Math.max(1, wave);
        int n = clampPlayers(players);
        double budget = budget(w, n);
        EnemyType boss = EnemyType.bossFor(w);
        if (boss != null) {
            budget *= BOSS_ESCORT;
        }
        List<EnemyType> pool = new ArrayList<>();
        for (EnemyType t : EnemyType.values()) {
            if (!t.boss() && t.firstWave() <= w) {
                pool.add(t);
            }
        }
        Random random = new Random(w * 7919L + n * 104729L);
        List<EnemyType> picked = new ArrayList<>();
        int left = (int) Math.round(budget);
        // Neue Gegnerart einmal vorstellen
        for (EnemyType t : pool) {
            if (t.firstWave() == w && left >= t.cost()) {
                picked.add(t);
                left -= t.cost();
            }
        }
        int limit = MAX_ENEMIES - (boss == null ? 0 : 1);
        while (left > 0 && picked.size() < limit) {
            List<EnemyType> fits = new ArrayList<>();
            for (EnemyType t : pool) {
                if (t.cost() <= left) {
                    // Billige Gegner öfter, damit Wellen voll wirken
                    for (int i = 0; i < weight(t, w); i++) {
                        fits.add(t);
                    }
                }
            }
            if (fits.isEmpty()) {
                break;
            }
            EnemyType t = fits.get(random.nextInt(fits.size()));
            picked.add(t);
            left -= t.cost();
        }
        Collections.shuffle(picked, random);
        if (boss != null) {
            picked.add(boss);
        }
        double health = 1 + 0.10 * (w - 1) + 0.20 * (n - 1);
        double damage = 1 + 0.06 * (w - 1);
        int spawn = Math.max(MIN_SPAWN_TICKS, START_SPAWN_TICKS - w);
        return new WavePlan(w, n, List.copyOf(picked), health, damage, spawn);
    }

    /** Budget der Welle (ohne Boss-Abzug). */
    public static double budget(int wave, int players) {
        return (4 + 2.5 * Math.max(1, wave)) * (1 + 0.6 * (clampPlayers(players) - 1));
    }

    private static int weight(EnemyType t, int wave) {
        return switch (t) {
            case PLUENDERER -> 5;
            case FLINKER -> 3;
            case SCHILDTRAEGER -> 2;
            case ARMBRUSTSCHUETZE -> 2;
            case BRECHER -> wave >= 10 ? 2 : 1;
            default -> 0;
        };
    }

    public static int clampPlayers(int players) {
        return Math.max(1, Math.min(MAX_PLAYERS, players));
    }

    public boolean bossWave() {
        return EnemyType.bossFor(wave) != null;
    }

    /** Wie viele Gegner jeder Art. */
    public Map<EnemyType, Integer> counts() {
        Map<EnemyType, Integer> m = new EnumMap<>(EnemyType.class);
        enemies.forEach(t -> m.merge(t, 1, Integer::sum));
        return m;
    }

    /** Leben eines Gegners in dieser Welle. */
    public double health(EnemyType t) {
        return Math.round(t.health() * healthScale * 10) / 10.0;
    }
}
