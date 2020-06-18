package de.halfminer.hmc;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.HalfminerModule;
import de.halfminer.hmc.module.ModuleDisabledException;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * HalfminerCore main class, core Bukkit/Spigot plugin containing most server side functionality for Halfminer
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerCore extends JavaPlugin {

    private static final String PACKAGE_PATH = "de.halfminer.hmc";
    private static HalfminerCore instance;

    static HalfminerCore getInstance() {
        return instance;
    }

    private HanStorage storage;
    private final Map<ModuleType, HalfminerModule> modules = new HashMap<>();


    @Override
    public void onEnable() {

        instance = this;

        storage = new HanStorage(this);
        try {

            List<String> disabledModules = getConfig().getStringList("disabledModules");

            outerLoop:
            for (ModuleType module : ModuleType.values()) {
                for (String disabledModule : disabledModules) {
                    if (module.getClassName().toLowerCase().endsWith(disabledModule.toLowerCase())) {
                        getLogger().info("The module " + module.getClassName() + " has been disabled");
                        continue outerLoop;
                    }
                }

                HalfminerModule mod = (HalfminerModule) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".module." + module.getClassName())
                        .getDeclaredConstructor()
                        .newInstance();

                modules.put(module, mod);
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error has occurred, see stacktrace for information", e);
            setEnabled(false);
            return;
        }

        HalfminerSystem.getInstance().getHalfminerManager().reload(this);
        getLogger().info("HalfminerCore enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equals("?")) return true;

        HalfminerCommand command;
        try {
            command = (HalfminerCommand) this.getClassLoader()
                    .loadClass(PACKAGE_PATH + ".cmd.Cmd" + cmd.getName())
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error has occured executing " + cmd.getName(), e);
            return true;
        }

        if (command.hasPermission(sender)) {
            command.run(sender, label, args);
        } else MessageBuilder.create("noPermission", "Info").sendMessage(sender);

        return true;
    }

    public HanStorage getStorage() {
        return storage;
    }

    public HalfminerModule getModule(ModuleType type) throws ModuleDisabledException {
        if (modules.containsKey(type)) {
            return modules.get(type);
        } else {
            throw new ModuleDisabledException(type);
        }
    }

    public boolean isModuleEnabled(ModuleType type) {
        return modules.containsKey(type);
    }
}
