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

    public static HalfminerSystem getInstance() {
        return instance;
    }

    private HalfminerStorage storage;
    private final Map<ModuleType, HalfminerModule> modules = new HashMap<>();

    @Override
    public void onEnable() {

        instance = this;
        loadConfig();

        // load modules
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
        modules.put(ModuleType.TITLES, new ModTitles());
        modules.put(ModuleType.PVP, new ModPvP());

        // Register modules
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
            getLogger().severe("An error has occured executing " + cmd.getName() + ":");
            e.printStackTrace();
            return true;
        }

        if (command.hasPermission(sender)) {
            command.run(sender, label, args);
        } else sender.sendMessage(Language.getMessagePlaceholders("noPermission", true, "%PREFIX%", "Info"));
        return true;
    }

    public HalfminerStorage getStorage() {
        return storage;
    }

    public HalfminerModule getModule(ModuleType type) {
        return modules.get(type);
    }

    public void loadConfig() {

        saveDefaultConfig(); // Save default config.yml if not yet done
        reloadConfig(); // Make sure that if the file changed, it is reread
        getConfig().options().copyDefaults(true); // If parameters are missing, add them
        saveConfig(); // Save config.yml to disk

        // Load storage
        if (storage != null) storage.load();
        else storage = new HalfminerStorage();

        //noinspection ConstantConditions (Cmdhms accesses this method through reflection, so the IDE does not detect the sense of the statement)
        if (modules != null) for (HalfminerModule mod : modules.values()) mod.reloadConfig();
    }

}
