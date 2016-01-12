package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Material;
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

@SuppressWarnings("unused")
public class ModPvP extends HalfminerModule implements Listener {

    private Map<Player, Long> lastShot = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent e) {

        final Player p = e.getPlayer();
        if (p.hasPermission("hms.bypass.pvp")) return;

        ItemStack item = e.getItem();
        if (item.getType() == Material.GOLDEN_APPLE && item.getDurability() == 1) {

            hms.getServer().getScheduler().runTask(hms, new Runnable() {
                @Override
                public void run() {
                    p.removePotionEffect(PotionEffectType.REGENERATION);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 2));
                }
            });
        } else if (item.getType() == Material.POTION && item.getDurability() == 8233) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Language.getMessagePlaceholders("modPvPDisabledStrength", true, "%PREFIX%", "PvP"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent e) {
        for (PotionEffect effect : e.getPotion().getEffects()) {
            if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE) && effect.getAmplifier() > 0) {

                e.setCancelled(true);
                if (e.getPotion().getShooter() instanceof Player) {
                    ((Player) e.getPotion().getShooter()).sendMessage(
                            Language.getMessagePlaceholders("modPvPDisabledStrength", true, "%PREFIX%", "PvP"));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void antiBowSpam(EntityShootBowEvent e) {

        if (!e.getEntity().hasPermission("hms.bypass.pvp") && e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            long currentTime = System.currentTimeMillis();
            if (lastShot.containsKey(p) && lastShot.get(p) + 750 > currentTime) {
                e.setCancelled(true);
            } else lastShot.put(p, currentTime);
        }
    }
}
