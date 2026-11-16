package de.moinmorni.bollwerk.logic;

/**
 * Bauten, die man zwischen den Wellen auf feste Bauplätze setzt. Jeder Bau hat drei Stufen; Werte je Stufe stehen in
 * den Feldern (Index 0 = Stufe 1). Rein rechnerisch – das Bauen im Spiel macht {@code game.Defences}.
 */
public enum DefenceType {

    /** Holzpalisade quer über den Weg: hält Gegner auf, bis sie zerschlagen ist. */
    PALISADE("palisade", Spot.LANE, 30, new int[] {40, 70}, new double[] {120, 220, 360},
            new double[] {0, 0, 0}, new double[] {0, 0, 0}, 0),
    /** Kesselfalle: brodelnder Kessel am Weg – bremst und verbrüht alles in der Nähe. */
    KESSELFALLE("kesselfalle", Spot.LANE, 45, new int[] {45, 80}, new double[] {150, 220, 300},
            new double[] {3, 4, 5}, new double[] {1, 2, 3}, 20),
    /** Pfeilturm: schießt auf den Gegner, der dem Herzen am nächsten ist. */
    PFEILTURM("pfeilturm", Spot.SIDE, 60, new int[] {55, 95}, new double[] {150, 250, 400},
            new double[] {12, 15, 18}, new double[] {4, 6, 9}, 30),
    /** Heilbanner: heilt Leute in der Nähe; ab Stufe 3 auch das Herz. */
    HEILBANNER("heilbanner", Spot.SIDE, 40, new int[] {40, 70}, new double[] {120, 180, 260},
            new double[] {6, 8, 10}, new double[] {1, 2, 3}, 40);

    /** Art des Bauplatzes: auf dem Weg der Gegner oder daneben. */
    public enum Spot {
        LANE, SIDE
    }

    public static final int MAX_LEVEL = 3;
    /** Reparatur kostet 1 Gulden je so viele Lebenspunkte. */
    public static final int REPAIR_HP_PER_GULDEN = 4;
    private static final int[] TOWER_COOLDOWN = {30, 24, 18};

    private final String id;
    private final Spot spot;
    private final int price;
    private final int[] upgrades;
    private final double[] health;
    private final double[] range;
    private final double[] power;
    private final int interval;

    DefenceType(String id, Spot spot, int price, int[] upgrades, double[] health, double[] range, double[] power,
            int interval) {
        this.id = id;
        this.spot = spot;
        this.price = price;
        this.upgrades = upgrades;
        this.health = health;
        this.range = range;
        this.power = power;
        this.interval = interval;
    }

    public String id() {
        return id;
    }

    public Spot spot() {
        return spot;
    }

    /** Preis des Bauplans. */
    public int price() {
        return price;
    }

    /** Preis für den Ausbau von {@code level} auf {@code level + 1}; -1, wenn es nicht weiter geht. */
    public int upgradePrice(int level) {
        if (level < 1 || level >= MAX_LEVEL) {
            return -1;
        }
        return upgrades[level - 1];
    }

    public double health(int level) {
        return health[idx(level)];
    }

    /** Reichweite in Blöcken (Turm: Schussweite, Kessel/Banner: Radius). */
    public double range(int level) {
        return range[idx(level)];
    }

    /** Turm: Schaden je Pfeil · Kessel: Schaden je Sekunde · Banner: Heilung je Puls (halbe Herzen). */
    public double power(int level) {
        return power[idx(level)];
    }

    /** Ticks zwischen zwei Wirkungen (Turm: Schuss, Kessel/Banner: Puls). */
    public int interval(int level) {
        return this == PFEILTURM ? TOWER_COOLDOWN[idx(level)] : interval;
    }

    /** Stufe der Bremse durch die Kesselfalle (Langsamkeit I … III). */
    public int slowAmplifier(int level) {
        return this == KESSELFALLE ? idx(level) : -1;
    }

    /** Heilbanner heilt ab Stufe 3 auch das Herz (so viel wie einen Spieler). */
    public boolean healsHeart(int level) {
        return this == HEILBANNER && level >= MAX_LEVEL;
    }

    /** Gulden für eine Reparatur von {@code missing} Lebenspunkten (aufgerundet, mindestens 1). */
    public static int repairPrice(double missing) {
        if (missing <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(missing / REPAIR_HP_PER_GULDEN));
    }

    /** Wie viel ein Gegner einem Bau pro Schlag abzieht (Brecher ×4 steckt schon in {@link EnemyType#wallDamage()}). */
    public static double hit(EnemyType enemy, double damageScale) {
        return enemy.wallDamage() * damageScale;
    }

    private static int idx(int level) {
        return Math.max(1, Math.min(MAX_LEVEL, level)) - 1;
    }

    public static DefenceType byId(String id) {
        for (DefenceType t : values()) {
            if (t.id.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }
}
