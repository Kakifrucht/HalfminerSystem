package de.halfminer.hms;

import de.halfminer.hms.cmd.BaseCommand;
import de.halfminer.hms.modules.*;
import de.halfminer.hms.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;


/**
 * HalfminerSystem Main class
 *
 * @author Fabian Prieto Wunderlich / Kakifrucht
 */
public class HalfminerSystem extends JavaPlugin {

    private static HalfminerSystem instance;
    private HalfminerStorage storage;
    private List<HalfminerModule> modules;

    public static HalfminerSystem getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {

        instance = this;
        loadConfig();

        modules = new ArrayList<>(11);
        modules.add(new ModAutoMessage());
        modules.add(new ModAntiKillfarming());
        modules.add(new ModBedrockProtection());
        modules.add(new ModMotd());
        modules.add(new ModSignEdit());
        modules.add(new ModRedstoneLimit());
        modules.add(new ModCombatLog());
        modules.add(new ModTps());
        modules.add(new ModStats());
        modules.add(new ModSkillLevel());
        modules.add(new ModStaticListeners());

        for (HalfminerModule mod : modules)
            if (mod instanceof Listener) getServer().getPluginManager().registerEvents((Listener) mod, this);

        getLogger().info("HalfminerSystem enabled");

    }

    @Override
    public void onDisable() {
        for (HalfminerModule mod : modules) mod.onDisable();
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

    public ModAntiKillfarming getModAntiKillfarming() {
        return (ModAntiKillfarming) modules.get(1);
    }

    public ModMotd getModMotd() {
        return (ModMotd) modules.get(3);
    }

    public ModSignEdit getModSignEdit() {
        return (ModSignEdit) modules.get(4);
    }

    public ModTps getModTps() {
        return (ModTps) modules.get(7);
    }

    public ModSkillLevel getModSkillLevel() {
        return (ModSkillLevel) modules.get(9);
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
        if (modules != null) for (HalfminerModule mod : modules) mod.reloadConfig();
    }

}
