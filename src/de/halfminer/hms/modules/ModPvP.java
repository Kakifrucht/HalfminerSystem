package de.halfminer.hms.modules;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class ModPvP extends HalfminerModule implements Listener {

    private Map<Player, Long> lastShot = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onEat(PlayerItemConsumeEvent e) {

        final Player p = e.getPlayer();
        if (p.hasPermission("hms.bypass.pvp")) return;

        ItemStack item = e.getItem();
        if (item.getType() == Material.GOLDEN_APPLE && item.getDurability() == 1) {

            updateEffect(p, PotionEffectType.REGENERATION, 300, 3);
        } else if (item.getType() == Material.POTION
                && (item.getDurability() == 8201 || item.getDurability() == 8265 || item.getDurability() == 8233)) {

            nerfEffect(p, PotionEffectType.INCREASE_DAMAGE);
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
                            nerfEffect((Player) entity, PotionEffectType.INCREASE_DAMAGE);
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
            if (lastShot.containsKey(p) && lastShot.get(p) + 1000 > currentTime) {
                e.setCancelled(true);
            } else lastShot.put(p, currentTime);
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

    private void nerfEffect(final Player player, final PotionEffectType effect) {

        final PotionEffect oldEffect = getEffectFromType(player, effect);

        hms.getServer().getScheduler().runTask(hms, new Runnable() {
            @Override
            public void run() {

                PotionEffect newEffect = getEffectFromType(player, effect);

                if (newEffect != null
                        && (oldEffect == null || !isSameEffect(oldEffect, newEffect))) {

                    int nerfRatio = 3;
                    int amplifier = newEffect.getAmplifier();
                    if (amplifier > 0) nerfRatio *= 6;

                    updateEffect(player, effect, newEffect.getDuration() / nerfRatio, amplifier);
                }
            }
        });
    }

    private boolean isSameEffect(PotionEffect oldEff, PotionEffect newEff) {
        int oldDuration = oldEff.getDuration();
        int oldDurationBounds = oldDuration - 20;
        int newDuration = newEff.getDuration();

        return oldDuration > newDuration
                && newDuration > oldDurationBounds
                && oldEff.getAmplifier() == newEff.getAmplifier()
                && oldEff.getType().equals(newEff.getType());
    }

    private PotionEffect getEffectFromType(Player player, PotionEffectType type) {

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(type)) return effect;
        }

        return null;
    }
}
