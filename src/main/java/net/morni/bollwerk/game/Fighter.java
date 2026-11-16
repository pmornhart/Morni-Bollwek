package net.morni.bollwerk.game;

import java.util.UUID;
import net.morni.bollwerk.logic.Economy;
import net.morni.bollwerk.logic.Offer;

/** Eine Person in einer Runde: Geld, Ausrüstung, Zustand (steht, am Boden, ausgeschieden). */
public final class Fighter {

    public enum State {
        ALIVE, DOWNED, OUT
    }

    private final UUID uuid;
    private final String name;
    private final Economy.Wallet wallet;
    private final Offer.Gear gear = new Offer.Gear();
    private State state = State.ALIVE;
    private boolean ready;
    private int kills;
    private int revives;
    private int downTicks;
    private int reviveTicks;
    private UUID reviver;
    private boolean online = true;

    Fighter(UUID uuid, String name, int gold) {
        this.uuid = uuid;
        this.name = name;
        this.wallet = new Economy.Wallet(gold);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Economy.Wallet wallet() {
        return wallet;
    }

    public Offer.Gear gear() {
        return gear;
    }

    public State state() {
        return state;
    }

    void state(State s) {
        state = s;
        reviveTicks = 0;
        reviver = null;
    }

    public boolean ready() {
        return ready;
    }

    void ready(boolean r) {
        ready = r;
    }

    public int kills() {
        return kills;
    }

    void addKill() {
        kills++;
    }

    public int revives() {
        return revives;
    }

    void addRevive() {
        revives++;
    }

    int downTicks() {
        return downTicks;
    }

    void downTicks(int t) {
        downTicks = t;
    }

    int reviveTicks() {
        return reviveTicks;
    }

    void reviveTicks(int t, UUID by) {
        reviveTicks = t;
        reviver = by;
    }

    UUID reviver() {
        return reviver;
    }

    public boolean online() {
        return online;
    }

    void online(boolean o) {
        online = o;
    }
}
