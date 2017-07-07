package de.halfminer.hmc;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.HalfminerModule;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
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
            for (ModuleType module : ModuleType.values()) {
                HalfminerModule mod = (HalfminerModule) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".module." + module.getClassName()).newInstance();

                modules.put(module, mod);
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error has occurred, see stacktrace for information", e);
            setEnabled(false);
            return;
        }

        HalfminerSystem.getInstance().getHalfminerManager().reloadOcurred(this);
        getLogger().info("HalfminerCore enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equals("?")) return true;

        HalfminerCommand command;
        try {
            command = (HalfminerCommand) this.getClassLoader()
                    .loadClass(PACKAGE_PATH + ".cmd.Cmd" + cmd.getName()).newInstance();
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

    public HalfminerModule getModule(ModuleType type) {
        if (modules.size() != ModuleType.values().length)
            throw new RuntimeException("Illegal call to getModule before all modules were initialized");
        return modules.get(type);
    }
}
