package de.moinmorni.bollwerk.game;

import de.moinmorni.bollwerk.BollwerkPlugin;
import de.moinmorni.bollwerk.Items;
import de.moinmorni.bollwerk.Texts;
import de.moinmorni.bollwerk.Ui;
import de.moinmorni.bollwerk.logic.DefenceType;
import de.moinmorni.bollwerk.logic.Offer;
import de.moinmorni.bollwerk.menu.Menu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Der Krämerladen: Truhen-Menü, ein Klick = kaufen. Offen nur in der Aufbauphase. */
public final class Shop extends Menu {

    private static final int ROWS = 6;
    private static final Map<Offer, Integer> SLOTS = Map.ofEntries(
            Map.entry(Offer.EISENSCHWERT, 10), Map.entry(Offer.DIAMANTSCHWERT, 11), Map.entry(Offer.BOGEN_KRAFT, 12),
            Map.entry(Offer.BOGEN_MEISTER, 13), Map.entry(Offer.PFEILE, 14),
            Map.entry(Offer.KETTENRUESTUNG, 19), Map.entry(Offer.EISENRUESTUNG, 20), Map.entry(Offer.DIAMANTRUESTUNG, 21),
            Map.entry(Offer.HEILTRANK, 23), Map.entry(Offer.STAERKETRANK, 24), Map.entry(Offer.TEMPOTRANK, 25),
            Map.entry(Offer.GOLDAPFEL, 26),
            Map.entry(Offer.BAUPLAN_PALISADE, 28), Map.entry(Offer.BAUPLAN_KESSELFALLE, 29),
            Map.entry(Offer.BAUPLAN_PFEILTURM, 30), Map.entry(Offer.BAUPLAN_HEILBANNER, 31),
            Map.entry(Offer.HERZ_FLICKEN, 37), Map.entry(Offer.MAUERN_FLICKEN, 38));

    private final BollwerkPlugin plugin;
    private final Game game;
    private final Fighter fighter;
    private final String lang;

    private Shop(BollwerkPlugin plugin, Game game, Fighter fighter, String lang) {
        super(ROWS, Texts.text(lang, "shop.title", Placeholder.unparsed("n", String.valueOf(fighter.wallet().balance()))));
        this.plugin = plugin;
        this.game = game;
        this.fighter = fighter;
        this.lang = lang;
        draw();
    }

    /** Laden öffnen (nur in der Aufbauphase). */
    public static void open(BollwerkPlugin plugin, Game game, Player p) {
        Fighter f = game.fighter(p.getUniqueId());
        String lang = plugin.lang(p);
        if (f == null || f.state() != Fighter.State.ALIVE) {
            return;
        }
        if (game.phase() != Game.Phase.PREPARE) {
            p.sendMessage(Texts.msg(lang, "shop.closed"));
            return;
        }
        new Shop(plugin, game, f, lang).open(p);
        Ui.sound(p, "minecraft:block.barrel.open", 1.2f);
    }

    private void draw() {
        clear();
        ItemStack frame = Items.pane(Material.GREEN_STAINED_GLASS_PANE);
        for (int i = 0; i < ROWS * 9; i++) {
            set(i, frame, null);
        }
        set(4, Items.named(Material.GOLD_NUGGET, 1, Texts.text(lang, "shop.wallet",
                Placeholder.unparsed("n", String.valueOf(fighter.wallet().balance()))), Texts.lines(lang, "shop.wallet.lore")), null);
        set(9, Items.named(Material.IRON_SWORD, 1, Texts.text(lang, "shop.cat.waffen"), null), null);
        set(18, Items.named(Material.IRON_CHESTPLATE, 1, Texts.text(lang, "shop.cat.ruestung"), null), null);
        set(22, Items.named(Material.BREWING_STAND, 1, Texts.text(lang, "shop.cat.traenke"), null), null);
        set(27, Items.named(Material.CRAFTING_TABLE, 1, Texts.text(lang, "shop.cat.bauten"), null), null);
        set(36, Items.named(Material.ANVIL, 1, Texts.text(lang, "shop.cat.reparatur"), null), null);
        SLOTS.forEach((offer, slot) -> set(slot, icon(offer), p -> buy(p, offer)));
        set(49, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
    }

    private int price(Offer o) {
        return o == Offer.MAUERN_FLICKEN ? game.repairAllPrice() : o.price();
    }

    private ItemStack icon(Offer o) {
        Material m = switch (o) {
            case EISENSCHWERT -> Material.IRON_SWORD;
            case DIAMANTSCHWERT -> Material.DIAMOND_SWORD;
            case BOGEN_KRAFT, BOGEN_MEISTER -> Material.BOW;
            case PFEILE -> Material.ARROW;
            case KETTENRUESTUNG -> Material.CHAINMAIL_CHESTPLATE;
            case EISENRUESTUNG -> Material.IRON_CHESTPLATE;
            case DIAMANTRUESTUNG -> Material.DIAMOND_CHESTPLATE;
            case HEILTRANK -> Material.GLISTERING_MELON_SLICE;
            case STAERKETRANK -> Material.BLAZE_POWDER;
            case TEMPOTRANK -> Material.SUGAR;
            case GOLDAPFEL -> Material.GOLDEN_APPLE;
            case BAUPLAN_PALISADE -> Material.OAK_LOG;
            case BAUPLAN_KESSELFALLE -> Material.CAULDRON;
            case BAUPLAN_PFEILTURM -> Material.TARGET;
            case BAUPLAN_HEILBANNER -> Material.LIME_BANNER;
            case HERZ_FLICKEN -> Material.HEART_OF_THE_SEA;
            case MAUERN_FLICKEN -> Material.BRICKS;
        };
        List<Component> lore = new ArrayList<>();
        DefenceType d = o.defence();
        if (d != null) {
            lore.addAll(Texts.lines(lang, "defence." + d.id() + ".desc"));
            lore.add(Texts.text(lang, d.spot() == DefenceType.Spot.LANE ? "spot.lane" : "spot.side"));
        } else if (o == Offer.HERZ_FLICKEN || o == Offer.MAUERN_FLICKEN) {
            lore.addAll(Texts.lines(lang, "offer." + o.id() + ".desc"));
        }
        int price = price(o);
        lore.add(Texts.text(lang, "shop.price", Placeholder.unparsed("n", String.valueOf(price))));
        boolean allowed = o.allowed(fighter.gear());
        if (!allowed) {
            lore.add(Texts.text(lang, "shop.owned"));
        } else if (!fighter.wallet().canPay(price)) {
            lore.add(Texts.text(lang, "shop.poor"));
        } else {
            lore.add(Texts.text(lang, "shop.buy"));
        }
        ItemStack item = Items.named(m, 1, Texts.text(lang, "offer." + o.id()), lore);
        return Items.glow(item, allowed && fighter.wallet().canPay(price) && price > 0);
    }

    private void buy(Player p, Offer o) {
        if (game.phase() != Game.Phase.PREPARE) {
            p.closeInventory();
            p.sendMessage(Texts.msg(lang, "shop.closed"));
            return;
        }
        if (!o.allowed(fighter.gear())) {
            Ui.sound(p, "minecraft:entity.villager.no", 1f);
            return;
        }
        String error;
        int price = price(o);
        if (o == Offer.HERZ_FLICKEN) {
            error = game.patchHeart(p, fighter);
        } else if (o == Offer.MAUERN_FLICKEN) {
            error = game.repairAll(fighter);
        } else if (!fighter.wallet().pay(price)) {
            error = "shop.poor";
        } else {
            Kit.deliver(p, fighter, o, lang);
            error = null;
        }
        if (error != null) {
            Ui.sound(p, "minecraft:entity.villager.no", 1f);
            if ("shop.poor".equals(error)) {
                p.sendMessage(Texts.msg(lang, "shop.not-enough",
                        Placeholder.unparsed("n", String.valueOf(price - fighter.wallet().balance()))));
            } else {
                p.sendMessage(Texts.msg(lang, error));
            }
            return;
        }
        Ui.sound(p, "minecraft:entity.villager.yes", 1.1f);
        p.sendMessage(Texts.msg(lang, "shop.bought", Placeholder.unparsed("name", Texts.plain(lang, "offer." + o.id())),
                Placeholder.unparsed("n", String.valueOf(price))));
        // neu öffnen: Titel zeigt den neuen Kontostand
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (game.phase() == Game.Phase.PREPARE && p.isOnline()) {
                new Shop(plugin, game, fighter, lang).open(p);
            }
        });
    }
}
