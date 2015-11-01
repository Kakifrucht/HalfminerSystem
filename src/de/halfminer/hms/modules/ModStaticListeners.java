package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class ModStaticListeners extends HalfminerModule implements Listener {

    @EventHandler
    public void joinNoMessage(PlayerJoinEvent e) {
        e.setJoinMessage("");
    }

    @EventHandler
    public void quitNoMessage(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler(ignoreCancelled = true)
    public void merchantBlock(InventoryClickEvent e) {
        Inventory clicked = e.getInventory();
        if (clicked != null
                && clicked.getType() == InventoryType.MERCHANT
                && !e.getWhoClicked().hasPermission("hms.bypass.merchant")) {
            ItemStack item = e.getCurrentItem();
            if (item != null) {
                if (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.APPLE) e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void deathSounds(PlayerDeathEvent e) {

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
    public void teleportRemoveJump(PlayerTeleportEvent e) {
        e.getPlayer().removePotionEffect(PotionEffectType.JUMP);
    }

    @EventHandler
    public void chatFilter(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (storage.getBoolean("sys.globalmute") && !p.hasPermission("hms.chat.advanced")) {
            p.sendMessage(Language.getMessagePlaceholders("commandChatGlobalmuteDenied", true,
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
    public void commandFilter(PlayerCommandPreprocessEvent e) {

        if (e.getPlayer().hasPermission("hms.bypass.commandfilter")) return;

        for (Character check : e.getMessage().toLowerCase().toCharArray()) {
            if (check.equals(' ')) return;
            if (check.equals(':')) {
                e.getPlayer().sendMessage(Language.getMessagePlaceholders("noPermission", true, "%PREFIX%", "Info"));
                e.setCancelled(true);
                return;
            }
        }
    }
}
