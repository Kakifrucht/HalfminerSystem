package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;

public class ModBedrockProtection extends HalfminerModule implements Listener {

    private final Map<Player, Long> lastMessage = new HashMap<>();

    public ModBedrockProtection() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onMoveBedrockCheck(PlayerMoveEvent e) {

        if ((e.getFrom().getBlock().getType() == Material.BEDROCK || e.getFrom().getBlock().getType() == Material.OBSIDIAN)
                && !e.getPlayer().hasPermission("hms.bypass.bedrockcheck")
                && Math.round(e.getFrom().getY()) == e.getFrom().getBlockY()) {

            if (lastMessage.get(e.getPlayer()) == null || lastMessage.get(e.getPlayer()) < System.currentTimeMillis() / 1000) {

                Bukkit.broadcast(Language.getMessagePlaceholders("modBedrockProtectionGlitching", true, "%PREFIX%", "Warnung",
                        "%PLAYER%", e.getPlayer().getName(), "%LOCATION%", Language.getStringFromLocation(e.getTo())), "hms.admin");
                lastMessage.put(e.getPlayer(), (System.currentTimeMillis() / 1000) + 4);
            }
        }
    }
}
