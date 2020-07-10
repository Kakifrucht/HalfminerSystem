package de.halfminer.hms;

import de.halfminer.hms.handler.*;
import de.halfminer.hms.manageable.HalfminerManager;
import de.halfminer.hms.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * HalfminerSystem main class, base API for Halfminer Bukkit/Spigot plugins
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerSystem extends JavaPlugin {

    private static HalfminerSystem instance;

    public static HalfminerSystem getInstance() {
        return instance;
    }

    private HalfminerManager manager;
    private final Map<HandlerType, HalfminerClass> handlers = new HashMap<>();

    @Override
    public void onEnable() {

        instance = this;
        manager = new HalfminerManager(this);

        try {
            // Load handlers
            for (HandlerType handler : HandlerType.values()) {
                handlers.put(handler, (HalfminerClass) this.getClassLoader()
                        .loadClass("de.halfminer.hms.handler." + handler.getClassName())
                        .getDeclaredConstructor()
                        .newInstance());
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error has occurred, see stacktrace for information", e);
            setEnabled(false);
            return;
        }

        manager.reload(this);
        getLogger().info("HalfminerSystem enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // empty command, useful for command overwriting through commands.yml bukkit file
        if (command.getName().equalsIgnoreCase("alias")) return true;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {

            boolean reloadAll = args.length > 1 && args[1].equalsIgnoreCase("-a");
            for (Plugin plugin : manager.getManagedPlugins()) {

                if (!reloadAll && !plugin.equals(this)) {
                    continue;
                }

                manager.reload(plugin);
                Message.create("pluginReloaded", "HMS")
                        .addPlaceholder("%PLUGINNAME%", plugin.getName())
                        .send(sender);
            }
            return true;
        }

        Message.create("cmdHmsSystem", "HMS")
                .addPlaceholder("%VERSION%", getDescription().getVersion())
                .send(sender);

        Set<Plugin> managedPlugins = manager.getManagedPlugins();
        if (managedPlugins.size() <= 1) {
            Message.create("cmdHmsNoneHooked").send(sender);
        } else {
            for (Plugin plugin : managedPlugins) {
                if (plugin == this) continue;

                final String pluginPrefix = "Halfminer";
                String pluginName = plugin.getName();
                if (pluginName.startsWith(pluginPrefix)) {
                    pluginName = pluginName.substring(pluginPrefix.length());
                }

                Message.create("cmdHmsHooked")
                        .addPlaceholder("%NAME%", pluginName)
                        .addPlaceholder("%VERSION%", plugin.getDescription().getVersion())
                        .send(sender);
            }

            if (getServer().getPluginCommand("hmc") != null) {
                Message.create("cmdHmsCoreCommand").send(sender);
            }
        }

        return true;
    }

    public HalfminerManager getHalfminerManager() {
        return manager;
    }

    public HanBossBar getBarHandler() {
        return (HanBossBar) handlers.get(HandlerType.BOSS_BAR);
    }

    public HanHooks getHooksHandler() {
        return (HanHooks) handlers.get(HandlerType.HOOKS);
    }

    public HanMenu getMenuHandler() {
        return (HanMenu) handlers.get(HandlerType.MENU);
    }

    public HanStorage getStorageHandler() {
        return (HanStorage) handlers.get(HandlerType.STORAGE);
    }

    public HanTeleport getTeleportHandler() {
        return (HanTeleport) handlers.get(HandlerType.TELEPORT);
    }

    public HanTitles getTitlesHandler() {
        return (HanTitles) handlers.get(HandlerType.TITLES);
    }
}
