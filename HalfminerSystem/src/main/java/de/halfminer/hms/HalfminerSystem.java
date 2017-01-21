package de.halfminer.hms;

import de.halfminer.hms.cmd.abs.HalfminerCommand;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.handlers.HalfminerHandler;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.interfaces.Reloadable;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.modules.HalfminerModule;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * HalfminerSystem main class
 * - Loads handlers and modules
 * - Starts sweep task
 * - Dispatches commands
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerSystem extends JavaPlugin {

    private final static String PACKAGE_PATH = "de.halfminer.hms";
    private static HalfminerSystem instance;

    public static HalfminerSystem getInstance() {
        return instance;
    }

    private final Map<HandlerType, HalfminerHandler> handlers = new HashMap<>();
    private final Map<ModuleType, HalfminerModule> modules = new HashMap<>();

    @Override
    public void onEnable() {

        instance = this;
        loadConfig();

        // Load handlers and modules
        try {

            for (HandlerType handler : HandlerType.values()) {
                HalfminerHandler han = (HalfminerHandler) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".handlers." + handler.getClassName()).newInstance();

                if (han instanceof Reloadable) ((Reloadable) han).loadConfig();
                handlers.put(handler, han);
            }
            for (ModuleType module : ModuleType.values()) {
                HalfminerModule mod = (HalfminerModule) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".modules." + module.getClassName()).newInstance();

                mod.loadConfig();
                modules.put(module, mod);
            }
        } catch (Exception e) {
            getLogger().severe("An error has occurred, see stacktrace for information");
            e.printStackTrace();
            setEnabled(false);
            return;
        }

        // Register listeners
        modules.values().stream()
                .filter(mod -> mod instanceof Listener)
                .forEach(mod -> getServer().getPluginManager().registerEvents((Listener) mod, this));

        // Start sweep task, every 10 minutes
        getServer().getScheduler().runTaskTimer(this,
                () -> modules.values().stream()
                        .filter(mod -> mod instanceof Sweepable)
                        .forEach(mod -> ((Sweepable) mod).sweep()), 12000L, 12000L);

        getLogger().info("HalfminerSystem enabled");
    }

    @Override
    public void onDisable() {

        modules.values().stream()
                .filter(mod -> mod instanceof Disableable).forEach(mod -> ((Disableable) mod).onDisable());

        handlers.values().stream()
                .filter(han -> han instanceof Disableable).forEach(han -> ((Disableable) han).onDisable());

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("HalfminerSystem disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equals("?")) return true;

        HalfminerCommand command;
        try {
            command = (HalfminerCommand) this.getClassLoader()
                    .loadClass(PACKAGE_PATH + ".cmd.Cmd" + cmd.getName()).newInstance();
        } catch (Exception e) {
            getLogger().severe("An error has occured executing " + cmd.getName() + ":");
            e.printStackTrace();
            return true;
        }

        if (command.hasPermission(sender)) {
            command.run(sender, label, args);
        } else MessageBuilder.create(this, "noPermission", "Info").sendMessage(sender);

        return true;
    }

    public HalfminerHandler getHandler(HandlerType type) {
        if (handlers.size() != HandlerType.values().length)
            throw new RuntimeException("Illegal call to getHandler before all handlers were initialized");
        return handlers.get(type);
    }

    public HalfminerModule getModule(ModuleType type) {
        if (modules.size() != ModuleType.values().length)
            throw new RuntimeException("Illegal call to getModule before all modules were initialized");
        return modules.get(type);
    }

    public void loadConfig() {

        saveDefaultConfig(); // Save default config.yml if not yet done
        reloadConfig(); // Make sure that if the file changed, it is reread
        getConfig().options().copyDefaults(true); // If parameters are missing, add them
        saveConfig(); // Save config.yml to disk

        //noinspection ConstantConditions (suppress warning due to java reflection)
        handlers.values().stream().filter(han -> han instanceof Reloadable).forEach(han -> ((Reloadable) han).loadConfig());
        modules.values().forEach(HalfminerModule::loadConfig);
    }
}
