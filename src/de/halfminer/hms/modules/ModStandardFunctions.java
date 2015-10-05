package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class ModStandardFunctions implements Listener, HalfminerModule {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage("");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        e.setLeaveMessage("");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.setDeathMessage("");
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {

        String message = e.getMessage();
        if (message.length() < 4 || e.getPlayer().isOp()) return;

        int amountUppercase = 0;
        for (Character check : message.toCharArray()) if (Character.isUpperCase(check)) amountUppercase++;
        if (amountUppercase > (message.length() / 2)) e.setMessage(message.toLowerCase());

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreCommand(PlayerCommandPreprocessEvent e) {

        if (e.getPlayer().isOp()) return;

        for (Character check : e.getMessage().toLowerCase().toCharArray()) {
            if (check.equals(' ')) return;
            if (check.equals(':')) {
                e.getPlayer().sendMessage(Language.getMessagePlaceholderReplace("noPermission", true, "%PREFIX%", "Hinweis"));
                e.setCancelled(true);
                return;
            }
        }
    }

    @Override
    public void reloadConfig() {
    }
}
