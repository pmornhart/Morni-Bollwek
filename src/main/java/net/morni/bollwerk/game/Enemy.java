package net.morni.bollwerk.game;

import net.morni.bollwerk.logic.EnemyType;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

/** Ein Gegner im Spiel: welche Art, welcher Weg, wohin als Nächstes. */
final class Enemy {

    final Mob mob;
    final EnemyType type;
    final int lane;
    final double damageScale;
    int next = 1;
    int attackCooldown;
    int summonCooldown = 160;
    int stuckChecks;
    Location lastPos;
    long towerHitTick = -100;
    boolean arrived;

    Enemy(Mob mob, EnemyType type, int lane, double damageScale) {
        this.mob = mob;
        this.type = type;
        this.lane = lane;
        this.damageScale = damageScale;
        this.lastPos = mob.getLocation();
    }
}
