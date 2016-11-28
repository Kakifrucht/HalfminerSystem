package de.halfminer.hms.modules;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * - Shows healthbar of attacking/attacked player/entity in bossbar
 *   - Contains playername / mobname
 *   - If player, shows players skilllevel
 *   - Updates on damage or health regain for every player who hit the entity
 *   - Dynamic bar segmentation, solid if none available client side
 * - Only shows one bar at a time
 *   - Shows bar and updates for 8 seconds max, or until other entity was hit
 *   - When entity was killed, shows bar in green and only for 2 seconds
 */
@SuppressWarnings("unused")
public class ModHealthBar extends HalfminerModule implements Listener {

    private final Map<Player, BarUpdateContainer> playerSeesBar = new HashMap<>();
    private final Map<Damageable, List<Player>> damageableHealthSeenBy = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityHit(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Damageable || e.getDamager() instanceof Projectile)
                || !(e.getEntity() instanceof Damageable))
            return;

        Damageable victim = (Damageable) e.getEntity();
        Damageable attacker;

        if (e.getDamager() instanceof Projectile) {

            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Damageable)
                attacker = (Damageable) projectile.getShooter();
            else return;

        } else attacker = (Damageable) e.getDamager();

        if (victim instanceof Player) addToDamageable(attacker, (Player) victim);
        if (attacker instanceof Player) addToDamageable(victim, (Player) attacker);

        updateBossbars(attacker, (int) attacker.getHealth());
        updateBossbars(victim, (int) Math.ceil(victim.getHealth() - e.getFinalDamage()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHealthRegain(EntityRegainHealthEvent e) {

        if (isApplicableHealthChangeEvent(e)) {
            Damageable d = (Damageable) e.getEntity();
            updateBossbars(d, (int) Math.ceil(d.getHealth() + e.getAmount()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {

        if (e instanceof EntityDamageByEntityEvent) return;

        if (isApplicableHealthChangeEvent(e)) {
            Damageable entity = (Damageable) e.getEntity();
            updateBossbars(entity, (int) Math.ceil(entity.getHealth() - e.getFinalDamage()));
        }
    }

    private boolean isApplicableHealthChangeEvent(EntityEvent e) {
        return e.getEntity() instanceof Damageable
                && damageableHealthSeenBy.containsKey(e.getEntity())
                && !damageableHealthSeenBy.get(e.getEntity()).isEmpty();
    }

    private void addToDamageable(Damageable addTo, Player toAdd) {

        if (playerSeesBar.containsKey(toAdd)) {
            playerSeesBar.get(toAdd).setLastDamageable(addTo);
        } else {
            playerSeesBar.put(toAdd, new BarUpdateContainer(toAdd, addTo));
        }
    }

    private void updateBossbars(Damageable entityToUpdate, int newHealth) {

        if (!damageableHealthSeenBy.containsKey(entityToUpdate)) return;

        List<Player> updatePlayers = damageableHealthSeenBy.get(entityToUpdate);
        Player otherPlayer = entityToUpdate instanceof Player ? (Player) entityToUpdate : null;

        int health;
        double maxHealth;
        double healthRatio;
        BarStyle style;
        BarColor color;

        maxHealth = entityToUpdate.getMaxHealth();

        if (newHealth == -1) health = (int) entityToUpdate.getHealth();
        else health = Math.max(newHealth, 0);
        health = Math.min(health, (int) maxHealth);

        healthRatio = health / maxHealth;

        // get segmented style for maxhealth if available, else show solid bar
        try {
            style = BarStyle.valueOf("SEGMENTED_" + (int) maxHealth);
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        if (health == 0) color = BarColor.GREEN;
        else if (healthRatio < 0.2d) color = BarColor.YELLOW;
        else color = BarColor.RED;

        for (Player toUpdate : updatePlayers) {
            // disregard if no longer online, will be removed automatically by bukkit scheduler
            if (!toUpdate.isOnline()) continue;

            barHandler.sendBar(toUpdate,
                    MessageBuilder.create(hms, "modCombatLogBossBar")
                            .addPlaceholderReplace("%PLAYER%", entityToUpdate.getName())
                            .addPlaceholderReplace("%LEVEL%", otherPlayer != null ?
                                    storage.getPlayer(otherPlayer).getString(DataType.SKILL_LEVEL) : "1")
                            .addPlaceholderReplace("%HEALTH%", String.valueOf(health))
                            .addPlaceholderReplace("%MAXHEALTH%", String.valueOf((int) maxHealth))
                            .returnMessage(),
                    color,
                    style,
                    health > 0 ? 8 : 2, // only show bar for two seconds if dead, else 8 seconds
                    healthRatio);
        }
    }

    private class BarUpdateContainer {

        BukkitTask currentRemovalTask;

        final Player player;
        Damageable currentDamageable;

        BarUpdateContainer(Player player, Damageable initiatedDamageable) {

            this.player = player;
            setLastDamageable(initiatedDamageable);
            scheduleTask();
        }

        void setLastDamageable(Damageable updateTo) {

            // remove from old update map
            cleanFromDamageableList();

            // set and add to new updatelist
            currentDamageable = updateTo;

            List<Player> list;

            if (!damageableHealthSeenBy.containsKey(updateTo)) {
                list = new LinkedList<>();
                damageableHealthSeenBy.put(updateTo, list);
            } else list = damageableHealthSeenBy.get(updateTo);

            if (!list.contains(player)) list.add(player);
            scheduleTask();
        }

        void cleanFromDamageableList() {

            if (damageableHealthSeenBy.containsKey(currentDamageable)) {
                List<Player> list = damageableHealthSeenBy.get(currentDamageable);
                list.remove(player);
                if (list.isEmpty())
                    damageableHealthSeenBy.remove(currentDamageable);
            }
        }

        void scheduleTask() {

            if (currentRemovalTask != null)
                currentRemovalTask.cancel();

            currentRemovalTask = scheduler.runTaskLater(hms, () -> {

                cleanFromDamageableList();
                playerSeesBar.remove(player);
                barHandler.removeBar(player);
            }, 120L);
        }
    }
}
