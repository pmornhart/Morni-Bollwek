package net.morni.bollwerk.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeartTest {

    @Test
    void fallsExactlyOnce() {
        Heart h = new Heart(100);
        assertFalse(h.damage(40));
        assertEquals(60, h.health());
        assertEquals(0.6, h.fraction(), 1e-9);
        assertFalse(h.damage(0));
        assertFalse(h.damage(-5));
        assertTrue(h.damage(80), "fällt jetzt");
        assertEquals(0, h.health(), "nie unter 0");
        assertTrue(h.fallen());
        assertFalse(h.damage(10), "fällt nicht zweimal");
    }

    @Test
    void repairStopsAtTheTopAndNotAfterTheFall() {
        Heart h = new Heart(100);
        h.damage(30);
        assertEquals(30, h.repair(50));
        assertEquals(100, h.health());
        assertEquals(0, h.repair(10));
        h.damage(200);
        assertEquals(0, h.repair(50), "gefallen bleibt gefallen");
    }

    @Test
    void enemiesHitTheHeartWithScaledDamage() {
        Heart h = new Heart(400);
        WavePlan w = WavePlan.compose(11, 2);
        for (int i = 0; i < 10; i++) {
            h.damage(EnemyType.PLUENDERER.heartDamage() * w.damageScale());
        }
        assertEquals(400 - 10 * 4 * 1.6, h.health(), 1e-9);
        Heart small = new Heart(0);
        assertEquals(1, small.max(), "mindestens 1");
    }

    @Test
    void adminCanSetTheHealth() {
        Heart h = new Heart(50);
        h.set(20);
        assertEquals(20, h.health());
        h.set(999);
        assertEquals(50, h.health());
        h.set(-1);
        assertTrue(h.fallen());
    }
}
