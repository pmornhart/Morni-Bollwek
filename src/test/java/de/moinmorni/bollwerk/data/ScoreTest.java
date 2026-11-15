package de.moinmorni.bollwerk.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScoreTest {

    @Test
    void moreWavesRankHigherEarlierWinsTies() {
        Leaderboard b = new Leaderboard();
        assertEquals(1, b.add("talsperre", 5, List.of("Anna"), 1));
        assertEquals(1, b.add("talsperre", 9, List.of("Bert", "Carl"), 2));
        assertEquals(3, b.add("talsperre", 5, List.of("Dora"), 3), "gleich viele: die Spätere dahinter");
        assertEquals(0, b.add("talsperre", 0, List.of("Emil"), 4), "0 Wellen kommen nicht auf die Liste");
        assertEquals(9, b.best("talsperre"));
        assertEquals(0, b.best("klosterhof"));
        assertEquals(List.of("Bert", "Carl"), b.top("talsperre").get(0).names());
    }

    @Test
    void onlyTheBestTenStay() {
        Leaderboard b = new Leaderboard();
        for (int i = 1; i <= 12; i++) {
            b.add("klosterhof", i, List.of("P" + i), i);
        }
        assertEquals(Leaderboard.SIZE, b.top("klosterhof").size());
        assertEquals(12, b.top("klosterhof").get(0).waves());
        assertEquals(3, b.top("klosterhof").get(9).waves());
        assertEquals(0, b.add("klosterhof", 2, List.of("zu wenig"), 99));
    }

    @Test
    void leaderboardSurvivesSaving() {
        Leaderboard b = new Leaderboard();
        b.add("talsperre", 7, List.of("Anna", "Bert"), 1000);
        b.add("klosterhof", 3, List.of("Carl"), 2000);
        Leaderboard back = Leaderboard.fromYaml(b.toYaml());
        assertEquals(b.top("talsperre"), back.top("talsperre"));
        assertEquals(b.top("klosterhof"), back.top("klosterhof"));
    }

    @Test
    void playerKeepsTheirBestWavePerMap() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Anna");
        assertTrue(d.addGame("talsperre", 12, 4, 1));
        assertFalse(d.addGame("talsperre", 3, 2, 0), "schlechter: keine neue Bestleistung");
        assertTrue(d.addGame("klosterhof", 5, 6, 2));
        assertEquals(3, d.games());
        assertEquals(20, d.kills());
        assertEquals(12, d.waves());
        assertEquals(3, d.revives());
        assertEquals(4, d.best("talsperre"));
        assertEquals(6, d.bestOverall());
        PlayerData back = PlayerData.fromYaml(d.uuid(), d.toYaml());
        assertEquals(d.toYaml(), back.toYaml());
        assertEquals("Anna", back.name());
    }
}
