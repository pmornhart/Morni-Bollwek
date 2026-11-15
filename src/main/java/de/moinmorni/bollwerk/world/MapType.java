package de.moinmorni.bollwerk.world;

import de.moinmorni.bollwerk.logic.DefenceType;
import de.moinmorni.bollwerk.world.MapLayout.Lane;
import de.moinmorni.bollwerk.world.MapLayout.Point;
import de.moinmorni.bollwerk.world.MapLayout.Spot;
import java.util.List;

/**
 * Die Karten. Jede liegt an ihrem eigenen Platz in der Welt (Mittelpunkt {@code originX}/{@code originZ}, Boden auf
 * {@link #FLOOR_Y}); auf jeder Karte läuft höchstens eine Runde gleichzeitig.
 */
public enum MapType {

    /** Talsperre Morni-Dorf: ummauertes Bergdorf; Tor im Norden, Bresche im Osten, Almtor im Westen. */
    TALSPERRE("talsperre", 1000, 0, talsperre()),
    /** Klosterhof: Kloster auf einer Felsnase; Pilgerweg von Süden, Hängebrücke im Osten, Gratweg im Westen. */
    KLOSTERHOF("klosterhof", 2000, 0, klosterhof());

    /** Höhe der obersten Bodenschicht in der Welt (man steht eins darüber). */
    public static final int FLOOR_Y = 100;

    private final String id;
    private final int originX;
    private final int originZ;
    private final MapLayout layout;

    MapType(String id, int originX, int originZ, MapLayout layout) {
        this.id = id;
        this.originX = originX;
        this.originZ = originZ;
        this.layout = layout;
    }

    public String id() {
        return id;
    }

    public int originX() {
        return originX;
    }

    public int originZ() {
        return originZ;
    }

    public MapLayout layout() {
        return layout;
    }

    /** Bauplan der Karte (braucht den Server für Blockzustände). */
    public Plan plan() {
        return this == TALSPERRE ? TalsperrePlan.build(layout) : KlosterhofPlan.build(layout);
    }

    public static MapType byId(String id) {
        for (MapType m : values()) {
            if (m.id.equalsIgnoreCase(id) || m.name().equalsIgnoreCase(id)) {
                return m;
            }
        }
        return null;
    }

    private static Point pt(double x, double y, double z) {
        return new Point(x, y, z);
    }

    private static MapLayout talsperre() {
        List<Lane> lanes = List.of(
                new Lane("nordtor", List.of(pt(-10.5, 1, -46.5), pt(0.5, 1, -35.5), pt(0.5, 1, -22.5), pt(0.5, 1, -10.5),
                        pt(0.5, 1, -4.5))),
                new Lane("bresche", List.of(pt(46.5, 1, 10.5), pt(33.5, 1, 0.5), pt(22.5, 1, 0.5), pt(10.5, 1, 0.5),
                        pt(4.5, 1, 0.5))),
                new Lane("almtor", List.of(pt(-46.5, 1, -8.5), pt(-33.5, 1, 0.5), pt(-22.5, 1, 0.5), pt(-10.5, 1, 0.5),
                        pt(-3.5, 1, 0.5))));
        List<Spot> spots = List.of(
                new Spot(1, DefenceType.Spot.LANE, 0, 1, -16, 0, true),
                new Spot(2, DefenceType.Spot.LANE, 0, 1, -29, 0, true),
                new Spot(3, DefenceType.Spot.LANE, 16, 1, 0, 1, false),
                new Spot(4, DefenceType.Spot.LANE, 28, 1, 0, 1, false),
                new Spot(5, DefenceType.Spot.LANE, -16, 1, 0, 2, false),
                new Spot(6, DefenceType.Spot.LANE, -28, 1, 0, 2, false),
                new Spot(7, DefenceType.Spot.SIDE, -4, 1, -14, -1, false),
                new Spot(8, DefenceType.Spot.SIDE, 4, 1, -14, -1, false),
                new Spot(9, DefenceType.Spot.SIDE, 14, 1, -4, -1, false),
                new Spot(10, DefenceType.Spot.SIDE, 14, 1, 4, -1, false),
                new Spot(11, DefenceType.Spot.SIDE, -14, 1, -4, -1, false),
                new Spot(12, DefenceType.Spot.SIDE, -14, 1, 4, -1, false),
                new Spot(13, DefenceType.Spot.SIDE, 5, 1, -6, -1, false),
                new Spot(14, DefenceType.Spot.SIDE, -6, 1, 5, -1, false));
        return new MapLayout(pt(0.5, 1, 0.5), 4.2, pt(0.5, 1, 8.5), 180f, pt(4.5, 1, 9.5), lanes, spots);
    }

    private static MapLayout klosterhof() {
        List<Lane> lanes = List.of(
                new Lane("pilgerweg", List.of(pt(0.5, -4, 46.5), pt(0.5, -4, 36.5), pt(0.5, 1, 24.5), pt(0.5, 1, 16.5),
                        pt(0.5, 1, 4.5))),
                new Lane("haengebruecke", List.of(pt(45.5, 1, 0.5), pt(33.5, 1, 0.5), pt(22.5, 1, 0.5), pt(10.5, 1, 0.5),
                        pt(4.5, 1, 0.5))),
                new Lane("gratweg", List.of(pt(-46.5, 1, -14.5), pt(-33.5, 1, -14.5), pt(-24.5, 1, 0.5),
                        pt(-12.5, 1, 0.5), pt(-3.5, 1, 0.5))));
        List<Spot> spots = List.of(
                new Spot(1, DefenceType.Spot.LANE, 0, 1, 12, 0, true),
                new Spot(2, DefenceType.Spot.LANE, 0, 1, 21, 0, true),
                new Spot(3, DefenceType.Spot.LANE, 12, 1, 0, 1, false),
                new Spot(4, DefenceType.Spot.LANE, 21, 1, 0, 1, false),
                new Spot(5, DefenceType.Spot.LANE, -12, 1, 0, 2, false),
                new Spot(6, DefenceType.Spot.LANE, -20, 1, 0, 2, false),
                new Spot(7, DefenceType.Spot.SIDE, -4, 1, 12, -1, false),
                new Spot(8, DefenceType.Spot.SIDE, 4, 1, 12, -1, false),
                new Spot(9, DefenceType.Spot.SIDE, 12, 1, -4, -1, false),
                new Spot(10, DefenceType.Spot.SIDE, 12, 1, 4, -1, false),
                new Spot(11, DefenceType.Spot.SIDE, -12, 1, -4, -1, false),
                new Spot(12, DefenceType.Spot.SIDE, -12, 1, 4, -1, false),
                new Spot(13, DefenceType.Spot.SIDE, 5, 1, -5, -1, false),
                new Spot(14, DefenceType.Spot.SIDE, -5, 1, 5, -1, false));
        return new MapLayout(pt(0.5, 1, 0.5), 4.2, pt(6.5, 1, 7.5), 135f, pt(-7.5, 1, 12.5), lanes, spots);
    }
}
