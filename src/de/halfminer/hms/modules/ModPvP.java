package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
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
 * - Golden apple nerfed
 * - Strength potions nerfed
 * - Bow spamming disabled
 * - Kill/Deathstreaks via titles
 * - Sounds on kill/death
 * - Remove effects on teleport
 */
public class ModPvP extends HalfminerModule implements Listener {

    private Map<Player, Long> lastBowShot;

    private final Map<UUID, Integer> killStreaks = new HashMap<>();
    private final Map<UUID, Integer> deathStreaks = new HashMap<>();

    public ModPvP() {
        reloadConfig();
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onEat(PlayerItemConsumeEvent e) {

        Player p = e.getPlayer();
        if (p.hasPermission("hms.bypass.pvp")) return;

        ItemStack item = e.getItem();
        if (item.getType() == Material.GOLDEN_APPLE && item.getDurability() == 1) {

            updateEffect(p, PotionEffectType.REGENERATION, 300, 3);
        } else if (item.getType() == Material.POTION
                && (item.getDurability() == 8201 || item.getDurability() == 8265 || item.getDurability() == 8233)) {

            reduceStrengthDuration(p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onPotionSplash(PotionSplashEvent e) {

        for (PotionEffect effect : e.getPotion().getEffects()) {
            if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {

                for (LivingEntity entity : e.getAffectedEntities()) {
                    if (entity instanceof Player) {
                        Player p = (Player) entity;
                        if (!p.hasPermission("hms.bypass.pvp"))
                            reduceStrengthDuration((Player) entity);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
                    killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 1.0f, 2.0f);
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 1.0f, 0.5f);
                }
            }, 5);

        } else {
            died.playSound(e.getEntity().getLocation(), Sound.AMBIENCE_CAVE, 1.0f, 1.4f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public void teleportRemoveJumpAndStrength(PlayerTeleportEvent e) {

        if (!e.getPlayer().hasPermission("hms.bypass.pvp") && e.getFrom().distance(e.getTo()) > 100.0d) {
            e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
            e.getPlayer().removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        }
    }

    private void updateEffect(final Player player, final PotionEffectType effect, final int time, final int amplifier) {

        hms.getServer().getScheduler().runTask(hms, new Runnable() {
            @Override
            public void run() {
                player.removePotionEffect(effect);
                player.addPotionEffect(new PotionEffect(effect, time, amplifier));
            }
        });
    }

    private void reduceStrengthDuration(final Player player) {

        final PotionEffect oldEffect = getStrengthFromPlayer(player);

        hms.getServer().getScheduler().runTask(hms, new Runnable() {
            @Override
            public void run() {

                PotionEffect newEffect = getStrengthFromPlayer(player);

                if (newEffect != null
                        && (oldEffect == null || !isSameEffect(oldEffect, newEffect))) {

                    int nerfRatio = 3;
                    int amplifier = newEffect.getAmplifier();
                    if (amplifier > 0) nerfRatio *= 6;

                    updateEffect(player, PotionEffectType.INCREASE_DAMAGE,
                            newEffect.getDuration() / nerfRatio, amplifier);
                }
            }
        });
    }

    private boolean isSameEffect(PotionEffect oldEff, PotionEffect newEff) {
        int oldDuration = oldEff.getDuration();
        int oldDurationBounds = oldDuration - 20;
        int newDuration = newEff.getDuration();

        return oldEff.getType().equals(newEff.getType())
                && oldEff.getAmplifier() == newEff.getAmplifier()
                && newDuration > oldDurationBounds
                && oldDuration > newDuration;
    }

    private PotionEffect getStrengthFromPlayer(Player player) {

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) return effect;
        }

        return null;
    }

    public void reloadConfig() {
        lastBowShot = new HashMap<>();
    }
}
