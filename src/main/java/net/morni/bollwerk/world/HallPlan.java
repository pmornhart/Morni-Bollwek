package net.morni.bollwerk.world;

import java.util.Random;

/**
 * Die Wachstube: ein Holzpalisaden-Lager auf einer kleinen Alm-Insel. In der Mitte das Lagerfeuer, im Norden zwei
 * Kartentische (Talsperre, Klosterhof) mit schwebender Schrift, ringsum Bänke, Fässer und Waffenständer.
 * Hier wartet man auf die nächste Runde und stimmt über die Karte ab. Eigener Entwurf.
 */
public final class HallPlan {

    public static final double SPAWN_X = 0.5;
    public static final double SPAWN_Y = 1;
    public static final double SPAWN_Z = 8.5;
    public static final float SPAWN_YAW = 180f;
    /** Alles innerhalb dieses Abstands zur Mitte gehört zur Wachstube. */
    public static final int REACH = 26;
    /** Kartentische (relativ): Talsperre links, Klosterhof rechts. */
    public static final int[][] TABLES = {{-6, -6}, {6, -6}};
    /** Bestenliste (relativ). */
    public static final int[] BOARD = {0, -12};

    private static final int RING = 15;

    private HallPlan() {
    }

    public static Plan build() {
        Plan p = new Plan();
        Random r = new Random(1291);
        MapKit.meadow(p, 20, 14, r);
        for (int x = -RING; x <= RING; x++) {
            for (int z = -RING; z <= RING; z++) {
                double d = Math.hypot(x, z);
                if (d < RING - 0.5) {
                    p.set(x, 0, z, d < 4 ? "cobblestone" : Decor.lozenge(x, z, 3) ? "spruce_planks" : "stripped_spruce_wood[axis=y]");
                } else if (d < RING + 0.5) {
                    int h = 3 + (Math.floorMod(x * 7 + z * 3, 3) == 0 ? 1 : 0);
                    for (int y = 1; y <= h; y++) {
                        p.set(x, y, z, "spruce_log[axis=y]");
                    }
                    p.set(x, h + 1, z, "spruce_fence");
                }
            }
        }
        // Lagerfeuer mit Steinkreis
        p.set(0, 1, 0, "campfire[lit=true,signal_fire=false,facing=north]");
        for (int[] s : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            p.set(s[0], 1, s[1], "cobblestone_slab[type=bottom]");
        }
        // Bänke um das Feuer
        for (int i = -2; i <= 2; i++) {
            p.set(i, 1, 4, "spruce_stairs[facing=south,half=bottom,shape=straight]");
            p.set(4, 1, i, "spruce_stairs[facing=east,half=bottom,shape=straight]");
            p.set(-4, 1, i, "spruce_stairs[facing=west,half=bottom,shape=straight]");
        }
        // Kartentische
        for (int[] t : TABLES) {
            for (int dx = -1; dx <= 1; dx++) {
                p.set(t[0] + dx, 1, t[1], "spruce_fence");
                p.set(t[0] + dx, 2, t[1], "spruce_pressure_plate");
            }
            p.set(t[0], 2, t[1], "cartography_table");
        }
        // Bestenliste: Tafel aus dunklem Holz
        for (int dx = -3; dx <= 3; dx++) {
            p.set(BOARD[0] + dx, 1, BOARD[1] - 1, "dark_oak_planks");
            p.set(BOARD[0] + dx, 2, BOARD[1] - 1, "dark_oak_planks");
            p.set(BOARD[0] + dx, 3, BOARD[1] - 1, "dark_oak_planks");
        }
        // Fässer, Waffenständer, Laternen
        for (int[] b : new int[][] {{-11, 5}, {-11, 6}, {11, 5}, {10, 7}}) {
            p.set(b[0], 1, b[1], "barrel[facing=up,open=false]");
        }
        for (int[] l : new int[][] {{-10, -9}, {10, -9}, {-10, 9}, {10, 9}}) {
            Decor.lanternPost(p, l[0], 1, l[1], "spruce_fence");
        }
        p.set(-12, 1, -2, "grindstone[face=floor,facing=north]");
        p.set(12, 1, -2, "smithing_table");
        p.set(12, 1, 2, "anvil[facing=north]");
        for (int i = 0; i < 12; i++) {
            int x = r.nextInt(36) - 18;
            int z = r.nextInt(36) - 18;
            if (Math.hypot(x, z) > RING + 2 && p.get(x, 0, z) != null && p.get(x, 1, z) == null) {
                Decor.spruce(p, x, 1, z, 4 + r.nextInt(3), r);
            }
        }
        p.connect();
        return p;
    }
}
