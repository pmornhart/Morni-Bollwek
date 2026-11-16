package net.morni.bollwerk.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.bollwerk.Items;
import net.morni.bollwerk.Texts;
import net.morni.bollwerk.Ui;
import net.morni.bollwerk.logic.DefenceType;
import net.morni.bollwerk.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Menü eines Baus: Werte, Verbessern, Ausbessern. */
final class DefenceMenu extends Menu {

    private final Game game;
    private final Fighter fighter;
    private final Defence defence;
    private final String lang;

    DefenceMenu(Game game, Fighter fighter, Defence defence, String lang) {
        super(3, Texts.text(lang, "menu.defence.title",
                Placeholder.unparsed("name", Texts.plain(lang, "defence." + defence.type.id())),
                Placeholder.unparsed("n", String.valueOf(defence.level))));
        this.game = game;
        this.fighter = fighter;
        this.defence = defence;
        this.lang = lang;
        draw();
    }

    private void draw() {
        clear();
        DefenceType t = defence.type;
        List<Component> info = new ArrayList<>(Texts.lines(lang, "defence." + t.id() + ".desc"));
        info.addAll(Texts.lines(lang, "menu.defence.info",
                Placeholder.unparsed("hp", (int) Math.ceil(defence.health) + " / " + (int) defence.maxHealth()),
                Placeholder.unparsed("range", String.format(Locale.ROOT, "%.0f", t.range(defence.level))),
                Placeholder.unparsed("power", String.format(Locale.ROOT, "%.0f", t.power(defence.level)))));
        set(11, Items.named(Material.BOOK, 1, Texts.text(lang, "defence." + t.id()), info), null);
        int price = t.upgradePrice(defence.level);
        if (price < 0) {
            set(13, Items.named(Material.NETHER_STAR, 1, Texts.text(lang, "menu.defence.max"), null), null);
        } else {
            set(13, Items.glow(Items.named(Material.EXPERIENCE_BOTTLE, 1, Texts.text(lang, "menu.defence.upgrade",
                    Placeholder.unparsed("n", String.valueOf(defence.level + 1))),
                    List.of(Texts.text(lang, "shop.price", Placeholder.unparsed("n", String.valueOf(price))))),
                    fighter.wallet().canPay(price)), p -> act(p, game.upgrade(p, fighter, defence)));
        }
        int repair = game.repairPrice(defence);
        set(15, Items.named(Material.BRICKS, 1, Texts.text(lang, "menu.defence.repair"),
                List.of(Texts.text(lang, "shop.price", Placeholder.unparsed("n", String.valueOf(repair))))),
                p -> act(p, game.repair(fighter, defence)));
        set(22, Items.named(Material.BARRIER, 1, Texts.text(lang, "menu.close"), null), Player::closeInventory);
    }

    private void act(Player p, String error) {
        if (error != null) {
            Ui.sound(p, "minecraft:entity.villager.no", 1f);
            p.sendMessage(Texts.msg(lang, error));
            return;
        }
        Ui.sound(p, "minecraft:block.anvil.use", 1.3f);
        p.closeInventory();
    }

    /** Öffnen (vom Rechtsklick auf den Bau). */
    static void open(Game game, Fighter f, Defence d, Player p, String lang) {
        new DefenceMenu(game, f, d, lang).open(p);
    }
}
