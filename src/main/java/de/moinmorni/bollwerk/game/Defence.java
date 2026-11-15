package de.moinmorni.bollwerk.game;

import de.moinmorni.bollwerk.Holo;
import de.moinmorni.bollwerk.logic.DefenceType;
import de.moinmorni.bollwerk.world.MapLayout;
import de.moinmorni.bollwerk.world.Plan;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.BlockData;

/**
 * Ein gebauter Bau auf einem Bauplatz. Merkt sich, wie die Blöcke vorher aussahen – beim Zerstören, Ausbauen und am
 * Ende der Runde kommt alles wieder so, wie es war.
 */
final class Defence {

    final MapLayout.Spot spot;
    final DefenceType type;
    int level = 1;
    double health;
    int cooldown;
    Holo label;
    private final World world;
    private final int bx;
    private final int by;
    private final int bz;
    private final Map<Block, BlockData> before = new LinkedHashMap<>();

    Defence(World world, int originX, int originZ, int floorY, MapLayout.Spot spot, DefenceType type) {
        this.world = world;
        this.spot = spot;
        this.type = type;
        this.bx = originX + spot.x();
        this.by = floorY + spot.y();
        this.bz = originZ + spot.z();
        this.health = type.health(1);
    }

    /** Mitte am Boden (für Abstände). */
    Location center() {
        return new Location(world, bx + 0.5, by, bz + 0.5);
    }

    /** Hier fliegen die Pfeile des Turms los. */
    Location muzzle() {
        return new Location(world, bx + 0.5, by + (level >= 2 ? 5.6 : 4.6), bz + 0.5);
    }

    double maxHealth() {
        return type.health(level);
    }

    boolean contains(Block b) {
        return before.containsKey(b);
    }

    /** Bauen (oder nach dem Ausbauen neu bauen). */
    void build() {
        restore();
        List<Object[]> parts = new ArrayList<>();
        switch (type) {
            case PALISADE -> palisade(parts);
            case KESSELFALLE -> kessel(parts);
            case PFEILTURM -> tower(parts);
            case HEILBANNER -> banner(parts);
            default -> {
            }
        }
        for (Object[] part : parts) {
            Block b = world.getBlockAt((int) part[0], (int) part[1], (int) part[2]);
            before.putIfAbsent(b, b.getBlockData().clone());
            b.setBlockData(Plan.data((String) part[3]), false);
        }
        if (type == DefenceType.HEILBANNER) {
            Block b = world.getBlockAt(bx, by + 2, bz);
            BlockState st = b.getState();
            if (st instanceof Banner banner) {
                List<Pattern> patterns = new ArrayList<>(List.of(new Pattern(DyeColor.WHITE, PatternType.FLOWER),
                        new Pattern(DyeColor.GREEN, PatternType.BORDER)));
                if (level >= 3) {
                    patterns.add(new Pattern(DyeColor.YELLOW, PatternType.CURLY_BORDER));
                }
                banner.setPatterns(patterns);
                banner.update(true, false);
            }
        }
    }

    /** Alles zurück, wie es vorher war. */
    void restore() {
        List<Map.Entry<Block, BlockData>> list = new ArrayList<>(before.entrySet());
        java.util.Collections.reverse(list);
        for (Map.Entry<Block, BlockData> e : list) {
            e.getKey().setBlockData(e.getValue(), false);
        }
        before.clear();
    }

    private void add(List<Object[]> parts, int dx, int dy, int dz, String spec) {
        parts.add(new Object[] {bx + dx, by + dy, bz + dz, spec});
    }

    private void palisade(List<Object[]> parts) {
        String log = level == 1 ? "spruce_log[axis=y]" : "dark_oak_log[axis=y]";
        int height = level == 1 ? 2 : 3;
        for (int i = -1; i <= 1; i++) {
            int dx = spot.acrossX() ? i : 0;
            int dz = spot.acrossX() ? 0 : i;
            for (int y = 0; y < height; y++) {
                add(parts, dx, y, dz, level == 3 && y == 0 ? "stone_bricks" : log);
            }
            add(parts, dx, height, dz, level == 1 ? "spruce_fence" : "dark_oak_fence");
        }
    }

    private void kessel(List<Object[]> parts) {
        for (int i : new int[] {-2, 2}) {
            int dx = spot.acrossX() ? i : 0;
            int dz = spot.acrossX() ? 0 : i;
            add(parts, dx, 0, dz, level >= 3 ? "soul_campfire[lit=true,signal_fire=false,facing=north]"
                    : "campfire[lit=true,signal_fire=false,facing=north]");
            add(parts, dx, 1, dz, "water_cauldron[level=3]");
            if (level >= 2) {
                int ox = spot.acrossX() ? Integer.signum(i) : 1;
                int oz = spot.acrossX() ? 1 : Integer.signum(i);
                add(parts, dx + (spot.acrossX() ? ox : 0), 0, dz + (spot.acrossX() ? 0 : oz), "barrel[facing=up,open=false]");
            }
        }
    }

    private void tower(List<Object[]> parts) {
        String pillar = level == 1 ? "spruce_log[axis=y]" : "stone_bricks";
        int deck = level == 1 ? 3 : 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = dx != 0 && dz != 0;
                if (corner) {
                    for (int y = 0; y < deck; y++) {
                        add(parts, dx, y, dz, pillar);
                    }
                    add(parts, dx, deck + 1, dz, level == 1 ? "spruce_fence" : "stone_brick_wall");
                }
                add(parts, dx, deck, dz, level == 1 ? "spruce_planks" : "polished_andesite");
            }
        }
        add(parts, 0, deck + 1, 0, "target");
        if (level >= 3) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    add(parts, dx, deck + 2, dz, dx == 0 && dz == 0 ? "lantern[hanging=false]" : "dark_oak_slab[type=bottom]");
                }
            }
        }
    }

    private void banner(List<Object[]> parts) {
        add(parts, 0, 0, 0, "spruce_fence");
        add(parts, 0, 1, 0, "spruce_fence");
        add(parts, 0, 2, 0, "lime_banner[rotation=0]");
        String pot = level >= 2 ? "potted_lily_of_the_valley" : "potted_poppy";
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            add(parts, c[0], 0, c[1], level >= 3 ? "lantern[hanging=false]" : pot);
        }
    }
}
