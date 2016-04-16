package de.halfminer.hms.modules;

import de.halfminer.hms.interfaces.Sweepable;
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
public class ModGlitchProtection extends HalfminerModule implements Listener, Sweepable {

    private final Set<Player> waitingForChorusTP = new HashSet<>();
    private Map<Player, Long> lastGlitchAlert = new HashMap<>();

    private BukkitTask checkIfOverNether;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMoveBedrockObsidianCheck(PlayerMoveEvent e) {

        if ((e.getFrom().getBlock().getType() == Material.BEDROCK || e.getFrom().getBlock().getType() == Material.OBSIDIAN)
                && !e.getPlayer().hasPermission("hms.bypass.bedrockcheck")
                && Math.round(e.getFrom().getY()) == e.getFrom().getBlockY()) {

            if (lastGlitchAlert.get(e.getPlayer()) == null || lastGlitchAlert.get(e.getPlayer()) < System.currentTimeMillis() / 1000) {

                server.broadcast(Language.getMessagePlaceholders("modGlitchProtectionBedrock", true,
                        "%PREFIX%", "Warnung",
                        "%PLAYER%", e.getPlayer().getName(),
                        "%LOCATION%", Language.getStringFromLocation(e.getTo()),
                        "%WORLD%", e.getTo().getWorld().getName()), "hms.bypass.bedrockcheck");
                lastGlitchAlert.put(e.getPlayer(), (System.currentTimeMillis() / 1000) + 4);
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
            scheduler.runTaskLater(hms, new Runnable() {
                @Override
                public void run() {
                    if (p.getLocation().distance(to) > 1.0d) p.teleport(to);
                }
            }, 1L);
        } else if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT)) {

            Location current = p.getLocation();
            World world = current.getWorld();
            int xValue = current.getBlockX();
            int zValue = current.getBlockZ();

            int yValue = current.getBlockY();
            if (yValue > 255) yValue = world.getHighestBlockYAt(xValue, zValue);
            else {
                while (world.getBlockAt(xValue, yValue, zValue).getType().equals(Material.AIR)
                        && yValue > 0) yValue--;
            }

            e.setCancelled(true);
            final Location newLoc = new Location(world, current.getX(), yValue + 1, current.getZ()
                    , current.getYaw(), current.getPitch());
            if (current.distance(newLoc) < 2.0d) return;

            if (!waitingForChorusTP.contains(p)) {

                waitingForChorusTP.add(p);
                scheduler.runTaskLater(hms, new Runnable() {
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
    public void sweep() {
        lastGlitchAlert = new HashMap<>();
    }

    @Override
    public void loadConfig() {

        if (checkIfOverNether == null) {

            checkIfOverNether = scheduler.runTaskTimer(hms, new Runnable() {
                @Override
                public void run() {
                    for (Player p : server.getOnlinePlayers()) {
                        Location loc = p.getLocation();
                        if (!p.hasPermission("hms.bypass.nethercheck")
                                && !p.isDead()
                                && loc.getWorld().getEnvironment().equals(World.Environment.NETHER)
                                && loc.getBlockY() > 127) {
                            p.setHealth(0.0d);
                            p.sendMessage(Language.getMessagePlaceholders("modGlitchProtectionNether", true,
                                    "%PREFIX%", "Warnung"));
                            server.broadcast(Language.getMessagePlaceholders("modGlitchProtectionNetherNotify",
                                    true, "%PREFIX%", "Warnung", "%PLAYER%", p.getName(),
                                    "%LOCATION%", Language.getStringFromLocation(loc)), "hms.bypass.nethercheck");
                        }
                    }
                }
            }, 100L, 100L);
        }
    }
}
