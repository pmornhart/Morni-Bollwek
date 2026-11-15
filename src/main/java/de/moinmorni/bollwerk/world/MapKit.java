package de.moinmorni.bollwerk.world;

import de.moinmorni.bollwerk.logic.DefenceType;
import java.util.List;
import java.util.Random;

/**
 * Gemeinsame Bausteine der Karten: Wege, Bauplätze, Alpenhäuser, Gegner-Lager. Eigene Entwürfe, nur
 * Minecraft-Blöcke.
 */
final class MapKit {

    private MapKit() {
    }

    /** Abstand eines Punkts zur Strecke a–b (waagrecht). */
    static double segmentDistance(double px, double pz, MapLayout.Point a, MapLayout.Point b) {
        double vx = b.x() - a.x();
        double vz = b.z() - a.z();
        double len2 = vx * vx + vz * vz;
        double t = len2 == 0 ? 0 : ((px - a.x()) * vx + (pz - a.z()) * vz) / len2;
        t = Math.max(0, Math.min(1, t));
        double cx = a.x() + t * vx;
        double cz = a.z() + t * vz;
        return Math.hypot(px - cx, pz - cz);
    }

    /** Abstand zum nächsten Weg (waagrecht). */
    static double laneDistance(MapLayout layout, double x, double z) {
        double best = Double.MAX_VALUE;
        for (MapLayout.Lane lane : layout.lanes()) {
            List<MapLayout.Point> pts = lane.path();
            for (int i = 1; i < pts.size(); i++) {
                best = Math.min(best, segmentDistance(x, z, pts.get(i - 1), pts.get(i)));
            }
        }
        return best;
    }

    /** Steht dieser Block auf einem Weg (Breite 3) oder einem Bauplatz? Dann bleibt er frei. */
    static boolean reserved(MapLayout layout, int x, int z, double pad) {
        if (laneDistance(layout, x + 0.5, z + 0.5) <= 1.6 + pad) {
            return true;
        }
        for (MapLayout.Spot s : layout.spots()) {
            if (Math.abs(s.x() - x) <= 1 + pad && Math.abs(s.z() - z) <= 1 + pad) {
                return true;
            }
        }
        return layout.heart().flat(x + 0.5, z + 0.5) <= 3 + pad;
    }

    /** Wege belegen: Oberfläche bei y = 0 ({@code inner} entscheidet zwischen Pflaster und Feldweg). */
    static void paths(Plan p, MapLayout layout, java.util.function.BiPredicate<Integer, Integer> inner, Random r) {
        int[] b = {-80, 80};
        for (int x = b[0]; x <= b[1]; x++) {
            for (int z = b[0]; z <= b[1]; z++) {
                if (laneDistance(layout, x + 0.5, z + 0.5) > 1.6) {
                    continue;
                }
                int y = surface(p, x, z);
                if (y == Integer.MIN_VALUE) {
                    continue;
                }
                String block = inner.test(x, z)
                        ? Decor.pick(r, "cobblestone", "stone_bricks", "stone_bricks", "andesite", "mossy_cobblestone")
                        : Decor.pick(r, "dirt_path", "dirt_path", "coarse_dirt", "gravel", "dirt_path");
                p.set(x, y, z, block);
            }
        }
    }

    /** Höchster Block einer Säule im Bereich y −10 … 0 (oder MIN_VALUE). */
    static int surface(Plan p, int x, int z) {
        for (int y = 0; y >= -10; y--) {
            if (p.get(x, y, z) != null) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Über den Wegen bis {@code height} alles freiräumen (Bäume, Felsen, Deko). */
    static void clearLanes(Plan p, MapLayout layout, int height) {
        for (int x = -80; x <= 80; x++) {
            for (int z = -80; z <= 80; z++) {
                if (laneDistance(layout, x + 0.5, z + 0.5) > 1.6) {
                    continue;
                }
                int s = surface(p, x, z);
                if (s == Integer.MIN_VALUE) {
                    continue;
                }
                for (int y = s + 1; y <= s + height; y++) {
                    p.clear(x, y, z);
                }
            }
        }
    }

    /** Bauplätze: an den Wegen ein Querstreifen aus Meißel-Steinziegeln, daneben eine 3×3-Platte. */
    static void spots(Plan p, MapLayout layout) {
        for (MapLayout.Spot s : layout.spots()) {
            int y = s.y() - 1;
            if (s.kind() == DefenceType.Spot.LANE) {
                for (int i = -1; i <= 1; i++) {
                    int x = s.acrossX() ? s.x() + i : s.x();
                    int z = s.acrossX() ? s.z() : s.z() + i;
                    p.set(x, y, z, "chiseled_stone_bricks");
                }
            } else {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        p.set(s.x() + dx, y, s.z() + dz, dx == 0 && dz == 0 ? "chiseled_stone_bricks" : "smooth_stone");
                        for (int i = 1; i <= 4; i++) {
                            p.clear(s.x() + dx, y + i, s.z() + dz);
                        }
                    }
                }
            }
        }
    }

    /** Lager der Angreifer am Anfang eines Wegs: Lagerfeuer, schwarze Fahne, Zeltplane. */
    static void camp(Plan p, MapLayout.Lane lane, int side, int floorY) {
        MapLayout.Point a = lane.path().get(0);
        MapLayout.Point b = lane.path().get(1);
        double len = Math.max(0.001, Math.hypot(b.x() - a.x(), b.z() - a.z()));
        int x = (int) Math.floor(a.x() - (b.z() - a.z()) / len * 5 * side);
        int z = (int) Math.floor(a.z() + (b.x() - a.x()) / len * 5 * side);
        p.set(x, floorY, z, "campfire[lit=true,signal_fire=false,facing=north]");
        p.set(x + side * 2, floorY, z, "dark_oak_fence");
        p.set(x + side * 2, floorY + 1, z, "dark_oak_fence");
        p.set(x + side * 2, floorY + 2, z, "black_banner[rotation=0]");
        for (int i = -1; i <= 1; i++) {
            p.setIfEmpty(x + i, floorY, z + 2, "brown_wool");
            p.setIfEmpty(x + i, floorY + 1, z + 2, "brown_carpet");
        }
    }

    /**
     * Alpenhaus: weiß verputztes Erdgeschoss, Holz-Obergeschoss mit Balkon, flaches Satteldach mit Überstand,
     * Fensterläden, Blumenkästen. {@code doorSouth}: Tür nach Süden (sonst Norden). Steht auf y = 1.
     */
    static void house(Plan p, int x1, int z1, int x2, int z2, boolean doorSouth, Random r) {
        String plaster = Decor.pick(r, "white_terracotta", "smooth_quartz", "white_concrete", "calcite");
        String wood = Decor.pick(r, "spruce", "dark_oak", "spruce");
        int h1 = 3;
        int h2 = 3;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                p.set(x, 0, z, "stone_bricks");
                for (int y = 1; y <= h1 + h2; y++) {
                    if (!edge) {
                        p.clear(x, y, z);
                        continue;
                    }
                    String block;
                    if (corner) {
                        block = wood + "_log[axis=y]";
                    } else if (y <= h1) {
                        block = plaster;
                    } else {
                        block = wood + "_planks";
                    }
                    p.set(x, y, z, block);
                }
                if (!edge) {
                    p.set(x, h1 + 1, z, wood + "_planks");
                }
            }
        }
        // Fenster
        for (int y : new int[] {2, h1 + 2}) {
            for (int x = x1 + 2; x <= x2 - 2; x += 2) {
                window(p, x, y, z1, "north", wood);
                window(p, x, y, z2, "south", wood);
            }
            for (int z = z1 + 2; z <= z2 - 2; z += 2) {
                p.set(x1, y, z, "glass_pane");
                p.set(x2, y, z, "glass_pane");
            }
        }
        // Tür
        int dx = (x1 + x2) / 2;
        int dz = doorSouth ? z2 : z1;
        String face = doorSouth ? "south" : "north";
        p.set(dx, 1, dz, wood + "_door[facing=" + face + ",half=lower,hinge=left,open=false]");
        p.set(dx, 2, dz, wood + "_door[facing=" + face + ",half=upper,hinge=left,open=false]");
        // Balkon
        int bz = doorSouth ? z2 + 1 : z1 - 1;
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            p.set(x, h1 + 1, bz, wood + "_slab[type=top]");
            p.set(x, h1 + 2, bz, wood + "_fence");
        }
        // Satteldach: First in z-Richtung, Überstand ringsum
        int top = h1 + h2 + 1;
        for (int i = 0;; i++) {
            int left = x1 - 1 + i;
            int right = x2 + 1 - i;
            int y = top + i - 1;
            if (left > right) {
                break;
            }
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                if (left == right) {
                    p.set(left, y, z, wood + "_slab[type=bottom]");
                    continue;
                }
                p.set(left, y, z, wood + "_stairs[facing=east,half=bottom,shape=straight]");
                p.set(right, y, z, wood + "_stairs[facing=west,half=bottom,shape=straight]");
                if (z == z1 || z == z2) {
                    for (int x = left + 1; x < right; x++) {
                        p.set(x, y, z, wood + "_planks");
                    }
                }
            }
        }
        // Kamin
        for (int y = top; y <= top + 3; y++) {
            p.set(x2 - 1, y, z1 + 1, "bricks");
        }
        p.set(x2 - 1, top + 4, z1 + 1, "campfire[lit=true,signal_fire=false,facing=north]");
    }

    private static void window(Plan p, int x, int y, int z, String face, String wood) {
        p.set(x, y, z, "glass_pane");
        int out = "north".equals(face) ? -1 : 1;
        p.set(x, y - 1, z + out, wood + "_trapdoor[facing=" + face + ",half=top,open=false]");
        p.set(x, y, z + out, Decor.pick(new Random(x * 31L + y * 7L + z), "potted_red_tulip", "potted_poppy",
                "potted_cornflower", "potted_azure_bluet"));
        p.set(x - 1, y, z + out, wood + "_trapdoor[facing=" + face + ",half=bottom,open=true]");
        p.set(x + 1, y, z + out, wood + "_trapdoor[facing=" + face + ",half=bottom,open=true]");
    }

    /** Schwebende Insel mit Wiese oben (y = 0), Erde darunter, Fels spitz zulaufend. */
    static void meadow(Plan p, int radius, int depth, Random r) {
        Decor.island(p, radius, depth, r, "dirt", 2);
        int edge = radius + 3;
        for (int x = -edge; x <= edge; x++) {
            for (int z = -edge; z <= edge; z++) {
                if (p.get(x, -1, z) != null) {
                    p.set(x, 0, z, "grass_block[snowy=false]");
                }
            }
        }
    }

    /** Ein paar Blumen und Gras auf freien Wiesenblöcken. */
    static void flowers(Plan p, MapLayout layout, int radius, Random r) {
        String[] flowers = {"short_grass", "short_grass", "short_grass", "dandelion", "poppy", "cornflower",
            "azure_bluet", "oxeye_daisy"};
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                var d = p.get(x, 0, z);
                if (d == null || d.getMaterial() != org.bukkit.Material.GRASS_BLOCK || p.get(x, 1, z) != null
                        || reserved(layout, x, z, 0) || r.nextInt(6) != 0) {
                    continue;
                }
                p.set(x, 1, z, flowers[r.nextInt(flowers.length)]);
            }
        }
    }
}
