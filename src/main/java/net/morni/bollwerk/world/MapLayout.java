package net.morni.bollwerk.world;

import java.util.List;
import net.morni.bollwerk.logic.DefenceType;

/**
 * Was das Spiel über eine Karte wissen muss – ohne Blöcke, darum ohne Server prüfbar. Alle Angaben relativ zum
 * Mittelpunkt der Karte; y = 1 ist die Höhe, auf der man steht.
 *
 * @param heart      Mitte des Herzens (Brunnen, Glocke)
 * @param heartReach so nah müssen Gegner heran, um das Herz zu treffen (Fernkämpfer: {@link #RANGED_REACH})
 * @param spawn      Startpunkt der Spieler
 * @param spawnYaw   Blickrichtung am Startpunkt
 * @param shop       Platz der Krämerin
 * @param lanes      Wege der Gegner (erster Punkt = wo sie auftauchen, letzter = vor dem Herzen)
 * @param spots      Bauplätze
 */
public record MapLayout(Point heart, double heartReach, Point spawn, float spawnYaw, Point shop, List<Lane> lanes,
        List<Spot> spots) {

    /** Fernkämpfer schießen aus dieser Entfernung aufs Herz. */
    public static final double RANGED_REACH = 9;

    public record Point(double x, double y, double z) {

        public double distance(Point o) {
            double dx = x - o.x;
            double dy = y - o.y;
            double dz = z - o.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        /** Waagrechter Abstand (ohne Höhe). */
        public double flat(double ox, double oz) {
            double dx = x - ox;
            double dz = z - oz;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    /** Ein Weg. {@code id} für Texte („lane.&lt;id&gt;“). */
    public record Lane(String id, List<Point> path) {

        public Point start() {
            return path.get(0);
        }

        /** Gesamtlänge des Wegs. */
        public double length() {
            double sum = 0;
            for (int i = 1; i < path.size(); i++) {
                sum += path.get(i - 1).distance(path.get(i));
            }
            return sum;
        }

        /**
         * Wie weit ist jemand an dieser Stelle schon gekommen (0 … 1)? Es zählt der nächste Punkt auf dem Weg
         * (waagrecht).
         */
        public double progressAt(double x, double z) {
            double total = length();
            if (total <= 0) {
                return 1;
            }
            double before = 0;
            double bestDist = Double.MAX_VALUE;
            double best = 0;
            for (int i = 1; i < path.size(); i++) {
                Point a = path.get(i - 1);
                Point b = path.get(i);
                double vx = b.x() - a.x();
                double vz = b.z() - a.z();
                double len2 = vx * vx + vz * vz;
                double t = len2 == 0 ? 0 : ((x - a.x()) * vx + (z - a.z()) * vz) / len2;
                t = Math.max(0, Math.min(1, t));
                double d = Math.hypot(x - (a.x() + t * vx), z - (a.z() + t * vz));
                double seg = a.distance(b);
                if (d < bestDist) {
                    bestDist = d;
                    best = before + t * seg;
                }
                before += seg;
            }
            return Math.max(0, Math.min(1, best / total));
        }

        /** Punkt auf dem Weg nach {@code distance} Blöcken (geklemmt auf Anfang und Ende). */
        public Point pointAt(double distance) {
            double left = Math.max(0, distance);
            for (int i = 1; i < path.size(); i++) {
                Point a = path.get(i - 1);
                Point b = path.get(i);
                double seg = a.distance(b);
                if (left <= seg && seg > 0) {
                    double t = left / seg;
                    return new Point(a.x() + t * (b.x() - a.x()), a.y() + t * (b.y() - a.y()), a.z() + t * (b.z() - a.z()));
                }
                left -= seg;
            }
            return path.get(path.size() - 1);
        }
    }

    /**
     * Bauplatz. {@code acrossX}: Palisade steht quer zur x-Achse (Weg läuft in z-Richtung). {@code lane}: zu welchem Weg
     * er gehört (-1 bei Plätzen neben dem Weg).
     */
    public record Spot(int id, DefenceType.Spot kind, int x, int y, int z, int lane, boolean acrossX) {
    }

    public Spot spot(int id) {
        return spots.stream().filter(s -> s.id() == id).findFirst().orElse(null);
    }
}
