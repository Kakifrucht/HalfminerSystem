package de.halfminer.hms;

import de.halfminer.hms.cmd.HalfminerCommand;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.handlers.HalfminerHandler;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.interfaces.Reloadable;
import de.halfminer.hms.modules.HalfminerModule;
import de.halfminer.hms.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * HalfminerSystem Main class
 *
 * @author Fabian Prieto Wunderlich / Kakifrucht
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

                if (han instanceof Reloadable) ((Reloadable) han).reloadConfig();
                handlers.put(handler, han);
            }
            for (ModuleType module : ModuleType.values()) {
                HalfminerModule mod = (HalfminerModule) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".modules." + module.getClassName()).newInstance();

                mod.reloadConfig();
                modules.put(module, mod);
            }
        } catch (Exception e) {
            getLogger().severe("An error has occurred, see stacktrace for information");
            e.printStackTrace();
            setEnabled(false);
            return;
        }

        // Register listeners
        for (HalfminerModule mod : modules.values())
            if (mod instanceof Listener) getServer().getPluginManager().registerEvents((Listener) mod, this);

        getLogger().info("HalfminerSystem enabled");
    }

    @Override
    public void onDisable() {

        for (HalfminerModule mod : modules.values()) if (mod instanceof Disableable) ((Disableable) mod).onDisable();
        for (HalfminerHandler han : handlers.values()) if (han instanceof Disableable) ((Disableable) han).onDisable();
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
        } else sender.sendMessage(Language.getMessagePlaceholders("noPermission", true, "%PREFIX%", "Info"));

        return true;
    }

    public HalfminerHandler getHandler(HandlerType type) {
        return handlers.get(type);
    }

    public HalfminerModule getModule(ModuleType type) {
        return modules.get(type);
    }

    public void loadConfig() {

        saveDefaultConfig(); // Save default config.yml if not yet done
        reloadConfig(); // Make sure that if the file changed, it is reread
        getConfig().options().copyDefaults(true); // If parameters are missing, add them
        saveConfig(); // Save config.yml to disk

        //noinspection ConstantConditions (suppress warning due to java refelction)
        for (HalfminerHandler han : handlers.values())
                if (han instanceof Reloadable) ((Reloadable) han).reloadConfig();
        for (HalfminerModule mod : modules.values()) mod.reloadConfig();
    }

}
