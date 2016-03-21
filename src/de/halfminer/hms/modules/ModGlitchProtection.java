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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * - Logs and notifies about possible Bedrock/Obsidian glitching
 * - Override spigot teleport safety
 * - Prevents glitching with chorus fruit, instead teleports down
 * - Kills players above the netherroof
 */
@SuppressWarnings("unused")
public class ModGlitchProtection extends HalfminerModule implements Listener {

    private final Map<Player, Long> lastMessage = new HashMap<>();
    private final Set<Player> waitingForChorusTP = new HashSet<>();
    private BukkitTask checkIfOverNether;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleportPreventGlitch(PlayerTeleportEvent e) {

        final Player p = e.getPlayer();
        final Location to = e.getTo();

        if (p.hasPermission("hms.bypass.teleportchecks")) return;

        if (!e.getFrom().getWorld().equals(to.getWorld())) {

            // Override Spigot default teleport safety
            hms.getServer().getScheduler().runTaskLater(hms, new Runnable() {
                @Override
                public void run() {
                    if (p.getLocation().distance(to) > 1.0d) p.teleport(to);
                }
            }, 1L);
        } else if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT)) {

            Location current = p.getLocation();
            World world = current.getWorld();
            double xValue = current.getX();
            double zValue = current.getZ();

            int yValue = current.getBlockY();
            if (yValue > 255) yValue = world.getHighestBlockYAt((int) xValue, (int) zValue);
            else {
                while (world.getBlockAt((int) xValue, yValue, (int) zValue).getType().equals(Material.AIR)
                        && yValue > 0) yValue--;
            }

            e.setCancelled(true);
            final Location newLoc = new Location(world, xValue, yValue + 1, zValue, current.getYaw(), current.getPitch());

            if (!waitingForChorusTP.contains(p)) {

                waitingForChorusTP.add(p);
                hms.getServer().getScheduler().runTaskLater(hms, new Runnable() {
                    @Override
                    public void run() {
                        p.setFallDistance(0.0f);
                        p.teleport(newLoc);
                        waitingForChorusTP.remove(p);
                    }
                }, 1L);
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
