package de.halfminer.hms;

import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.interfaces.Manageable;
import de.halfminer.hms.interfaces.Reloadable;
import de.halfminer.hms.interfaces.Sweepable;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central hub for all {@link Manageable} classes, such as {@link HalfminerClass}, to be registered for reloads,
 * sweeps, plugin disables and as event listeners
 */
public class HalfminerManager {

    private final Map<Plugin, List<Disableable>> toDisable = new HashMap<>();
    private final Map<Plugin, List<Reloadable>> toReload = new HashMap<>();
    private final Map<Plugin, List<Sweepable>> toSweep = new HashMap<>();

    private BukkitTask sweepTask;

    public void registerClass(Manageable toAdd) {

        Plugin plugin = toAdd.getPlugin();
        if (toAdd instanceof Disableable) {
            if (toDisable.containsKey(plugin)) {
                toDisable.get(plugin).add((Disableable) toAdd);
            } else {
                List<Disableable> list = new ArrayList<>();
                list.add((Disableable) toAdd);
                toDisable.put(plugin, list);
            }
        }

        if (toAdd instanceof Reloadable) {
            Reloadable reloadable = (Reloadable) toAdd;
            reloadable.loadConfig();
            if (toReload.containsKey(plugin)) {
                toReload.get(plugin).add(reloadable);
            } else {
                List<Reloadable> list = new ArrayList<>();
                list.add(reloadable);
                toReload.put(plugin, list);
            }
        }

        if (toAdd instanceof Sweepable) {
            if (toSweep.containsKey(plugin)) {
                toSweep.get(plugin).add((Sweepable) toAdd);
            } else {
                List<Sweepable> list = new ArrayList<>();
                list.add((Sweepable) toAdd);
                toSweep.put(plugin, list);
            }

            if (sweepTask == null) {
                HalfminerSystem hms = HalfminerSystem.getInstance();
                sweepTask = hms.getServer().getScheduler().runTaskTimer(hms, () -> {
                    for (List<Sweepable> sweepables : toSweep.values()) {
                        sweepables.forEach(Sweepable::sweep);
                    }
                }, 12000L, 12000L);
            }
        }

        if (toAdd instanceof Listener) {
            plugin.getServer().getPluginManager().registerEvents((Listener) toAdd, plugin);
        }
    }

    public void pluginDisabled(Plugin pluginToDisable) {

        if (toDisable.containsKey(pluginToDisable)) {
            List<Disableable> list = toDisable.get(pluginToDisable);
            for (int i = list.size() - 1; i >= 0; i--) {
                list.get(i).onDisable();
            }
            toDisable.remove(pluginToDisable);
        }

        toReload.remove(pluginToDisable);
        toSweep.remove(pluginToDisable);
        if (sweepTask != null && toSweep.size() == 0) {
            sweepTask.cancel();
            sweepTask = null;
        }

        pluginToDisable.getServer().getScheduler().cancelTasks(pluginToDisable);
    }

    public void reloadOcurred(Plugin pluginToReload) {

        // Save default config.yml if not yet done
        pluginToReload.saveDefaultConfig();
        // Make sure that if the file changed, it is reread
        pluginToReload.reloadConfig();
        // If parameters are missing, add them (reload again afterwards)
        pluginToReload.getConfig().options().copyDefaults(true);
        // Save config.yml to disk
        pluginToReload.saveConfig();
        pluginToReload.reloadConfig();
        if (toReload.containsKey(pluginToReload)) {
            toReload.get(pluginToReload).forEach(Reloadable::loadConfig);
        }
    }
}
