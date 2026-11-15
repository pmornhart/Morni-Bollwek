package de.moinmorni.bollwerk.world;

import java.util.Random;

/**
 * „Talsperre Morni-Dorf“: ein ummauertes Bergdorf auf einer Alm. Mitten auf dem Dorfplatz der Dorfbrunnen mit
 * Maibaum – das Herz, das verteidigt wird. Drei Wege führen herein: das Nordtor mit Torhaus, eine Bresche in der
 * Ostmauer und das kleine Almtor im Westen. Im Süden schützen Berge. Eigener Entwurf.
 */
final class TalsperrePlan {

    private static final int WALL = 22;
    private static final int WALL_TOP = 5;
    private static final int RADIUS = 54;

    private TalsperrePlan() {
    }

    static Plan build(MapLayout layout) {
        Plan p = new Plan();
        Random r = new Random(1848);
        MapKit.meadow(p, RADIUS, 24, r);
        mountains(p, layout, r);
        MapKit.paths(p, layout, (x, z) -> Math.abs(x) < WALL && Math.abs(z) < WALL, r);
        plaza(p);
        streets(p, r);
        walls(p, r);
        gatehouse(p);
        houses(p, r);
        heart(p);
        trees(p, layout, r);
        for (int i = 0; i < layout.lanes().size(); i++) {
            MapKit.camp(p, layout.lanes().get(i), i % 2 == 0 ? 1 : -1, 1);
        }
        MapKit.clearLanes(p, layout, 4);
        MapKit.spots(p, layout);
        MapKit.flowers(p, layout, RADIUS, r);
        p.connect();
        return p;
    }

    /** Berge außerhalb der Mauer – nur dort, wo kein Weg ist. */
    private static void mountains(Plan p, MapLayout layout, Random r) {
        int[][] peaks = {{-38, 38, 15, 24}, {38, 38, 15, 22}, {0, 46, 13, 26}, {40, -40, 12, 18}, {-42, -38, 11, 16},
            {-22, 46, 10, 18}, {22, 46, 10, 17}};
        for (int[] pk : peaks) {
            Plan m = new Plan();
            Decor.mountain(m, pk[0], pk[1], 0, pk[2], pk[3], r);
            m.forEach((k, d) -> {
                int x = Plan.keyX(k);
                int y = Plan.keyY(k);
                int z = Plan.keyZ(k);
                if (Math.abs(x) <= WALL + 3 && Math.abs(z) <= WALL + 3) {
                    return;
                }
                if (MapKit.laneDistance(layout, x + 0.5, z + 0.5) < 6 || y < 0) {
                    return;
                }
                if (p.get(x, 0, z) == null) {
                    return;
                }
                p.set(x, y, z, d);
            });
        }
    }

    private static void plaza(Plan p) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                if (x * x + z * z <= 64) {
                    p.set(x, 0, z, Decor.lozenge(x, z, 2) ? "polished_andesite" : "stone_bricks");
                }
            }
        }
    }

    /** Gasse nach Süden (kein Weg der Gegner) zum Startplatz. */
    private static void streets(Plan p, Random r) {
        for (int z = 8; z < WALL; z++) {
            for (int x = -1; x <= 1; x++) {
                p.set(x, 0, z, Decor.pick(r, "cobblestone", "stone_bricks", "andesite"));
            }
        }
    }

    private static void walls(Plan p, Random r) {
        for (int i = -WALL; i <= WALL; i++) {
            for (int[] xz : new int[][] {{i, -WALL}, {i, WALL}, {-WALL, i}, {WALL, i}}) {
                int x = xz[0];
                int z = xz[1];
                for (int y = 1; y <= WALL_TOP; y++) {
                    p.set(x, y, z, r.nextInt(5) == 0 ? "mossy_stone_bricks" : r.nextInt(7) == 0 ? "cracked_stone_bricks"
                            : "stone_bricks");
                }
                if (Math.floorMod(i, 2) == 0) {
                    p.set(x, WALL_TOP + 1, z, "stone_brick_wall");
                }
            }
        }
        // Wehrgang innen (Holz), Ecktürme
        for (int[] c : new int[][] {{-WALL, -WALL}, {WALL, -WALL}, {-WALL, WALL}, {WALL, WALL}}) {
            tower(p, c[0], c[1], 8);
        }
        // Almtor (Westen): Lücke mit Bogen
        for (int z = -1; z <= 1; z++) {
            for (int y = 1; y <= 3; y++) {
                p.clear(-WALL, y, z);
            }
            p.set(-WALL, 4, z, "spruce_planks");
        }
        p.set(-WALL - 1, 3, -2, "lantern[hanging=false]");
        p.set(-WALL - 1, 3, 2, "lantern[hanging=false]");
        // Bresche (Osten): eingestürzt, Schutt seitlich
        for (int z = -2; z <= 2; z++) {
            for (int y = 1; y <= WALL_TOP + 1; y++) {
                p.clear(WALL, y, z);
            }
        }
        for (int z : new int[] {-3, 3}) {
            p.clear(WALL, WALL_TOP, z);
            p.clear(WALL, WALL_TOP + 1, z);
            p.set(WALL + 1, 1, z, "cobblestone");
            p.set(WALL - 1, 1, z, "cobblestone_slab[type=bottom]");
            p.set(WALL + 1, 1, z * 4 / 3, "mossy_cobblestone_slab[type=bottom]");
        }
    }

    /** Runder Wehrturm mit Spitzdach. */
    private static void tower(Plan p, int cx, int cz, int height) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    continue;
                }
                for (int y = 1; y <= height; y++) {
                    boolean shell = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    if (shell) {
                        p.set(cx + dx, y, cz + dz, y == height - 2 && (dx == 0 || dz == 0) ? "spruce_trapdoor[facing=north,half=bottom,open=true]"
                                : "stone_bricks");
                    } else {
                        p.set(cx + dx, y, cz + dz, "stone_bricks");
                    }
                }
            }
        }
        Decor.towerRoof(p, cx, height + 1, cz, 2, "spruce_stairs", "spruce_planks", Math.floorMod(cx + cz, 3));
    }

    /** Nordtor mit Torhaus: zwei Türme, Bogen über der Durchfahrt, Fallgatter oben, Banner. */
    private static void gatehouse(Plan p) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 4; y++) {
                p.clear(x, y, -WALL);
            }
            for (int y = 5; y <= 8; y++) {
                p.set(x, y, -WALL, "stone_bricks");
                p.set(x, y, -WALL - 1, y == 5 ? "stone_brick_stairs[facing=south,half=top,shape=straight]" : "stone_bricks");
            }
            p.set(x, 4, -WALL - 1, "iron_bars");
            p.set(x, 9, -WALL, "stone_brick_wall");
        }
        tower(p, -4, -WALL, 10);
        tower(p, 4, -WALL, 10);
        p.banner(-1, 7, -WALL - 2, Decor.bannerBlock(2, true, "north"), Decor.bannerPatterns(2));
        p.banner(1, 7, -WALL - 2, Decor.bannerBlock(0, true, "north"), Decor.bannerPatterns(0));
        p.set(-2, 3, -WALL - 1, "lantern[hanging=false]");
        p.set(2, 3, -WALL - 1, "lantern[hanging=false]");
    }

    private static void houses(Plan p, Random r) {
        // Nordost, Nordwest, Südost, Südwest – je zwei Häuser
        MapKit.house(p, 8, -20, 14, -14, true, r);
        MapKit.house(p, 15, -12, 20, -8, false, r);
        MapKit.house(p, -14, -20, -8, -14, true, r);
        MapKit.house(p, -20, -12, -15, -8, false, r);
        MapKit.house(p, 8, 9, 14, 15, false, r);
        MapKit.house(p, 15, 16, 20, 20, false, r);
        MapKit.house(p, -14, 9, -8, 15, false, r);
        MapKit.house(p, -20, 16, -15, 20, false, r);
        // Kleinkram: Bänke, Holzstapel, Brunnenrand, Blumen
        for (int[] b : new int[][] {{-3, 10}, {3, 12}}) {
            p.set(b[0], 1, b[1], "spruce_stairs[facing=east,half=bottom,shape=straight]");
        }
        for (int z = 16; z <= 19; z++) {
            p.set(3, 1, z, "spruce_log[axis=z]");
            p.set(-3, 1, z, "spruce_log[axis=z]");
        }
    }

    /** Dorfbrunnen mit Maibaum: Steinrand, Wasser, weiß-blau gestreifter Baum mit Kranz und Fahnen. */
    private static void heart(Plan p) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean rim = Math.abs(x) == 2 || Math.abs(z) == 2;
                p.set(x, 0, z, "stone_bricks");
                if (rim) {
                    p.set(x, 1, z, Math.abs(x) == 2 && Math.abs(z) == 2 ? "chiseled_stone_bricks" : "stone_brick_slab[type=bottom]");
                } else if (x != 0 || z != 0) {
                    p.set(x, 0, z, "water[level=0]");
                }
            }
        }
        for (int y = 0; y <= 14; y++) {
            p.set(0, y, 0, (y / 2) % 2 == 0 ? "white_concrete" : "light_blue_concrete");
        }
        // Kranz
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    p.set(dx, 12, dz, "spruce_leaves[persistent=true]");
                }
            }
        }
        p.set(0, 15, 0, "gold_block");
        // Fahnen auf drei Höhen
        int design = 0;
        for (int y : new int[] {6, 9}) {
            p.banner(1, y, 0, Decor.bannerBlock(design, true, "east"), Decor.bannerPatterns(design));
            p.banner(-1, y, 0, Decor.bannerBlock(design + 1, true, "west"), Decor.bannerPatterns(design + 1));
            p.banner(0, y, 1, Decor.bannerBlock(design + 2, true, "south"), Decor.bannerPatterns(design + 2));
            p.banner(0, y, -1, Decor.bannerBlock(design, true, "north"), Decor.bannerPatterns(design));
            design++;
        }
        // Laternen am Platzrand
        for (int[] l : new int[][] {{-6, -3}, {6, 3}, {-3, 6}, {3, -6}}) {
            Decor.lanternPost(p, l[0], 1, l[1], "spruce_fence");
        }
    }

    private static void trees(Plan p, MapLayout layout, Random r) {
        for (int i = 0; i < 70; i++) {
            int x = r.nextInt(2 * RADIUS) - RADIUS;
            int z = r.nextInt(2 * RADIUS) - RADIUS;
            if (Math.abs(x) <= WALL + 3 && Math.abs(z) <= WALL + 3) {
                continue;
            }
            if (MapKit.reserved(layout, x, z, 3) || p.get(x, 0, z) == null || p.get(x, 1, z) != null
                    || Math.hypot(x, z) > RADIUS - 4) {
                continue;
            }
            Decor.spruce(p, x, 1, z, 5 + r.nextInt(4), r);
        }
    }
}
