package de.moinmorni.bollwerk.logic;

/**
 * Das Herz der Karte (Dorfbrunnen, Klosterglocke …): fällt es, ist das Spiel vorbei. Rein rechnerisch.
 */
public final class Heart {

    private final double max;
    private double health;
    private double lastHit;

    public Heart(double max) {
        this.max = Math.max(1, max);
        this.health = this.max;
    }

    public double max() {
        return max;
    }

    public double health() {
        return health;
    }

    public boolean fallen() {
        return health <= 0;
    }

    /** Schaden; liefert {@code true}, wenn das Herz gerade dadurch fällt. */
    public boolean damage(double amount) {
        if (fallen() || amount <= 0) {
            return false;
        }
        lastHit = amount;
        health = Math.max(0, health - amount);
        return fallen();
    }

    /** Reparieren (nicht über das Maximum, ein gefallenes Herz bleibt gefallen); liefert, wie viel es wirklich war. */
    public double repair(double amount) {
        if (fallen() || amount <= 0) {
            return 0;
        }
        double before = health;
        health = Math.min(max, health + amount);
        return health - before;
    }

    /** Anteil 0 … 1 (für die Leiste). */
    public double fraction() {
        return health / max;
    }

    public double lastHit() {
        return lastHit;
    }

    /** Für die Verwaltung: Leben direkt setzen. */
    public void set(double value) {
        health = Math.max(0, Math.min(max, value));
    }
}
