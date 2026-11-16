package net.morni.bollwerk.listen;

import net.morni.bollwerk.BollwerkPlugin;
import net.morni.bollwerk.game.Fighter;
import net.morni.bollwerk.game.Game;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

/** Kampf in den Runden: niemand stirbt wirklich (am Boden statt tot), kein Schaden unter Freunden oder Feinden. */
public final class GameListener implements Listener {

    private final BollwerkPlugin plugin;

    public GameListener(BollwerkPlugin plugin) {
        this.plugin = plugin;
    }

    private static Entity source(Entity damager) {
        if (damager instanceof Projectile pr) {
            ProjectileSource s = pr.getShooter();
            return s instanceof Entity e ? e : null;
        }
        return damager;
    }

    private static boolean enemy(Entity e) {
        return e != null && e.getScoreboardTags().contains(Game.ENEMY_TAG);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Entity damager = e.getDamager();
        if (damager.getScoreboardTags().contains(Game.ARROW_TAG)) {
            e.setCancelled(true);
            damager.remove();
            return;
        }
        Entity from = source(damager);
        if (victim instanceof Player && from instanceof Player) {
            e.setCancelled(true);
        } else if (enemy(victim) && enemy(from)) {
            e.setCancelled(true);
        } else if (victim.getScoreboardTags().contains(Game.SHOP_TAG)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.getWorld() != plugin.world()) {
            if (e.getEntity().getScoreboardTags().contains(Game.SHOP_TAG)) {
                e.setCancelled(true);
            }
            return;
        }
        Game g = plugin.games().playing(p.getUniqueId());
        if (g == null) {
            e.setCancelled(true);
            return;
        }
        Fighter f = g.fighter(p.getUniqueId());
        if (f.state() != Fighter.State.ALIVE || g.phase() == Game.Phase.OVER) {
            e.setCancelled(true);
            return;
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
            e.setCancelled(true);
            return;
        }
        if (e.getFinalDamage() >= p.getHealth()) {
            e.setCancelled(true);
            g.down(p);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (!enemy(dead)) {
            return;
        }
        e.getDrops().clear();
        e.setDroppedExp(0);
        for (Game g : plugin.games().all()) {
            if (g.isEnemy(dead)) {
                g.enemyDied(dead, dead.getKiller());
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (e.getEntity().getWorld() != plugin.world()) {
            return;
        }
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        e.deathMessage(null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Game g = plugin.games().playing(p.getUniqueId());
        e.setRespawnLocation(g != null ? g.respawnPoint() : plugin.hall().spawn());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!enemy(e.getEntity()) || e.getTarget() == null) {
            return;
        }
        if (!(e.getTarget() instanceof Player p)) {
            e.setCancelled(true);
            return;
        }
        Game g = plugin.games().playing(p.getUniqueId());
        if (g == null || g.fighter(p.getUniqueId()).state() != Fighter.State.ALIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArrow(ProjectileHitEvent e) {
        if (e.getEntity().getScoreboardTags().contains(Game.ARROW_TAG)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> e.getEntity().remove());
        }
    }
}
