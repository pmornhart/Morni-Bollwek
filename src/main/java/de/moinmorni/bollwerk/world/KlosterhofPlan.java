package de.moinmorni.bollwerk.world;

import java.util.Random;

/**
 * „Klosterhof“: ein Kloster auf einer Felsnase hoch über dem Tal. Kirche mit Zwiebelturm im Norden, Konventbau im
 * Westen, Kräutergarten im Süden; mitten im Kreuzgang die Klosterglocke – das Herz. Die Angreifer kommen über den
 * Pilgerweg (Treppe von Süden), über die Hängebrücke (Osten) und den schmalen Gratweg (Westen). Eigener Entwurf.
 */
final class KlosterhofPlan {

    private static final int PLATEAU = 24;
    private static final int WALL = 17;

    private KlosterhofPlan() {
    }

    static Plan build(MapLayout layout) {
        Plan p = new Plan();
        Random r = new Random(1517);
        ground(p, layout, r);
        backdrop(p, r);
        MapKit.paths(p, layout, (x, z) -> Math.abs(x) <= WALL && Math.abs(z) <= WALL, r);
        ramp(p);
        bridge(p);
        cloister(p, r);
        church(p);
        convent(p, r);
        garden(p, r);
        heart(p);
        trees(p, layout, r);
        for (int i = 0; i < layout.lanes().size(); i++) {
            MapLayout.Lane lane = layout.lanes().get(i);
            MapKit.camp(p, lane, i % 2 == 0 ? 1 : -1, (int) Math.round(lane.start().y()));
        }
        MapKit.clearLanes(p, layout, 4);
        MapKit.spots(p, layout);
        MapKit.flowers(p, layout, 52, r);
        p.connect();
        return p;
    }

    /** Höhe der Oberfläche (oder MIN_VALUE, wenn hier Luft ist). */
    static int floor(MapLayout layout, int x, int z) {
        double sx = x + 0.5;
        double sz = z + 0.5;
        double rr = Math.pow(Math.pow(Math.abs(sx), 4) + Math.pow(Math.abs(sz), 4), 0.25);
        if (rr <= PLATEAU) {
            return 0;
        }
        // Pilgerweg-Absatz im Süden
        if (Math.abs(x) <= 7 && z >= 22 && z <= 50) {
            return z >= 35 ? -5 : z <= 25 ? 0 : -5 + (int) Math.ceil((35 - z) / 2.0);
        }
        // Felsen im Osten (Ende der Hängebrücke)
        if (x >= 37 && x <= 50 && Math.abs(z) <= 7) {
            return 0;
        }
        // Grat im Westen
        MapLayout.Lane west = layout.lanes().get(2);
        for (int i = 1; i < west.path().size(); i++) {
            if (x < -PLATEAU + 2 && MapKit.segmentDistance(sx, sz, west.path().get(i - 1), west.path().get(i)) <= 4.5) {
                return 0;
            }
        }
        if (x <= -40 && x >= -50 && z >= -22 && z <= -7) {
            return 0;
        }
        return Integer.MIN_VALUE;
    }

    private static void ground(Plan p, MapLayout layout, Random r) {
        for (int x = -56; x <= 56; x++) {
            for (int z = -56; z <= 56; z++) {
                int top = floor(layout, x, z);
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                double rr = Math.hypot(x, z);
                int depth = rr <= PLATEAU ? 30 - (int) (rr / 3) + r.nextInt(3) : 10 + r.nextInt(4);
                p.set(x, top, z, r.nextInt(9) == 0 ? "moss_block" : "grass_block[snowy=false]");
                for (int d = 1; d <= depth; d++) {
                    p.set(x, top - d, z, d <= 2 ? "dirt" : Decor.rock(r, d));
                }
            }
        }
    }

    /** Ferne Gipfel als Kulisse. */
    private static void backdrop(Plan p, Random r) {
        Decor.mountain(p, -10, -64, -14, 24, 44, r);
        Decor.mountain(p, 34, -58, -14, 18, 34, r);
        Decor.mountain(p, -46, -50, -14, 16, 30, r);
    }

    /** Treppe vom Pilgerweg-Absatz hinauf (Stufen dort, wo es eins höher geht). */
    private static void ramp(Plan p) {
        for (int z = 26; z <= 34; z++) {
            int top = -5 + (int) Math.ceil((35 - z) / 2.0);
            int below = -5 + (int) Math.ceil((35 - (z + 1)) / 2.0);
            for (int x = -7; x <= 7; x++) {
                if (top > below && Math.abs(x) <= 2) {
                    p.set(x, top, z, "stone_brick_stairs[facing=north,half=bottom,shape=straight]");
                    p.set(x, top - 1, z, "stone_bricks");
                } else if (Math.abs(x) <= 2) {
                    p.set(x, top, z, "stone_bricks");
                }
            }
            p.set(-3, top + 1, z, "stone_brick_wall");
            p.set(3, top + 1, z, "stone_brick_wall");
        }
        p.set(-3, 0, 27, "lantern[hanging=false]");
        p.set(3, 0, 27, "lantern[hanging=false]");
    }

    private static void bridge(Plan p) {
        for (int x = 23; x <= 37; x++) {
            for (int z = -1; z <= 1; z++) {
                p.set(x, 0, z, x % 3 == 0 ? "stripped_spruce_log[axis=x]" : "spruce_planks");
            }
            p.set(x, -1, -1, "spruce_slab[type=top]");
            p.set(x, -1, 1, "spruce_slab[type=top]");
            p.set(x, 1, -2, "spruce_fence");
            p.set(x, 1, 2, "spruce_fence");
            p.set(x, 0, -2, "spruce_planks");
            p.set(x, 0, 2, "spruce_planks");
            if (x % 4 == 0) {
                p.set(x, 2, -2, "spruce_fence");
                p.set(x, 2, 2, "spruce_fence");
                p.set(x, 3, -2, "lantern[hanging=false]");
                p.set(x, 3, 2, "lantern[hanging=false]");
            }
        }
    }

    /** Klostermauer: weiß verputzt, Ziegelkappe, drei Tore. */
    private static void cloister(Plan p, Random r) {
        for (int i = -WALL; i <= WALL; i++) {
            for (int[] xz : new int[][] {{i, -WALL}, {i, WALL}, {-WALL, i}, {WALL, i}}) {
                for (int y = 1; y <= 4; y++) {
                    p.set(xz[0], y, xz[1], y == 1 ? "stone_bricks" : r.nextInt(12) == 0 ? "white_terracotta" : "calcite");
                }
                p.set(xz[0], 5, xz[1], "brick_slab[type=bottom]");
            }
        }
        // Tore mit Rundbogen
        gate(p, 0, WALL, true);
        gate(p, WALL, 0, false);
        gate(p, -WALL, 0, false);
        // Kreuzgang-Pflaster
        for (int x = -WALL + 1; x < WALL; x++) {
            for (int z = -WALL + 1; z < WALL; z++) {
                boolean walk = Math.abs(x) >= WALL - 3 || Math.abs(z) >= WALL - 3 || Math.abs(x) <= 1 || Math.abs(z) <= 1;
                if (walk) {
                    p.set(x, 0, z, Decor.lozenge(x, z, 2) ? "polished_andesite" : "stone_bricks");
                }
            }
        }
        // Säulengang innen an der Mauer (Bögen aus Mauerblöcken, Dach aus Stufen)
        for (int i = -WALL + 2; i <= WALL - 2; i++) {
            for (int[] xz : new int[][] {{i, -WALL + 3}, {i, WALL - 3}, {-WALL + 3, i}, {WALL - 3, i}}) {
                if (Math.abs(xz[0]) <= 2 || Math.abs(xz[1]) <= 2) {
                    continue;
                }
                boolean pillar = Math.floorMod(i, 3) == 0;
                if (pillar) {
                    p.set(xz[0], 1, xz[1], "stone_brick_wall");
                    p.set(xz[0], 2, xz[1], "stone_brick_wall");
                    p.set(xz[0], 3, xz[1], "chiseled_stone_bricks");
                } else {
                    p.set(xz[0], 3, xz[1], "stone_brick_slab[type=top]");
                }
            }
        }
        for (int x = -WALL + 1; x < WALL; x++) {
            for (int z = -WALL + 1; z < WALL; z++) {
                boolean roof = (Math.abs(x) >= WALL - 3 || Math.abs(z) >= WALL - 3) && Math.abs(x) > 2 && Math.abs(z) > 2;
                if (roof) {
                    p.set(x, 4, z, "spruce_slab[type=bottom]");
                }
            }
        }
    }

    private static void gate(Plan p, int x, int z, boolean alongX) {
        for (int i = -1; i <= 1; i++) {
            int gx = alongX ? x + i : x;
            int gz = alongX ? z : z + i;
            for (int y = 1; y <= 3; y++) {
                p.clear(gx, y, gz);
            }
            p.set(gx, 4, gz, "stone_bricks");
        }
        for (int i : new int[] {-2, 2}) {
            int gx = alongX ? x + i : x;
            int gz = alongX ? z : z + i;
            for (int y = 1; y <= 6; y++) {
                p.set(gx, y, gz, "stone_bricks");
            }
            p.set(gx, 7, gz, "lantern[hanging=false]");
        }
    }

    /** Kirche mit Zwiebelturm. */
    private static void church(Plan p) {
        int x1 = -9;
        int x2 = 5;
        int z1 = -16;
        int z2 = -8;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                p.set(x, 0, z, "polished_andesite");
                for (int y = 1; y <= 8; y++) {
                    if (edge) {
                        p.set(x, y, z, y == 1 ? "stone_bricks" : "calcite");
                    } else {
                        p.clear(x, y, z);
                    }
                }
            }
        }
        for (int x = x1 + 2; x <= x2 - 2; x += 3) {
            for (int y = 3; y <= 6; y++) {
                p.set(x, y, z2, y == 6 ? "yellow_stained_glass_pane" : "light_blue_stained_glass_pane");
                p.set(x, y, z1, "light_blue_stained_glass_pane");
            }
        }
        // Portal
        p.set(-2, 1, z2, "spruce_door[facing=south,half=lower,hinge=left,open=false]");
        p.set(-2, 2, z2, "spruce_door[facing=south,half=upper,hinge=left,open=false]");
        p.set(-1, 1, z2, "spruce_door[facing=south,half=lower,hinge=right,open=false]");
        p.set(-1, 2, z2, "spruce_door[facing=south,half=upper,hinge=right,open=false]");
        // Steiles Dach (First in x-Richtung)
        for (int i = 0; i <= 5; i++) {
            int y = 9 + i;
            int a = z1 - 1 + i;
            int b = z2 + 1 - i;
            if (a > b) {
                break;
            }
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                if (a == b) {
                    p.set(x, y, a, "deepslate_tile_slab[type=bottom]");
                    continue;
                }
                p.set(x, y, a, "deepslate_tile_stairs[facing=south,half=bottom,shape=straight]");
                p.set(x, y, b, "deepslate_tile_stairs[facing=north,half=bottom,shape=straight]");
                if (x == x1 || x == x2) {
                    for (int z = a + 1; z < b; z++) {
                        p.set(x, y, z, "calcite");
                    }
                }
            }
        }
        // Zwiebelturm
        int tx = 8;
        int tz = -12;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int y = 1; y <= 16; y++) {
                    if (edge) {
                        p.set(tx + dx, y, tz + dz, y == 1 ? "stone_bricks" : (y == 13 || y == 14) && (dx == 0 || dz == 0)
                                ? "air" : "calcite");
                    }
                }
            }
        }
        p.set(tx, 13, tz, "bell[attachment=ceiling,facing=north]");
        p.set(tx, 14, tz, "spruce_planks");
        // Zwiebel aus Kupfer
        int[] radii = {3, 3, 2, 2, 1, 1, 0};
        for (int i = 0; i < radii.length; i++) {
            int rad = radii[i];
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx * dx + dz * dz <= rad * rad + 1) {
                        p.set(tx + dx, 17 + i, tz + dz, "oxidized_cut_copper");
                    }
                }
            }
        }
        p.set(tx, 24, tz, "lightning_rod[facing=up]");
        p.set(tx, 25, tz, "gold_block");
    }

    /** Konventbau im Westen (Wohnhaus der Klosterleute). */
    private static void convent(Plan p, Random r) {
        MapKit.house(p, -16, 7, -9, 15, false, r);
    }

    /** Kräutergarten im Südosten und Südwesten (Beete, die nie wachsen oder welken). */
    private static void garden(Plan p, Random r) {
        String[] herbs = {"allium", "blue_orchid", "cornflower", "lily_of_the_valley", "azure_bluet", "oxeye_daisy"};
        for (int x = -7; x <= 7; x++) {
            for (int z = 4; z <= 9; z++) {
                if (Math.abs(x) <= 2 || Math.abs(x) == 5) {
                    continue;
                }
                p.set(x, 0, z, "rooted_dirt");
                p.set(x, 1, z, herbs[r.nextInt(herbs.length)]);
            }
        }
        for (int x = -7; x <= 7; x++) {
            p.set(x, 1, 10, Math.abs(x) <= 1 ? "air" : "spruce_fence");
        }
    }

    /** Klosterglocke: offenes Glockenhaus auf vier Säulen, Pyramidendach. */
    private static void heart(Plan p) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                p.set(x, 0, z, Math.abs(x) == 2 || Math.abs(z) == 2 ? "polished_deepslate" : "chiseled_stone_bricks");
            }
        }
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            for (int y = 1; y <= 4; y++) {
                p.set(c[0], y, c[1], y == 4 ? "chiseled_stone_bricks" : "polished_andesite_wall");
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                p.set(x, 5, z, "spruce_planks");
            }
        }
        p.set(0, 5, 0, "spruce_planks");
        p.set(0, 4, 0, "bell[attachment=ceiling,facing=north]");
        Decor.towerRoof(p, 0, 6, 0, 2, "dark_oak_stairs", "dark_oak_planks", 2);
    }

    private static void trees(Plan p, MapLayout layout, Random r) {
        for (int i = 0; i < 90; i++) {
            int x = r.nextInt(112) - 56;
            int z = r.nextInt(112) - 56;
            if (Math.abs(x) <= WALL + 2 && Math.abs(z) <= WALL + 2) {
                continue;
            }
            int top = floor(layout, x, z);
            if (top == Integer.MIN_VALUE || MapKit.reserved(layout, x, z, 2.5) || p.get(x, top + 1, z) != null
                    || Math.abs(z) <= 4 && x > 20) {
                continue;
            }
            Decor.spruce(p, x, top + 1, z, 5 + r.nextInt(4), r);
        }
    }
}
