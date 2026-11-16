package net.morni.bollwerk;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "em", "au", "bl", "red", "green", "dark_gray",
            "b", "bold", "gold", "yellow");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wie 🏆 wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "game.over.title"), Texts.raw("en", "game.over.title"));
        assertEquals(Texts.raw("de", "map.klosterhof"), Texts.raw("fr", "map.klosterhof"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
    }

    @Test
    void ownColourTagsWinOverStandardTags() {
        var c = Texts.MM.deserialize("<em>x</em>");
        assertEquals(TextColor.color(Texts.EMERALD), c.children().isEmpty() ? c.color() : c.children().get(0).color());
        var flat = c.children().isEmpty() ? c : c.children().get(0);
        assertNotEquals(net.kyori.adventure.text.format.TextDecoration.State.TRUE,
                flat.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC), "<em> darf nicht kursiv sein");
    }

    /** Jede Gegnerart, jeder Bau, jedes Angebot und jede Karte hat einen Namen in beiden Sprachen. */
    @Test
    void everyGameThingHasAName() {
        for (var t : net.morni.bollwerk.logic.EnemyType.values()) {
            assertTrue(Texts.keys().contains("enemy." + t.id()), t.id());
        }
        for (var d : net.morni.bollwerk.logic.DefenceType.values()) {
            assertTrue(Texts.keys().contains("defence." + d.id()), d.id());
            assertTrue(Texts.keys().contains("defence." + d.id() + ".desc"), d.id());
        }
        for (var o : net.morni.bollwerk.logic.Offer.values()) {
            assertTrue(Texts.keys().contains("offer." + o.id()), o.id());
        }
        for (var m : net.morni.bollwerk.world.MapType.values()) {
            assertTrue(Texts.keys().contains("map." + m.id()), m.id());
            assertTrue(Texts.keys().contains("heart." + m.id()), m.id());
            assertTrue(Texts.keys().contains("board.table.desc." + m.id()), m.id());
        }
    }

    @Test
    void numbersUseTheRightDecimalMark() {
        assertEquals("▮▮▮▯▯", Ui.bar(0.6, 5));
        assertEquals("▯▯▯", Ui.bar(-1, 3));
        assertEquals("3,5", Ui.hearts(7.0, "de"));
        assertEquals("3.5", Ui.hearts(7.0, "en"));
        assertEquals("1:05", Ui.clock(65));
        assertEquals("0:00", Ui.clock(-3));
    }
}
