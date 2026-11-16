package de.moinmorni.bollwerk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Weiß-Blau. Namen von Spielern kommen immer als {@code Placeholder.unparsed} hinein.
 */
public final class Texts {

    public static final int EMERALD = 0x2BB673;
    public static final int GOLD = 0xF2B632;
    public static final int BLUE = 0x5DA9E9;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<au>⚑</au> <em>Bollwerk</em> <dark_gray>»</dark_gray> ", "<au>⚑</au> <em>Bulwark</em> <dark_gray>»</dark_gray> ");
        t("mode", "Bollwerk", "Bulwark");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("not-ready", "<gray>Die Karten werden gerade gebaut – gleich geht's los!</gray>",
                "<gray>The maps are being built – just a moment!</gray>");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>", "<gray>Off to the lobby – see you soon!</gray>");

        // ---------- Wachstube ----------
        t("hall.title", "<au>⚑ Bollwerk</au>", "<au>⚑ Bulwark</au>");
        t("hall.subtitle", "<gray>Verteidigt das Dorf – Welle um Welle!</gray>", "<gray>Defend the village – wave after wave!</gray>");
        t("hall.welcome", "<gray>Willkommen in der <em>Wachstube</em>! Mit dem <white>Kompass</white> stimmst du über die Karte ab, mit dem <white>grünen Farbstoff</white> sagst du „bereit“.</gray>",
                "<gray>Welcome to the <em>guardhouse</em>! Use the <white>compass</white> to vote for a map and the <white>green dye</white> to say you're ready.</gray>");
        t("hall.how", "<gray>So geht's: Gegner kommen in Wellen und wollen das <au>Herz</au> der Karte zerstören. Zwischen den Wellen kaufst du beim <em>Krämerladen</em> Ausrüstung und Baupläne – Bauten setzt du per Rechtsklick auf die <white>Bauplätze</white>.</gray>",
                "<gray>How it works: enemies come in waves and want to destroy the map's <au>heart</au>. Between waves you buy gear and blueprints at the <em>shop</em> – place buildings by right-clicking the <white>build spots</white>.</gray>");
        t("hall.waiting", "<gray>Warte auf Mitspieler … (<white><n></white> da)</gray>", "<gray>Waiting for players … (<white><n></white> here)</gray>");
        t("hall.countdown", "<gray>Nächste Runde in <au><secs> s</au> · Karte: <em><map></em></gray>",
                "<gray>Next round in <au><secs> s</au> · map: <em><map></em></gray>");
        t("hall.no-map", "<gray>Alle Karten sind belegt – gleich wird eine frei.</gray>", "<gray>All maps are busy – one will be free soon.</gray>");
        t("hall.ready", "<em>Du bist bereit.</em> <gray>Sind alle bereit, geht's gleich los.</gray>",
                "<em>You're ready.</em> <gray>Once everyone is ready, we start.</gray>");
        t("hall.not-ready", "<gray>Du bist nicht mehr bereit.</gray>", "<gray>You're no longer ready.</gray>");
        t("hall.voted", "<gray>Deine Stimme: <em><map></em>.</gray>", "<gray>Your vote: <em><map></em>.</gray>");
        t("hall.full", "<gray>Die Runde ist voll (höchstens 6) – du bist in der nächsten dabei.</gray>",
                "<gray>The round is full (6 at most) – you're in the next one.</gray>");
        t("hall.starting", "<em>Auf geht's!</em> <gray>Karte: <au><map></au> – <white><n></white> Verteidiger.</gray>",
                "<em>Here we go!</em> <gray>Map: <au><map></au> – <white><n></white> defenders.</gray>");
        t("hall.party-joined", "<gray>Deine Party spielt gerade – du kommst dazu!</gray>", "<gray>Your party is playing – you're joining them!</gray>");

        t("item.vote", "<em>✎ Karte wählen</em>", "<em>✎ Vote for a map</em>");
        t("item.vote.lore", "<gray>Rechtsklick: Karte wählen oder einer Runde zuschauen.</gray>",
                "<gray>Right-click: vote for a map or watch a round.</gray>");
        t("item.ready", "<em>✔ Bereit</em>", "<em>✔ Ready</em>");
        t("item.ready.lore", "<gray>Rechtsklick: bereit / nicht bereit.</gray>", "<gray>Right-click: ready / not ready.</gray>");
        t("item.stats", "<au>★ Bestenliste & Statistik</au>", "<au>★ Leaderboard & stats</au>");
        t("item.stats.lore", "<gray>Rechtsklick: deine Werte und die besten Truppen.</gray>",
                "<gray>Right-click: your stats and the best teams.</gray>");
        t("item.lobby", "<bl>⌂ Zurück zur Lobby</bl>", "<bl>⌂ Back to the lobby</bl>");
        t("item.lobby.lore", "<gray>Rechtsklick: zurück ins Dorf.</gray>", "<gray>Right-click: back to the village.</gray>");
        t("item.shop", "<au>⚖ Krämerladen</au>", "<au>⚖ Shop</au>");
        t("item.shop.lore", "<gray>Rechtsklick: Ausrüstung, Tränke und Baupläne kaufen.</gray>\n<gray>Geht zwischen den Wellen.</gray>",
                "<gray>Right-click: buy gear, potions and blueprints.</gray>\n<gray>Available between waves.</gray>");
        t("item.go", "<em>▶ Welle starten</em>", "<em>▶ Start the wave</em>");
        t("item.go.lore", "<gray>Rechtsklick: ich bin fertig. Sind alle fertig,</gray>\n<gray>kommt die nächste Welle sofort.</gray>",
                "<gray>Right-click: I'm done. Once everyone is done,</gray>\n<gray>the next wave comes right away.</gray>");
        t("item.plan", "<au>✎ Bauplan: <name></au>", "<au>✎ Blueprint: <name></au>");
        t("item.plan.lore", "<gray>Rechtsklick auf einen passenden <white>Bauplatz</white>: bauen.</gray>\n<gray><where></gray>",
                "<gray>Right-click a matching <white>build spot</white>: build.</gray>\n<gray><where></gray>");
        t("spot.lane", "Steht auf dem Weg (Meißelsteine quer über den Weg).", "Goes on the path (chiselled stones across the path).");
        t("spot.side", "Steht neben dem Weg (Steinplatte 3×3).", "Goes next to the path (3×3 stone pad).");

        // ---------- Karten ----------
        t("map.talsperre", "Talsperre Morni-Dorf", "Morni Village Barrier");
        t("map.talsperre.desc", "<gray>Ummauertes Bergdorf. Gegner kommen durchs</gray>\n<gray>Nordtor, die Bresche und das Almtor.</gray>",
                "<gray>A walled mountain village. Enemies come through</gray>\n<gray>the north gate, the breach and the pasture gate.</gray>");
        t("map.klosterhof", "Klosterhof", "Cloister Court");
        t("map.klosterhof.desc", "<gray>Kloster auf der Felsnase. Gegner kommen über</gray>\n<gray>den Pilgerweg, die Hängebrücke und den Grat.</gray>",
                "<gray>A monastery on a crag. Enemies come up the</gray>\n<gray>pilgrims' path, over the rope bridge and the ridge.</gray>");
        t("heart.talsperre", "Dorfbrunnen", "Village Well");
        t("heart.klosterhof", "Klosterglocke", "Cloister Bell");

        // ---------- Menüs ----------
        t("menu.vote.title", "Karte wählen", "Vote for a map");
        t("menu.vote.votes", "<gray>Stimmen: <white><n></white></gray>", "<gray>Votes: <white><n></white></gray>");
        t("menu.vote.best", "<gray>Beste Truppe: <au><n> Wellen</au></gray>", "<gray>Best team: <au><n> waves</au></gray>");
        t("menu.vote.click", "<em>▶ Klicken zum Abstimmen</em>", "<em>▶ Click to vote</em>");
        t("menu.vote.mine", "<au>✔ Deine Stimme</au>", "<au>✔ Your vote</au>");
        t("menu.vote.busy", "<gray>Läuft gerade: Welle <white><n></white> – klicken zum Zuschauen</gray>",
                "<gray>Running: wave <white><n></white> – click to watch</gray>");
        t("menu.close", "<red>Schließen</red>", "<red>Close</red>");
        t("menu.stats.title", "Bestenliste & Statistik", "Leaderboard & stats");
        t("menu.stats.me", "<au>Deine Werte</au>", "<au>Your stats</au>");
        t("menu.stats.lines", "<gray>Spiele: <white><games></white></gray>\n<gray>Besiegte Gegner: <white><kills></white></gray>\n<gray>Überstandene Wellen: <white><waves></white></gray>\n<gray>Gerettete Mitspieler: <white><revives></white></gray>\n<gray>Beste Welle: <au><best></au></gray>",
                "<gray>Games: <white><games></white></gray>\n<gray>Enemies defeated: <white><kills></white></gray>\n<gray>Waves survived: <white><waves></white></gray>\n<gray>Teammates revived: <white><revives></white></gray>\n<gray>Best wave: <au><best></au></gray>");
        t("menu.stats.top", "<au>Beste Truppen: <map></au>", "<au>Best teams: <map></au>");
        t("menu.stats.row", "<au><pos>.</au> <white><waves> Wellen</white> <gray>– <names></gray>",
                "<au><pos>.</au> <white><waves> waves</white> <gray>– <names></gray>");
        t("menu.stats.empty", "<gray>Noch niemand – sei die erste Truppe!</gray>", "<gray>Nobody yet – be the first team!</gray>");

        // ---------- Laden ----------
        t("shop.title", "Krämerladen – <n> Gulden", "Shop – <n> guilders");
        t("shop.cat.waffen", "<au>⚔ Waffen</au>", "<au>⚔ Weapons</au>");
        t("shop.cat.ruestung", "<au>⛉ Rüstung</au>", "<au>⛉ Armour</au>");
        t("shop.cat.traenke", "<au>⚗ Tränke & Essen</au>", "<au>⚗ Potions & food</au>");
        t("shop.cat.bauten", "<au>⚒ Baupläne</au>", "<au>⚒ Blueprints</au>");
        t("shop.cat.reparatur", "<au>✚ Ausbessern</au>", "<au>✚ Repairs</au>");
        t("shop.price", "<gray>Preis: <au><n> Gulden</au></gray>", "<gray>Price: <au><n> guilders</au></gray>");
        t("shop.buy", "<em>▶ Klicken zum Kaufen</em>", "<em>▶ Click to buy</em>");
        t("shop.poor", "<red>Zu wenig Gulden</red>", "<red>Not enough guilders</red>");
        t("shop.owned", "<gray>Hast du schon (oder Besseres).</gray>", "<gray>You already have this (or better).</gray>");
        t("shop.closed", "<gray>Der Laden hat nur zwischen den Wellen offen.</gray>", "<gray>The shop is only open between waves.</gray>");
        t("shop.bought", "<gray>Gekauft: <white><name></white> (<au>-<n> Gulden</au>).</gray>",
                "<gray>Bought: <white><name></white> (<au>-<n> guilders</au>).</gray>");
        t("shop.not-enough", "<red>Dafür fehlen dir noch <n> Gulden.</red>", "<red>You need <n> more guilders.</red>");
        t("shop.nothing-to-repair", "<gray>Da gibt es gerade nichts auszubessern.</gray>", "<gray>Nothing to repair right now.</gray>");
        t("shop.wallet", "<au>⛃ <n> Gulden</au>", "<au>⛃ <n> guilders</au>");
        t("shop.wallet.lore", "<gray>Gulden gibt es für jeden besiegten Gegner</gray>\n<gray>und für jede geschaffte Welle.</gray>",
                "<gray>You earn guilders for every enemy defeated</gray>\n<gray>and for every wave survived.</gray>");

        t("offer.eisenschwert", "Eisenschwert", "Iron sword");
        t("offer.diamantschwert", "Diamantschwert", "Diamond sword");
        t("offer.bogen_kraft", "Kraftbogen (Stärke II)", "Power bow (Power II)");
        t("offer.bogen_meister", "Meisterbogen (Stärke IV, Flamme)", "Master bow (Power IV, Flame)");
        t("offer.pfeile", "32 Pfeile", "32 arrows");
        t("offer.kettenruestung", "Kettenrüstung", "Chain armour");
        t("offer.eisenruestung", "Eisenrüstung", "Iron armour");
        t("offer.diamantruestung", "Diamantrüstung", "Diamond armour");
        t("offer.heiltrank", "Heiltrank", "Healing potion");
        t("offer.staerketrank", "Stärketrank (1:30)", "Strength potion (1:30)");
        t("offer.tempotrank", "Tempotrank (2:00)", "Swiftness potion (2:00)");
        t("offer.goldapfel", "Goldapfel", "Golden apple");
        t("offer.bauplan_palisade", "Bauplan: Palisade", "Blueprint: palisade");
        t("offer.bauplan_kesselfalle", "Bauplan: Kesselfalle", "Blueprint: cauldron trap");
        t("offer.bauplan_pfeilturm", "Bauplan: Pfeilturm", "Blueprint: arrow tower");
        t("offer.bauplan_heilbanner", "Bauplan: Heilbanner", "Blueprint: healing banner");
        t("offer.herz_flicken", "Herz flicken (+50)", "Patch the heart (+50)");
        t("offer.mauern_flicken", "Alle Bauten ausbessern", "Repair all buildings");
        t("offer.herz_flicken.desc", "<gray>Gibt dem Herzen 50 Lebenspunkte zurück.</gray>", "<gray>Gives the heart 50 health back.</gray>");
        t("offer.mauern_flicken.desc", "<gray>Alle beschädigten Bauten wieder ganz.</gray>\n<gray>1 Gulden je 4 Lebenspunkte.</gray>",
                "<gray>Every damaged building back to full.</gray>\n<gray>1 guilder per 4 health.</gray>");

        // ---------- Bauten ----------
        t("defence.palisade", "Palisade", "Palisade");
        t("defence.kesselfalle", "Kesselfalle", "Cauldron trap");
        t("defence.pfeilturm", "Pfeilturm", "Arrow tower");
        t("defence.heilbanner", "Heilbanner", "Healing banner");
        t("defence.palisade.desc", "<gray>Sperrt den Weg, bis sie zerschlagen ist.</gray>\n<gray>Brecher machen ihr viermal so viel Schaden.</gray>",
                "<gray>Blocks the path until it's smashed.</gray>\n<gray>Breakers deal four times the damage.</gray>");
        t("defence.kesselfalle.desc", "<gray>Brodelt am Weg: bremst und verbrüht</gray>\n<gray>Gegner in der Nähe.</gray>",
                "<gray>Bubbles by the path: slows and scalds</gray>\n<gray>enemies nearby.</gray>");
        t("defence.pfeilturm.desc", "<gray>Schießt auf den Gegner, der dem Herzen</gray>\n<gray>am nächsten ist.</gray>",
                "<gray>Shoots the enemy closest</gray>\n<gray>to the heart.</gray>");
        t("defence.heilbanner.desc", "<gray>Heilt Verteidiger in der Nähe;</gray>\n<gray>auf Stufe 3 auch das Herz.</gray>",
                "<gray>Heals defenders nearby;</gray>\n<gray>at level 3 it heals the heart too.</gray>");
        t("defence.built", "<gray><player> hat gebaut: <au><name></au>.</gray>", "<gray><player> built: <au><name></au>.</gray>");
        t("defence.wrong-spot", "<gray>Das passt hier nicht. <where></gray>", "<gray>That doesn't fit here. <where></gray>");
        t("defence.taken", "<gray>Hier steht schon etwas – Rechtsklick drauf zum Verbessern.</gray>",
                "<gray>Something already stands here – right-click it to upgrade.</gray>");
        t("defence.no-spot", "<gray>Kein Bauplatz in der Nähe. Bauplätze erkennst du an der schwebenden Schrift.</gray>",
                "<gray>No build spot nearby. Build spots have floating text above them.</gray>");
        t("defence.destroyed", "<red>Zerstört: <name>!</red>", "<red>Destroyed: <name>!</red>");
        t("defence.upgraded", "<gray><player> hat verbessert: <au><name></au> (Stufe <white><n></white>).</gray>",
                "<gray><player> upgraded: <au><name></au> (level <white><n></white>).</gray>");
        t("defence.repaired", "<gray>Ausgebessert (<au>-<n> Gulden</au>).</gray>", "<gray>Repaired (<au>-<n> guilders</au>).</gray>");
        t("menu.defence.title", "<name> – Stufe <n>", "<name> – level <n>");
        t("menu.defence.info", "<gray>Leben: <white><hp></white></gray>\n<gray>Reichweite: <white><range></white> · Stärke: <white><power></white></gray>",
                "<gray>Health: <white><hp></white></gray>\n<gray>Range: <white><range></white> · Power: <white><power></white></gray>");
        t("menu.defence.upgrade", "<em>▲ Verbessern auf Stufe <n></em>", "<em>▲ Upgrade to level <n></em>");
        t("menu.defence.max", "<au>★ Höchste Stufe</au>", "<au>★ Maximum level</au>");
        t("menu.defence.repair", "<em>✚ Ausbessern</em>", "<em>✚ Repair</em>");
        t("spot.marker.lane", "<gray>⚒ Bauplatz</gray>\n<white>Palisade · Kesselfalle</white>", "<gray>⚒ Build spot</gray>\n<white>Palisade · Cauldron trap</white>");
        t("spot.marker.side", "<gray>⚒ Bauplatz</gray>\n<white>Pfeilturm · Heilbanner</white>", "<gray>⚒ Build spot</gray>\n<white>Arrow tower · Healing banner</white>");

        // ---------- Gegner ----------
        t("enemy.pluenderer", "Plünderer", "Plunderer");
        t("enemy.flinker", "Flinker", "Scamper");
        t("enemy.schildtraeger", "Schildträger", "Shieldbearer");
        t("enemy.armbrustschuetze", "Armbrustschütze", "Crossbowman");
        t("enemy.brecher", "Brecher", "Breaker");
        t("enemy.lawinenriese", "Der Lawinenriese", "The Avalanche Giant");
        t("enemy.nebelhexe", "Die Nebelhexe", "The Mist Witch");
        t("enemy.new", "<gray>Neu dabei: <au><name></au> – <desc></gray>", "<gray>New enemy: <au><name></au> – <desc></gray>");
        t("enemy.flinker.desc", "schnell, aber schwach.", "fast but weak.");
        t("enemy.schildtraeger.desc", "viel Leben, Turmpfeile prallen zur Hälfte ab.", "lots of health, tower arrows only do half.");
        t("enemy.armbrustschuetze.desc", "schießt aus der Ferne aufs Herz.", "shoots the heart from afar.");
        t("enemy.brecher.desc", "geht auf Bauten los – schützt eure Palisaden!", "goes for buildings – protect your palisades!");
        t("boss.coming", "<red>⚠ <name> kommt!</red>", "<red>⚠ <name> is coming!</red>");
        t("boss.down", "<au>★ <name> ist besiegt!</au>", "<au>★ <name> is defeated!</au>");
        t("boss.summon", "<gray><name> ruft Flinke herbei!</gray>", "<gray><name> summons scampers!</gray>");

        // ---------- Spielablauf ----------
        t("game.welcome", "<gray>Haltet das Herz: <au><heart></au>! <white>Krämerladen</white> (Smaragd) und <white>Bauplätze</white> nutzen – die erste Welle kommt in <au><secs> s</au>.</gray>",
                "<gray>Hold the heart: <au><heart></au>! Use the <white>shop</white> (emerald) and the <white>build spots</white> – the first wave arrives in <au><secs> s</au>.</gray>");
        t("game.prepare.bar", "Aufbau: noch <secs> s – Welle <n> kommt", "Build phase: <secs> s left – wave <n> is coming");
        t("game.wave.bar", "Welle <n> – noch <left> Gegner", "Wave <n> – <left> enemies left");
        t("game.heart.bar", "<heart>: <hp> / <max>", "<heart>: <hp> / <max>");
        t("game.wave.title", "<red>Welle <n></red>", "<red>Wave <n></red>");
        t("game.wave.sub", "<gray><count> Gegner aus <lanes> Richtungen</gray>", "<gray><count> enemies from <lanes> directions</gray>");
        t("game.boss.sub", "<red>Mit Boss: <name></red>", "<red>With a boss: <name></red>");
        t("game.cleared.title", "<em>Welle <n> geschafft!</em>", "<em>Wave <n> survived!</em>");
        t("game.cleared.sub", "<au>+<amount> Gulden</au> <gray>· Aufbau: <secs> s</gray>", "<au>+<amount> guilders</au> <gray>· build phase: <secs> s</gray>");
        t("game.ready", "<gray><player> ist fertig (<n>/<total>).</gray>", "<gray><player> is done (<n>/<total>).</gray>");
        t("game.heart.hit", "<red>Das Herz wird angegriffen!</red>", "<red>The heart is under attack!</red>");
        t("game.heart.low", "<red>⚠ Das Herz hat nur noch <hp> Leben!</red>", "<red>⚠ The heart is down to <hp> health!</red>");
        t("game.heart.patched", "<gray><player> hat das Herz geflickt (+<n>).</gray>", "<gray><player> patched the heart (+<n>).</gray>");
        t("game.kill", "<au>+<n> Gulden</au> <gray>für <name></gray>", "<au>+<n> guilders</au> <gray>for <name></gray>");
        t("game.over.title", "<red>Das Bollwerk ist gefallen</red>", "<red>The bulwark has fallen</red>");
        t("game.over.sub", "<gray>Überstanden: <au><n> Wellen</au></gray>", "<gray>Survived: <au><n> waves</au></gray>");
        t("game.over.chat", "<gray>Das Herz ist gefallen (<heart>). Ihr habt <au><n> Wellen</au> überstanden – Platz <white><pos></white> auf der Bestenliste!</gray>",
                "<gray>The heart has fallen (<heart>). You survived <au><n> waves</au> – rank <white><pos></white> on the leaderboard!</gray>");
        t("game.over.chat.plain", "<gray>Das Herz ist gefallen (<heart>). Ihr habt <au><n> Wellen</au> überstanden.</gray>",
                "<gray>The heart has fallen (<heart>). You survived <au><n> waves</au>.</gray>");
        t("game.over.best", "<au>★ Neue Bestleistung für dich: <n> Wellen!</au>", "<au>★ New personal best: <n> waves!</au>");
        t("game.back", "<gray>Zurück in die Wachstube in <secs> s …</gray>", "<gray>Back to the guardhouse in <secs> s …</gray>");
        t("game.abandoned", "<gray>Alle sind gegangen – die Runde ist vorbei.</gray>", "<gray>Everyone left – the round is over.</gray>");
        t("game.left", "<gray><player> hat die Runde verlassen.</gray>", "<gray><player> left the round.</gray>");
        t("game.late", "<gray><player> kommt zur Verstärkung!</gray>", "<gray><player> joins as reinforcement!</gray>");
        t("game.respawn", "<em>Du bist wieder dabei!</em>", "<em>You're back in the fight!</em>");

        // ---------- Am Boden / Retten ----------
        t("down.title", "<red>Du bist am Boden!</red>", "<red>You're down!</red>");
        t("down.sub", "<gray>Jemand muss sich zu dir ducken (Schleichen) …</gray>", "<gray>Someone needs to crouch next to you (sneak) …</gray>");
        t("down.chat", "<red><player> ist am Boden!</red> <gray>Hinlaufen und <white>schleichen</white>, um zu retten.</gray>",
                "<red><player> is down!</red> <gray>Go there and <white>sneak</white> to revive.</gray>");
        t("down.bar", "<red>Am Boden – noch <secs> s</red>", "<red>Down – <secs> s left</red>");
        t("down.reviving", "<em>Rette <player> … <bar></em>", "<em>Reviving <player> … <bar></em>");
        t("down.revived", "<em><player> ist wieder auf den Beinen</em> <gray>(gerettet von <helper>).</gray>",
                "<em><player> is back on their feet</em> <gray>(revived by <helper>).</gray>");
        t("down.out", "<gray><player> ist ausgeschieden – kommt in der nächsten Aufbauphase zurück.</gray>",
                "<gray><player> is out – returns in the next build phase.</gray>");
        t("down.out.self", "<gray>Du bist ausgeschieden und schaust zu – in der nächsten Aufbauphase bist du wieder dabei.</gray>",
                "<gray>You're out and spectating – you'll be back in the next build phase.</gray>");

        // ---------- Leiste ----------
        t("side.title", "<au><b>⚑ BOLLWERK</b></au>", "<au><b>⚑ BULWARK</b></au>");
        t("side.map", "<gray>Karte</gray> <white><map></white>", "<gray>Map</gray> <white><map></white>");
        t("side.wave", "<gray>Welle</gray> <white><n></white>", "<gray>Wave</gray> <white><n></white>");
        t("side.enemies", "<gray>Gegner</gray> <red><n></red>", "<gray>Enemies</gray> <red><n></red>");
        t("side.prepare", "<gray>Aufbau</gray> <em><secs> s</em>", "<gray>Build</gray> <em><secs> s</em>");
        t("side.heart", "<gray>Herz</gray> <hp>", "<gray>Heart</gray> <hp>");
        t("side.gold", "<gray>Gulden</gray> <au><n></au>", "<gray>Guilders</gray> <au><n></au>");
        t("side.kills", "<gray>Besiegt</gray> <white><n></white>", "<gray>Defeated</gray> <white><n></white>");
        t("side.team", "<gray>Verteidiger</gray> <white><n></white>", "<gray>Defenders</gray> <white><n></white>");
        t("side.hall.players", "<gray>In der Wachstube</gray> <white><n></white>", "<gray>In the guardhouse</gray> <white><n></white>");
        t("side.hall.start", "<gray>Start in</gray> <au><secs> s</au>", "<gray>Starts in</gray> <au><secs> s</au>");
        t("side.hall.wait", "<gray>Warte auf Mitspieler</gray>", "<gray>Waiting for players</gray>");
        t("side.hall.best", "<gray>Deine beste Welle</gray> <au><n></au>", "<gray>Your best wave</gray> <au><n></au>");
        t("side.site", "<bl>moinmorni.net</bl>", "<bl>moinmorni.net</bl>");

        // ---------- Zuschauen / Verlassen ----------
        t("spectate.start", "<gray>Du schaust <em><map></em> zu. <white>/verlassen</white> bringt dich zurück.</gray>",
                "<gray>You're watching <em><map></em>. <white>/leave</white> takes you back.</gray>");
        t("spectate.none", "<gray>Gerade läuft keine Runde.</gray>", "<gray>No round is running right now.</gray>");
        t("leave.none", "<gray>Du bist schon in der Wachstube.</gray>", "<gray>You're already in the guardhouse.</gray>");
        t("leave.done", "<gray>Zurück in der Wachstube.</gray>", "<gray>Back in the guardhouse.</gray>");

        // ---------- Statistik ----------
        t("stats.head", "<au>Bollwerk – <name></au>", "<au>Bulwark – <name></au>");
        t("stats.none", "<gray>Von <white><name></white> gibt es noch keine Werte.</gray>", "<gray>No stats for <white><name></white> yet.</gray>");
        t("top.head", "<au>Beste Truppen – <map></au>", "<au>Best teams – <map></au>");
        t("board.title", "<au><b>Beste Truppen</b></au>", "<au><b>Best teams</b></au>");
        t("board.table", "<em><b><map></b></em>\n<gray><desc></gray>\n<gray>Rekord: <au><n> Wellen</au></gray>",
                "<em><b><map></b></em>\n<gray><desc></gray>\n<gray>Record: <au><n> waves</au></gray>");
        t("board.table.desc.talsperre", "Bergdorf mit Mauer und Tor", "Walled mountain village");
        t("board.table.desc.klosterhof", "Kloster auf der Felsnase", "Monastery on a crag");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „⚑ Bollwerk »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text (ohne Formatierung), z. B. für Namen über Gegnern. */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }
}
