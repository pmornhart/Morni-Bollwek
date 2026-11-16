package de.moinmorni.bollwerk.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class WavePlanTest {

    private static int budgetUsed(WavePlan p) {
        return p.enemies().stream().mapToInt(EnemyType::cost).sum();
    }

    @Test
    void firstWaveIsSmallAndOnlyPlunderers() {
        WavePlan p = WavePlan.compose(1, 1);
        assertEquals(Map.of(EnemyType.PLUENDERER, 7), p.counts());
        assertEquals(1.0, p.healthScale());
        assertEquals(1.0, p.damageScale());
        assertFalse(p.bossWave());
    }

    @Test
    void sameWaveSameEnemies() {
        assertEquals(WavePlan.compose(7, 3).enemies(), WavePlan.compose(7, 3).enemies());
    }

    @Test
    void budgetIsUsedUpExactly() {
        for (int w = 1; w <= 30; w++) {
            for (int n = 1; n <= 6; n++) {
                WavePlan p = WavePlan.compose(w, n);
                double budget = WavePlan.budget(w, n) * (p.bossWave() ? 0.6 : 1);
                if (p.enemies().size() < WavePlan.MAX_ENEMIES) {
                    assertEquals(Math.round(budget), budgetUsed(p), "Welle " + w + ", " + n + " Leute");
                }
            }
        }
    }

    @Test
    void wavesGrowWithWaveNumberAndPlayers() {
        for (int w = 1; w < 30; w++) {
            if (EnemyType.bossFor(w) != null || EnemyType.bossFor(w + 1) != null) {
                continue;
            }
            assertTrue(WavePlan.budget(w + 1, 1) > WavePlan.budget(w, 1));
            WavePlan next = WavePlan.compose(w + 1, 2);
            if (next.enemies().size() < WavePlan.MAX_ENEMIES) {
                assertTrue(budgetUsed(next) >= budgetUsed(WavePlan.compose(w, 2)), "Welle " + w);
            }
        }
        for (int n = 1; n < 6; n++) {
            assertTrue(budgetUsed(WavePlan.compose(8, n + 1)) > budgetUsed(WavePlan.compose(8, n)), n + " Leute");
            assertTrue(WavePlan.compose(8, n + 1).healthScale() > WavePlan.compose(8, n).healthScale());
        }
        assertTrue(WavePlan.compose(12, 1).healthScale() > WavePlan.compose(2, 1).healthScale());
        assertTrue(WavePlan.compose(12, 1).damageScale() > WavePlan.compose(2, 1).damageScale());
        assertTrue(WavePlan.compose(25, 1).spawnTicks() < WavePlan.compose(1, 1).spawnTicks());
        assertTrue(WavePlan.compose(60, 1).spawnTicks() >= 8);
    }

    @Test
    void playerCountIsClamped() {
        assertEquals(WavePlan.compose(4, 6).enemies(), WavePlan.compose(4, 50).enemies());
        assertEquals(WavePlan.compose(4, 1).enemies(), WavePlan.compose(4, 0).enemies());
    }

    @Test
    void enemyTypesOnlyFromTheirFirstWave() {
        for (int w = 1; w <= 20; w++) {
            for (EnemyType t : WavePlan.compose(w, 4).enemies()) {
                assertTrue(t.firstWave() <= w, t + " in Welle " + w);
            }
        }
    }

    @Test
    void newEnemyTypeIsIntroduced() {
        assertTrue(WavePlan.compose(2, 1).counts().containsKey(EnemyType.FLINKER));
        assertTrue(WavePlan.compose(3, 1).counts().containsKey(EnemyType.SCHILDTRAEGER));
        assertTrue(WavePlan.compose(4, 1).counts().containsKey(EnemyType.ARMBRUSTSCHUETZE));
        assertTrue(WavePlan.compose(6, 1).counts().containsKey(EnemyType.BRECHER));
    }

    @Test
    void everyFifthWaveHasABossAtTheEnd() {
        assertEquals(EnemyType.LAWINENRIESE, EnemyType.bossFor(5));
        assertEquals(EnemyType.NEBELHEXE, EnemyType.bossFor(10));
        assertEquals(EnemyType.LAWINENRIESE, EnemyType.bossFor(15));
        assertNull(EnemyType.bossFor(4));
        assertNull(EnemyType.bossFor(0));
        for (int w : new int[] {5, 10, 15, 20}) {
            WavePlan p = WavePlan.compose(w, 2);
            assertTrue(p.bossWave());
            assertEquals(EnemyType.bossFor(w), p.enemies().get(p.enemies().size() - 1), "Boss zuletzt");
            assertEquals(1, p.enemies().stream().filter(EnemyType::boss).count());
            assertTrue(budgetUsed(p) < budgetUsed(WavePlan.compose(w - 1, 2)) + 10, "weniger Begleitung");
        }
        for (int w : new int[] {1, 4, 6, 9}) {
            assertTrue(WavePlan.compose(w, 3).enemies().stream().noneMatch(EnemyType::boss));
        }
    }

    @Test
    void neverMoreThanTheCap() {
        WavePlan p = WavePlan.compose(200, 6);
        assertEquals(WavePlan.MAX_ENEMIES, p.enemies().size());
    }

    @Test
    void healthScalesPerType() {
        WavePlan p = WavePlan.compose(11, 3);
        assertEquals(EnemyType.PLUENDERER.health() * (1 + 0.10 * 10 + 0.20 * 2), p.health(EnemyType.PLUENDERER), 0.05);
    }
}
