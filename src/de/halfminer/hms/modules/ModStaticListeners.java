package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class ModStaticListeners extends HalfminerModule implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage("");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {

        e.setDeathMessage("");

        //Heal and play sound
        final Player killer = e.getEntity().getKiller();
        final Player died = e.getEntity();
        if (killer != null && killer != e.getEntity()) {

            killer.setHealth(killer.getMaxHealth());
            hms.getServer().getScheduler().scheduleSyncDelayedTask(hms, new Runnable() {
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

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {

        String message = e.getMessage();
        if (e.getPlayer().hasPermission("hms.bypass.capsfilter") || message.length() < 4) return;

        int amountUppercase = 0;
        for (Character check : message.toCharArray()) if (Character.isUpperCase(check)) amountUppercase++;
        if (amountUppercase > (message.length() / 2)) e.setMessage(message.toLowerCase());

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreCommand(PlayerCommandPreprocessEvent e) {

        if (e.getPlayer().hasPermission("hms.bypass.commandfilter")) return;

        for (Character check : e.getMessage().toLowerCase().toCharArray()) {
            if (check.equals(' ')) return;
            if (check.equals(':')) {
                e.getPlayer().sendMessage(Language.getMessagePlaceholderReplace("noPermission", true, "%PREFIX%", "Hinweis"));
                e.setCancelled(true);
                return;
            }
        }
    }
}
