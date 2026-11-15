package de.moinmorni.bollwerk.logic;

/**
 * Alle Angreifer – eigene Namen, eigene Ausrüstung. Werte gelten für Welle 1 mit einer Person; {@link WavePlan}
 * skaliert Leben und Schaden mit Welle und Spielerzahl.
 *
 * <p>{@code cost} ist der Preis aus dem Wellen-Budget, {@code firstWave} die erste Welle, in der es sie gibt.
 */
public enum EnemyType {

    /** Einfacher Nahkämpfer mit Axt. */
    PLUENDERER("pluenderer", 20, 1.0, 4, 3, 5, 1, 1, false, 1.0, false),
    /** Schnell und schwach – rennt am liebsten am Turm vorbei. */
    FLINKER("flinker", 12, 1.45, 3, 2, 4, 1, 2, false, 1.0, false),
    /** Langsam, viel Leben, Pfeile prallen zur Hälfte am Schild ab. */
    SCHILDTRAEGER("schildtraeger", 40, 0.8, 6, 4, 9, 3, 3, false, 0.5, false),
    /** Bleibt auf Abstand und schießt aufs Herz. */
    ARMBRUSTSCHUETZE("armbrustschuetze", 22, 1.0, 3, 1, 7, 2, 4, true, 1.0, false),
    /** Geht auf Mauern und Bauten los – richtet dort vierfachen Schaden an. */
    BRECHER("brecher", 36, 0.95, 5, 12, 10, 3, 6, false, 1.0, false),
    /** Boss der Wellen 5, 15, 25 …: groß, stark, walzt Palisaden nieder. */
    LAWINENRIESE("lawinenriese", 260, 0.9, 25, 40, 80, 0, 5, false, 0.8, true),
    /** Boss der Wellen 10, 20, 30 …: wirft Tränke, ruft Flinke herbei. */
    NEBELHEXE("nebelhexe", 180, 0.9, 15, 6, 90, 0, 10, true, 1.0, true);

    private final String id;
    private final double health;
    private final double speed;
    private final double heartDamage;
    private final double wallDamage;
    private final int reward;
    private final int cost;
    private final int firstWave;
    private final boolean ranged;
    private final double towerFactor;
    private final boolean boss;

    EnemyType(String id, double health, double speed, double heartDamage, double wallDamage, int reward, int cost,
            int firstWave, boolean ranged, double towerFactor, boolean boss) {
        this.id = id;
        this.health = health;
        this.speed = speed;
        this.heartDamage = heartDamage;
        this.wallDamage = wallDamage;
        this.reward = reward;
        this.cost = cost;
        this.firstWave = firstWave;
        this.ranged = ranged;
        this.towerFactor = towerFactor;
        this.boss = boss;
    }

    /** Schlüssel für Texte („enemy.&lt;id&gt;“) und Statistik. */
    public String id() {
        return id;
    }

    /** Leben in Welle 1 (halbe Herzen). */
    public double health() {
        return health;
    }

    /** Laufgeschwindigkeit als Faktor zur normalen Geschwindigkeit. */
    public double speed() {
        return speed;
    }

    /** Schaden am Herzen pro Schlag (alle 1 Sekunde). */
    public double heartDamage() {
        return heartDamage;
    }

    /** Schaden an Palisaden und Bauten pro Schlag. */
    public double wallDamage() {
        return wallDamage;
    }

    /** Gulden für den Sieg über diesen Gegner (Welle 1). */
    public int reward() {
        return reward;
    }

    /** Preis aus dem Wellen-Budget (Bosse: 0, sie kommen extra). */
    public int cost() {
        return cost;
    }

    public int firstWave() {
        return firstWave;
    }

    /** Fernkampf: bleibt vor dem Herzen stehen und schießt. */
    public boolean ranged() {
        return ranged;
    }

    /** Wie stark Pfeile von Türmen wirken (Schild: 0,5). */
    public double towerFactor() {
        return towerFactor;
    }

    public boolean boss() {
        return boss;
    }

    /** Greift lieber Bauten als das Herz an. */
    public boolean wallBreaker() {
        return this == BRECHER || this == LAWINENRIESE;
    }

    /** Boss dieser Welle (oder null): Welle 5, 15, 25 … Lawinenriese; 10, 20, 30 … Nebelhexe. */
    public static EnemyType bossFor(int wave) {
        if (wave <= 0 || wave % 5 != 0) {
            return null;
        }
        return (wave / 5) % 2 == 1 ? LAWINENRIESE : NEBELHEXE;
    }

    public static EnemyType byId(String id) {
        for (EnemyType t : values()) {
            if (t.id.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }
}
