package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * - Logs and notifies about possible Bedrock/Obsidian glitching
 * - Kills players above the netherroof
 */
public class ModGlitchProtection extends HalfminerModule implements Listener {

    private final Map<Player, Long> lastMessage = new HashMap<>();
    private BukkitTask checkIfOverNether;

    public ModGlitchProtection() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onMoveBedrockObsidianCheck(PlayerMoveEvent e) {

        if ((e.getFrom().getBlock().getType() == Material.BEDROCK || e.getFrom().getBlock().getType() == Material.OBSIDIAN)
                && !e.getPlayer().hasPermission("hms.bypass.bedrockcheck")
                && Math.round(e.getFrom().getY()) == e.getFrom().getBlockY()) {

            if (lastMessage.get(e.getPlayer()) == null || lastMessage.get(e.getPlayer()) < System.currentTimeMillis() / 1000) {

                hms.getServer().broadcast(Language.getMessagePlaceholders("modGlitchProtectionBedrock", true,
                        "%PREFIX%", "Warnung",
                        "%PLAYER%", e.getPlayer().getName(),
                        "%LOCATION%", Language.getStringFromLocation(e.getTo())), "hms.bypass.bedrockcheck");
                lastMessage.put(e.getPlayer(), (System.currentTimeMillis() / 1000) + 4);
            }
        }
    }

    @Override
    public void reloadConfig() {

        if (checkIfOverNether != null) return;

        checkIfOverNether = hms.getServer().getScheduler().runTaskTimer(hms, new Runnable() {
            @Override
            public void run() {
                for (Player p : hms.getServer().getOnlinePlayers()) {
                    Location loc = p.getLocation();
                    if (!p.hasPermission("hms.bypass.nethercheck")
                            && !p.isDead()
                            && loc.getWorld().getEnvironment().equals(World.Environment.NETHER)
                            && loc.getBlockY() > 127) {
                        p.setHealth(0.0d);
                        p.sendMessage(Language.getMessagePlaceholders("modGlitchProtectionNether", true,
                                "%PREFIX%", "Warnung"));
                        hms.getServer().broadcast(Language.getMessagePlaceholders("modGlitchProtectionNetherNotify", true,
                                "%PREFIX%", "Warnung",
                                "%PLAYER%", p.getName(),
                                "%LOCATION%", Language.getStringFromLocation(loc)), "hms.bypass.nethercheck");
                    }
                }
            }
        }, 100L, 100L);
    }
}
