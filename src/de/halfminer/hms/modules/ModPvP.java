package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PvP modifications/additions
 * - Golden apple regeneration nerfed
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
    public void onEatReduceRegeneration(PlayerItemConsumeEvent e) {

        final Player p = e.getPlayer();
        if (e.getPlayer().hasPermission("hms.bypass.pvp")) return;

        ItemStack item = e.getItem();
        if (item.getType() == Material.GOLDEN_APPLE && item.getDurability() == 1) {

            hms.getServer().getScheduler().runTask(hms, new Runnable() {
                @Override
                public void run() {
                    p.removePotionEffect(PotionEffectType.REGENERATION);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 3));
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttackReduceStrength(EntityDamageByEntityEvent e) {

        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {

            for (PotionEffect effect : ((Player) e.getDamager()).getActivePotionEffects()) {
                if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {

                    int level = effect.getAmplifier() + 1;

                    double newDamage =
                            e.getDamage(EntityDamageEvent.DamageModifier.BASE) / (level * 1.5d + 1.0d) + 3 * level;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void test(EntityDamageByEntityEvent e) {
        e.getDamager().sendMessage(e.getFinalDamage() + " AFTER"); //TODO REMOVE
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void test2(EntityDamageByEntityEvent e) {
        e.getDamager().sendMessage(e.getFinalDamage() + " BEFORE");
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
    public void teleportRemoveJumpAndStrength(PlayerTeleportEvent e) {

        Location from = e.getFrom();
        Location to = e.getTo();
        if (!e.getPlayer().hasPermission("hms.bypass.pvp") && (!from.getWorld().equals(to.getWorld()) || from.distance(to) > 100.0d)) {
            e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
            e.getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
    }

    public void reloadConfig() {
        lastBowShot = new HashMap<>();
    }
}
