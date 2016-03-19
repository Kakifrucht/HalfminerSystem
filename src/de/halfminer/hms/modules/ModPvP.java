package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
import net.minecraft.server.v1_9_R1.EntityFishingHook;
import net.minecraft.server.v1_9_R1.EntityPlayer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PvP modifications/additions
 * - Strength potions damage nerfed
 * - Bow spamming disabled
 * - Kill/Deathstreaks via titles
 * - Sounds on kill/death
 * - Remove effects on teleport
 */
@SuppressWarnings("unused")
public class ModPvP extends HalfminerModule implements Listener {

    private Map<Player, Long> lastBowShot;

    private final Map<UUID, Integer> killStreaks = new HashMap<>();
    private final Map<UUID, Integer> deathStreaks = new HashMap<>();

    public ModPvP() {
        reloadConfig();
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttackReduceStrength(EntityDamageByEntityEvent e) {

        Entity damager = e.getDamager();
        if (damager.hasPermission("hms.bypass.pvp")) return;

        if (damager instanceof Player && e.getEntity() instanceof Player) {

            for (PotionEffect effect : ((Player) damager).getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {

                    double newDamage = e.getDamage(EntityDamageEvent.DamageModifier.BASE) - 1.5d * (effect.getAmplifier() + 1);
                    double damageRatio = newDamage / e.getDamage(EntityDamageEvent.DamageModifier.BASE);

                    e.setDamage(EntityDamageEvent.DamageModifier.ARMOR,
                            e.getDamage(EntityDamageEvent.DamageModifier.ARMOR) * damageRatio);
                    e.setDamage(EntityDamageEvent.DamageModifier.MAGIC,
                            e.getDamage(EntityDamageEvent.DamageModifier.MAGIC) * damageRatio);
                    e.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE,
                            e.getDamage(EntityDamageEvent.DamageModifier.RESISTANCE) * damageRatio);
                    e.setDamage(EntityDamageEvent.DamageModifier.BLOCKING,
                            e.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) * damageRatio);
                    e.setDamage(EntityDamageEvent.DamageModifier.BASE, newDamage);

                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void antiBowSpam(EntityShootBowEvent e) {

        if (!e.getEntity().hasPermission("hms.bypass.pvp") && e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            long currentTime = System.currentTimeMillis();
            if (lastBowShot.containsKey(p) && lastBowShot.get(p) + 1000 > currentTime) {
                e.setCancelled(true);
            } else lastBowShot.put(p, currentTime);
        }
    }

    /*
     * Temporary fix for the rod, will be removed after WorldGuard implements safe
     */
    @EventHandler
    public void disablePullEffect(ProjectileHitEvent e) {

        if (!(e.getEntity() instanceof FishHook)) {
            return;
        }

        Player shooter = (Player) e.getEntity().getShooter();
        EntityPlayer entityPlayer = ((CraftPlayer) shooter).getHandle();
        EntityFishingHook hook = entityPlayer.hookedFish;
        for (Entity entity : e.getEntity().getNearbyEntities(0.35D, 0.35D, 0.35D)) {

            if (((entity instanceof Player)) && (!entity.getName().equals(shooter.getName()))) {
                Player hooked = (Player) entity;
                if (hook != null) {
                    hook.hooked = null;
                    hook.die();
                }
                entityPlayer.hookedFish = null;
                break;
            }
        }
    }

    @EventHandler
    public void deathSoundsAndHeal(PlayerDeathEvent e) {

        e.setDeathMessage("");

        // Heal and play sound
        final Player killer = e.getEntity().getKiller();
        final Player died = e.getEntity();
        if (killer != null && killer != e.getEntity()) {

            killer.setHealth(killer.getMaxHealth());
            hms.getServer().getScheduler().runTaskLaterAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                }
            }, 5);

        } else {
            died.playSound(e.getEntity().getLocation(), Sound.AMBIENT_CAVE, 1.0f, 1.4f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void killstreakTitlesOnDeath(PlayerDeathEvent e) {

        // Show killstreaks in actionbar
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer != null) {

            UUID killerUid = killer.getUniqueId();
            UUID victimUid = victim.getUniqueId();

            deathStreaks.remove(killerUid);
            killStreaks.remove(victimUid);

            int killerStreak;
            int victimStreak;

            if (killStreaks.containsKey(killerUid)) killerStreak = killStreaks.get(killerUid) + 1;
            else killerStreak = 1;

            if (deathStreaks.containsKey(victimUid)) victimStreak = deathStreaks.get(victimUid) + 1;
            else victimStreak = 1;

            killStreaks.put(killerUid, killerStreak);
            deathStreaks.put(victimUid, victimStreak);

            if (killerStreak > 4) {
                TitleSender.sendActionBar(null, Language.getMessagePlaceholders("modTitlesKillStreak", false,
                        "%PLAYER%", killer.getName(), "%STREAK%", String.valueOf(killerStreak)));
            }
            if (victimStreak > 4) {
                TitleSender.sendActionBar(null, Language.getMessagePlaceholders("modTitlesDeathStreak", false,
                        "%PLAYER%", victim.getName(), "%STREAK%", String.valueOf(victimStreak)));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void teleportRemoveEffects(PlayerTeleportEvent e) {

        Player p = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (!p.hasPermission("hms.bypass.pvp") && (!from.getWorld().equals(to.getWorld()) || from.distance(to) > 100.0d)) {
            p.removePotionEffect(PotionEffectType.JUMP);
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
            p.removePotionEffect(PotionEffectType.LEVITATION);
        }
    }

    public void reloadConfig() {
        lastBowShot = new HashMap<>();
    }
}
