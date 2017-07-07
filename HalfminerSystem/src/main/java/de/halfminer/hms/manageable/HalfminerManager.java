package de.halfminer.hms.manageable;

import de.halfminer.hms.HalfminerClass;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Central hub for all {@link Manageable} classes, such as {@link HalfminerClass}, to be registered for reloads,
 * sweeps, plugin disables and as event listeners
 */
public class HalfminerManager implements Listener {

    private final Plugin basePlugin;

    private final Set<Plugin> managedPlugins = new HashSet<>();
    private final Map<Plugin, List<Disableable>> toDisable = new HashMap<>();
    private final Map<Plugin, List<Reloadable>> toReload = new HashMap<>();
    private final Map<Plugin, List<Sweepable>> toSweep = new HashMap<>();

    private BukkitTask sweepTask;

    public HalfminerManager(Plugin pluginOwner) {
        this.basePlugin = pluginOwner;
        managedPlugins.add(basePlugin);
        basePlugin.getServer().getPluginManager().registerEvents(this, basePlugin);
    }

    public void registerClass(Manageable toAdd) {

        Plugin plugin = toAdd.getPlugin();
        managedPlugins.add(plugin);
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

            checkSweepTask();
        }

        if (toAdd instanceof Listener) {
            plugin.getServer().getPluginManager().registerEvents((Listener) toAdd, plugin);
        }
    }

    public void unregisterClass(Manageable toUnregister) {
        Plugin plugin = toUnregister.getPlugin();

        if (toUnregister instanceof Disableable && toDisable.containsKey(plugin)) {
            toDisable.get(plugin).remove(toUnregister);
        }

        if (toUnregister instanceof Reloadable && toReload.containsKey(plugin)) {
            toReload.get(plugin).remove(toUnregister);
        }

        if (toUnregister instanceof Sweepable && toSweep.containsKey(plugin)) {
            toSweep.get(plugin).remove(toUnregister);
            checkSweepTask();
        }

        if (toUnregister instanceof Listener) {
            HandlerList.unregisterAll((Listener) toUnregister);
        }
    }

    @EventHandler
    public void pluginDisabled(PluginDisableEvent e) {

        Plugin pluginToDisable = e.getPlugin();
        if (!managedPlugins.contains(pluginToDisable)) {
            return;
        }

        if (toDisable.containsKey(pluginToDisable)) {
            List<Disableable> list = toDisable.get(pluginToDisable);
            for (int i = list.size() - 1; i >= 0; i--) {
                list.get(i).onDisable();
            }
            toDisable.remove(pluginToDisable);
        }

        toReload.remove(pluginToDisable);
        toSweep.remove(pluginToDisable);
        checkSweepTask();

        managedPlugins.remove(pluginToDisable);
        pluginToDisable.getServer().getScheduler().cancelTasks(pluginToDisable);
        pluginToDisable.getLogger().info(pluginToDisable.getName() + " disabled");
    }

    public void reloadOcurred(Plugin pluginToReload) {

        if (!managedPlugins.contains(pluginToReload)) {
            managedPlugins.add(pluginToReload);
        }

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

    private void checkSweepTask() {
        if (sweepTask != null && toSweep.size() == 0) {
            sweepTask.cancel();
            sweepTask = null;
        } else if (sweepTask == null && toSweep.size() > 0) {
            sweepTask = basePlugin.getServer().getScheduler().runTaskTimer(this.basePlugin, () -> {
                for (List<Sweepable> sweepables : toSweep.values()) {
                    sweepables.forEach(Sweepable::sweep);
                }
            }, 12000L, 12000L);
        }
    }

    public Set<Plugin> getManagedPlugins() {
        return managedPlugins;
    }
}
