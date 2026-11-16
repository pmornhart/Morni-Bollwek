package de.moinmorni.bollwerk.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DefenceTest {

    @Test
    void everyLevelIsBetter() {
        for (DefenceType t : DefenceType.values()) {
            for (int l = 1; l < DefenceType.MAX_LEVEL; l++) {
                assertTrue(t.health(l + 1) > t.health(l), t + " Leben");
                assertTrue(t.range(l + 1) >= t.range(l), t + " Reichweite");
                assertTrue(t.power(l + 1) >= t.power(l), t + " Stärke");
                assertTrue(t.interval(l + 1) <= t.interval(l), t + " Takt");
                assertTrue(t.upgradePrice(l) > 0);
            }
            assertEquals(-1, t.upgradePrice(DefenceType.MAX_LEVEL), "über Stufe 3 geht nichts");
            assertEquals(-1, t.upgradePrice(0));
        }
    }

    @Test
    void spotsMatchTheBuildings() {
        assertEquals(DefenceType.Spot.LANE, DefenceType.PALISADE.spot());
        assertEquals(DefenceType.Spot.LANE, DefenceType.KESSELFALLE.spot());
        assertEquals(DefenceType.Spot.SIDE, DefenceType.PFEILTURM.spot());
        assertEquals(DefenceType.Spot.SIDE, DefenceType.HEILBANNER.spot());
    }

    @Test
    void towerFiresFasterWhenUpgraded() {
        assertEquals(30, DefenceType.PFEILTURM.interval(1));
        assertEquals(18, DefenceType.PFEILTURM.interval(3));
        assertEquals(4, DefenceType.PFEILTURM.power(1));
        assertEquals(12, DefenceType.PFEILTURM.range(1));
    }

    @Test
    void cauldronSlowsAndBannerHealsTheHeartOnlyAtTheTop() {
        assertEquals(0, DefenceType.KESSELFALLE.slowAmplifier(1));
        assertEquals(2, DefenceType.KESSELFALLE.slowAmplifier(3));
        assertEquals(-1, DefenceType.PFEILTURM.slowAmplifier(1));
        assertFalse(DefenceType.HEILBANNER.healsHeart(2));
        assertTrue(DefenceType.HEILBANNER.healsHeart(3));
        assertFalse(DefenceType.PFEILTURM.healsHeart(3));
    }

    @Test
    void repairsCostOneGuilderPerFourHealth() {
        assertEquals(0, DefenceType.repairPrice(0));
        assertEquals(1, DefenceType.repairPrice(0.5));
        assertEquals(1, DefenceType.repairPrice(4));
        assertEquals(3, DefenceType.repairPrice(9));
        assertEquals(30, DefenceType.repairPrice(120));
    }

    @Test
    void breakersSmashWallsFourTimesHarder() {
        assertEquals(4.0, DefenceType.hit(EnemyType.BRECHER, 1) / DefenceType.hit(EnemyType.PLUENDERER, 1));
        assertEquals(12 * 1.5, DefenceType.hit(EnemyType.BRECHER, 1.5), 1e-9);
        assertTrue(EnemyType.BRECHER.wallBreaker());
        assertTrue(EnemyType.LAWINENRIESE.wallBreaker());
        assertFalse(EnemyType.PLUENDERER.wallBreaker());
    }

    @Test
    void towerAimsAtWhoeverIsClosestToTheHeart() {
        List<Targeting.Candidate> all = List.of(
                new Targeting.Candidate("a", 5, 0, 0, 0.30),
                new Targeting.Candidate("b", 9, 0, 0, 0.80),
                new Targeting.Candidate("c", 30, 0, 0, 0.99),
                new Targeting.Candidate("d", 0, 0, 3, 0.80));
        assertEquals("d", Targeting.pick(0, 0, 0, 12, all).orElseThrow().id(), "gleich weit, näher am Turm");
        assertEquals("a", Targeting.pick(0, 0, 0, 6, List.of(all.get(0), all.get(1), all.get(2))).orElseThrow().id());
        assertTrue(Targeting.pick(100, 0, 100, 12, all).isEmpty(), "niemand in Reichweite");
        assertEquals("c", Targeting.pick(0, 0, 0, 40, all).orElseThrow().id());
        assertEquals(2, Targeting.within(0, 0, 0, 5, all).size());
    }

    @Test
    void shieldsHalveTowerArrows() {
        assertEquals(4, Targeting.arrowDamage(DefenceType.PFEILTURM, 1, EnemyType.PLUENDERER));
        assertEquals(2, Targeting.arrowDamage(DefenceType.PFEILTURM, 1, EnemyType.SCHILDTRAEGER));
        assertEquals(9, Targeting.arrowDamage(DefenceType.PFEILTURM, 3, EnemyType.FLINKER));
    }
}
