package de.halfminer.hms;

import de.halfminer.hms.cmd.BaseCommand;
import de.halfminer.hms.modules.*;
import de.halfminer.hms.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


/**
 * HalfminerSystem Main class
 *
 * @author Fabian Prieto Wunderlich / Kakifrucht
 */
public class HalfminerSystem extends JavaPlugin {

    private static HalfminerSystem instance;
    private HalfminerModule[] modules;

    public static HalfminerSystem getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {

        instance = this;
        loadConfig();

        modules = new HalfminerModule[]{
                new ModStandardFunctions(),
                new ModAutoMessage(),
                new ModAntiKillfarming(),
                new ModBedrockProtection(),
                new ModMOTD(),
                new ModSignEdit(),
                new ModRedstoneLimit(),
                new ModCombatLog()
        };

        for (HalfminerModule mod : modules)
            if (mod instanceof Listener) getServer().getPluginManager().registerEvents((Listener) mod, this);

        getLogger().info("HalfminerSystem enabled");

    }

    @Override
    public void onDisable() {
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

        if (command.hasPermission(sender) || sender.isOp()) {
            command.run(sender, cmd, label, args);
        } else sender.sendMessage(Language.getMessagePlaceholderReplace("noPermission", true, "%PREFIX%", "Hinweis"));
        return true;
    }

    //Module getters
    public ModAntiKillfarming getAntiKillfarming() {
        return (ModAntiKillfarming) modules[2];
    }

    public ModMOTD getMotd() {
        return (ModMOTD) modules[4];
    }

    public ModSignEdit getSignEdit() {
        return (ModSignEdit) modules[5];
    }

    public void loadConfig() {
        saveDefaultConfig(); //Save default config.yml if not yet done
        reloadConfig(); //Make sure that if the file changed, it is reread
        getConfig().options().copyDefaults(true); //if parameters are missing, add them
        saveConfig(); //save config.yml to disk

        if (modules != null) for (HalfminerModule mod : modules) mod.reloadConfig();
    }

}
