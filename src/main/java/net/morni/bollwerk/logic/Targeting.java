package net.morni.bollwerk.logic;

import java.util.List;
import java.util.Optional;

/**
 * Wen ein Pfeilturm beschießt: unter allen Gegnern in Reichweite den, der dem Herzen am nächsten gekommen ist
 * (größter Fortschritt auf seinem Weg); bei Gleichstand den nächsten am Turm.
 */
public final class Targeting {

    /** Ein möglicher Gegner. {@code progress}: zurückgelegter Anteil des Wegs (0 … 1). */
    public record Candidate(Object id, double x, double y, double z, double progress) {
    }

    private Targeting() {
    }

    public static Optional<Candidate> pick(double tx, double ty, double tz, double range, List<Candidate> all) {
        Candidate best = null;
        double bestDist = 0;
        double r2 = range * range;
        for (Candidate c : all) {
            double d = dist2(tx, ty, tz, c);
            if (d > r2) {
                continue;
            }
            if (best == null || c.progress() > best.progress()
                    || (c.progress() == best.progress() && d < bestDist)) {
                best = c;
                bestDist = d;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Alle in einem Umkreis (Kesselfalle, Heilbanner). */
    public static List<Candidate> within(double x, double y, double z, double radius, List<Candidate> all) {
        double r2 = radius * radius;
        return all.stream().filter(c -> dist2(x, y, z, c) <= r2).toList();
    }

    private static double dist2(double x, double y, double z, Candidate c) {
        double dx = c.x() - x;
        double dy = c.y() - y;
        double dz = c.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Schaden eines Turmpfeils an einem Gegner (Schildträger halbieren ihn). */
    public static double arrowDamage(DefenceType tower, int level, EnemyType target) {
        return tower.power(level) * target.towerFactor();
    }
}
