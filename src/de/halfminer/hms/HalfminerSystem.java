package de.halfminer.hms;

import de.halfminer.hms.cmd.BaseCommand;
import de.halfminer.hms.modules.*;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.ModuleType;
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

    private static HalfminerSystem instance;
    private HalfminerStorage storage;
    private Map<ModuleType, HalfminerModule> modules;

    public static HalfminerSystem getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {

        instance = this;
        loadConfig();

        modules = new HashMap<>();
        modules.put(ModuleType.AUTO_MESSAGE, new ModAutoMessage());
        modules.put(ModuleType.ANTI_KILLFARMING, new ModAntiKillfarming());
        modules.put(ModuleType.BEDROCK_PROTECTION, new ModBedrockProtection());
        modules.put(ModuleType.MOTD, new ModMotd());
        modules.put(ModuleType.SIGN_EDIT, new ModSignEdit());
        modules.put(ModuleType.REDSTONE_LIMIT, new ModRedstoneLimit());
        modules.put(ModuleType.COMBAT_LOG, new ModCombatLog());
        modules.put(ModuleType.TPS, new ModTps());
        modules.put(ModuleType.STATS, new ModStats());
        modules.put(ModuleType.SKILL_LEVEL, new ModSkillLevel());
        modules.put(ModuleType.STATIC_LISTENERS, new ModStaticListeners());

        for (HalfminerModule mod : modules.values())
            if (mod instanceof Listener) getServer().getPluginManager().registerEvents((Listener) mod, this);

        getLogger().info("HalfminerSystem enabled");

    }

    @Override
    public void onDisable() {
        for (HalfminerModule mod : modules.values()) mod.onDisable();
        storage.saveConfig();
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("HalfminerSystem disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equals("?")) return true;
        BaseCommand command;
        try {
            command = (BaseCommand) this.getClassLoader().loadClass("de.halfminer.hms.cmd.Cmd" + cmd.getName()).newInstance();
        } catch (Exception e) {
            getLogger().warning("An error has occured executing " + cmd.getName() + ":");
            e.printStackTrace();
            return true;
        }

        if (command.hasPermission(sender)) {
            command.run(sender, label, args);
        } else sender.sendMessage(Language.getMessagePlaceholderReplace("noPermission", true, "%PREFIX%", "Info"));
        return true;
    }

    //Module getters
    public HalfminerStorage getStorage() {
        return storage;
    }

    public HalfminerModule getModule(ModuleType type) {
        return modules.get(type);
    }

    public void loadConfig() {
        saveDefaultConfig(); //Save default config.yml if not yet done
        reloadConfig(); //Make sure that if the file changed, it is reread
        getConfig().options().copyDefaults(true); //if parameters are missing, add them
        saveConfig(); //save config.yml to disk

        //Load storage
        if (storage != null) storage.reloadConfig();
        else storage = new HalfminerStorage();

        //Reload modules
        if (modules != null) for (HalfminerModule mod : modules.values()) mod.reloadConfig();
    }

}
