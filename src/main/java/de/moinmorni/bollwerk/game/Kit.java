package de.moinmorni.bollwerk.game;

import de.moinmorni.bollwerk.Items;
import de.moinmorni.bollwerk.Texts;
import de.moinmorni.bollwerk.logic.DefenceType;
import de.moinmorni.bollwerk.logic.Offer;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/** Ausrüstung im Spiel: Startausrüstung, Gekauftes, Knöpfe der Schnellleiste. */
public final class Kit {

    public static final int SLOT_SHOP = 8;
    public static final int SLOT_GO = 7;
    private static final int TICKS_PER_SECOND = 20;

    private Kit() {
    }

    /** Alles neu nach der Ausrüstungs-Stufe (Start, Wiedereinstieg). Verbrauchbares bekommt man zum Start einmal. */
    static void give(Player p, Fighter f, String lang, boolean starter) {
        PlayerInventory inv = p.getInventory();
        if (starter) {
            inv.clear();
            inv.setItem(0, sword(f.gear().tier("schwert")));
            inv.setItem(1, bow(f.gear().tier("bogen")));
            inv.setItem(2, new ItemStack(Material.BREAD, 6));
            inv.setItem(9, new ItemStack(Material.ARROW, 24));
        }
        armour(p, f.gear().tier("ruestung"));
        buttons(p, lang, true);
    }

    /** Knöpfe: Krämerladen und (in der Aufbauphase) „Welle starten“. */
    static void buttons(Player p, String lang, boolean prepare) {
        PlayerInventory inv = p.getInventory();
        inv.setItem(SLOT_SHOP, Items.kitTag(Items.named(Material.EMERALD, 1, Texts.text(lang, "item.shop"),
                Texts.lines(lang, "item.shop.lore")), "shop"));
        inv.setItem(SLOT_GO, prepare ? Items.kitTag(Items.named(Material.LIME_DYE, 1, Texts.text(lang, "item.go"),
                Texts.lines(lang, "item.go.lore")), "go") : null);
    }

    static ItemStack sword(int tier) {
        Material m = switch (tier) {
            case 3 -> Material.DIAMOND_SWORD;
            case 2 -> Material.IRON_SWORD;
            default -> Material.STONE_SWORD;
        };
        ItemStack item = Items.kitTag(new ItemStack(m), "sword");
        item.editMeta(meta -> meta.setUnbreakable(true));
        return item;
    }

    static ItemStack bow(int tier) {
        ItemStack item = Items.kitTag(new ItemStack(Material.BOW), "bow");
        item.editMeta(meta -> {
            meta.setUnbreakable(true);
            if (tier >= 2) {
                meta.addEnchant(Enchantment.POWER, tier >= 3 ? 4 : 2, true);
            }
            if (tier >= 3) {
                meta.addEnchant(Enchantment.FLAME, 1, true);
            }
        });
        return item;
    }

    static void armour(Player p, int tier) {
        PlayerInventory inv = p.getInventory();
        String[] parts = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};
        String base = switch (tier) {
            case 4 -> "DIAMOND_";
            case 3 -> "IRON_";
            case 2 -> "CHAINMAIL_";
            default -> "LEATHER_";
        };
        ItemStack[] armour = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            ItemStack item = new ItemStack(Material.valueOf(base + parts[i]));
            item.editMeta(meta -> {
                meta.setUnbreakable(true);
                if (meta instanceof LeatherArmorMeta lm) {
                    lm.setColor(Color.fromRGB(0x2B, 0x6E, 0x4A));
                }
            });
            armour[i] = item;
        }
        inv.setHelmet(armour[0]);
        inv.setChestplate(armour[1]);
        inv.setLeggings(armour[2]);
        inv.setBoots(armour[3]);
    }

    /** Waffe ersetzen (Schwert oder Bogen), wo sie gerade liegt. */
    static void replace(Player p, String tag, ItemStack fresh) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (tag.equals(Items.kitTag(inv.getItem(i)))) {
                inv.setItem(i, fresh);
                return;
            }
        }
        inv.addItem(fresh);
    }

    /** Gekauftes übergeben. */
    static void deliver(Player p, Fighter f, Offer o, String lang) {
        switch (o) {
            case EISENSCHWERT, DIAMANTSCHWERT -> replace(p, "sword", sword(o.tier()));
            case BOGEN_KRAFT, BOGEN_MEISTER -> replace(p, "bow", bow(o.tier()));
            case KETTENRUESTUNG, EISENRUESTUNG, DIAMANTRUESTUNG -> armour(p, o.tier());
            case PFEILE -> p.getInventory().addItem(new ItemStack(Material.ARROW, 32));
            case HEILTRANK -> p.getInventory().addItem(potion(PotionType.STRONG_HEALING, null));
            case STAERKETRANK -> p.getInventory().addItem(potion(null, new PotionEffect(PotionEffectType.STRENGTH,
                    90 * TICKS_PER_SECOND, 0)));
            case TEMPOTRANK -> p.getInventory().addItem(potion(null, new PotionEffect(PotionEffectType.SPEED,
                    120 * TICKS_PER_SECOND, 0)));
            case GOLDAPFEL -> p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));
            default -> {
                DefenceType d = o.defence();
                if (d != null) {
                    p.getInventory().addItem(plan(d, lang));
                }
            }
        }
        f.gear().apply(o);
    }

    private static ItemStack potion(PotionType type, PotionEffect effect) {
        ItemStack item = new ItemStack(Material.POTION);
        item.editMeta(PotionMeta.class, meta -> {
            if (type != null) {
                meta.setBasePotionType(type);
            }
            if (effect != null) {
                meta.addCustomEffect(effect, true);
                meta.setColor(effect.getType() == PotionEffectType.STRENGTH ? Color.fromRGB(0xC0392B)
                        : Color.fromRGB(0x5DA9E9));
            }
        });
        return item;
    }

    /** Bauplan-Gegenstand (Markierung „plan:&lt;bau&gt;“). */
    public static ItemStack plan(DefenceType d, String lang) {
        Material m = switch (d) {
            case PALISADE -> Material.OAK_LOG;
            case KESSELFALLE -> Material.CAULDRON;
            case PFEILTURM -> Material.ARROW;
            case HEILBANNER -> Material.LIME_BANNER;
        };
        String name = Texts.plain(lang, "defence." + d.id());
        String where = Texts.plain(lang, d.spot() == DefenceType.Spot.LANE ? "spot.lane" : "spot.side");
        ItemStack item = Items.named(m == Material.ARROW ? Material.PAPER : m, 1,
                Texts.text(lang, "item.plan", Placeholder.unparsed("name", name)),
                List.copyOf(concat(Texts.lines(lang, "item.plan.lore", Placeholder.unparsed("where", where)),
                        Texts.lines(lang, "defence." + d.id() + ".desc"))));
        Items.glow(item, true);
        return Items.kitTag(item, "plan:" + d.id());
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        java.util.ArrayList<T> out = new java.util.ArrayList<>(a);
        out.addAll(b);
        return out;
    }
}
