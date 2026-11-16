package net.morni.bollwerk.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EconomyTest {

    @Test
    void rewardsGrowWithTheWave() {
        assertEquals(5, Economy.killReward(EnemyType.PLUENDERER, 1));
        assertEquals(7, Economy.killReward(EnemyType.PLUENDERER, 9));
        assertEquals(80, Economy.killReward(EnemyType.LAWINENRIESE, 1));
        assertTrue(Economy.killReward(EnemyType.BRECHER, 10) > Economy.killReward(EnemyType.FLINKER, 10));
        assertEquals(20, Economy.waveBonus(1));
        assertEquals(65, Economy.waveBonus(10));
        assertEquals(20, Economy.waveBonus(0), "Welle 0 zählt wie 1");
    }

    @Test
    void sharesAreRoundedUp() {
        assertEquals(4, Economy.share(10, 3));
        assertEquals(10, Economy.share(10, 1));
        assertEquals(10, Economy.share(10, 0));
        assertEquals(0, Economy.share(0, 4));
    }

    @Test
    void walletOnlyPaysWhatItHas() {
        Economy.Wallet w = new Economy.Wallet(50);
        assertTrue(w.pay(30));
        assertEquals(20, w.balance());
        assertFalse(w.pay(21));
        assertEquals(20, w.balance(), "nichts abgezogen");
        assertFalse(w.pay(-5));
        w.earn(15);
        w.earn(-100);
        assertEquals(35, w.balance());
        assertEquals(15, w.earned());
        assertTrue(w.pay(35));
        assertEquals(0, w.balance());
        assertEquals(0, new Economy.Wallet(-10).balance());
    }

    @Test
    void shopTiersOnlyGoUp() {
        Offer.Gear gear = new Offer.Gear();
        assertTrue(Offer.EISENSCHWERT.allowed(gear));
        assertTrue(Offer.DIAMANTSCHWERT.allowed(gear), "Stufe überspringen geht");
        gear.apply(Offer.DIAMANTSCHWERT);
        assertEquals(3, gear.tier("schwert"));
        assertFalse(Offer.EISENSCHWERT.allowed(gear));
        assertFalse(Offer.DIAMANTSCHWERT.allowed(gear));
        gear.apply(Offer.EISENSCHWERT);
        assertEquals(3, gear.tier("schwert"), "nie zurück");
        assertTrue(Offer.HEILTRANK.allowed(gear));
        gear.apply(Offer.KETTENRUESTUNG);
        assertTrue(Offer.EISENRUESTUNG.allowed(gear));
        assertFalse(Offer.KETTENRUESTUNG.allowed(gear));
    }

    @Test
    void blueprintsCostWhatTheBuildingCosts() {
        for (Offer o : Offer.values()) {
            if (o.defence() != null) {
                assertEquals(o.defence().price(), o.price(), o.id());
                assertEquals(Offer.Category.BAUTEN, o.category());
            }
        }
        assertEquals(Offer.BAUPLAN_PFEILTURM, Offer.byId("BAUPLAN_pfeilturm"));
    }
}
