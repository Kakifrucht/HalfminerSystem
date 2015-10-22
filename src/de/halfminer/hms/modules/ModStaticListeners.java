package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
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

        //Title on join
        final Player joined = e.getPlayer();
        int timeOnline = storage.getStatsInt(joined, StatsType.TIME_ONLINE);

        if (timeOnline == 0) {
            TitleSender.sendTitle(joined, Language.getMessage("modStaticListenersNewPlayerFormat"));
        } else {
            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    TitleSender.sendTitle(joined, Language.getMessagePlaceholderReplace("modStaticListenersJoinFormat",
                            false, "%NEWS%", storage.getString("sys.news")));

                    try {
                        Thread.sleep(6000l);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    TitleSender.sendTitle(joined, Language.getMessagePlaceholderReplace("modStaticListenersNewsFormat",
                            false, "%NEWS%", storage.getString("sys.news")), 40, 180, 40);
                }
            });

        }
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

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (storage.getBoolean("sys.globalmute") && !p.hasPermission("hms.chat.advanced")) {
            p.sendMessage(Language.getMessagePlaceholderReplace("commandChatGlobalmuteDenied", true,
                    "%PREFIX%", "Globalmute"));
            e.setCancelled(true);
        } else {
            String message = e.getMessage();
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1.0f, 1.9f);
            if (p.hasPermission("hms.bypass.capsfilter") || message.length() < 4) return;

            int amountUppercase = 0;
            for (Character check : message.toCharArray()) if (Character.isUpperCase(check)) amountUppercase++;
            if (amountUppercase > (message.length() / 2)) e.setMessage(message.toLowerCase());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreCommand(PlayerCommandPreprocessEvent e) {

        if (e.getPlayer().hasPermission("hms.bypass.commandfilter")) return;

        for (Character check : e.getMessage().toLowerCase().toCharArray()) {
            if (check.equals(' ')) return;
            if (check.equals(':')) {
                e.getPlayer().sendMessage(Language.getMessagePlaceholderReplace("noPermission", true, "%PREFIX%", "Info"));
                e.setCancelled(true);
                return;
            }
        }
    }
}
