package de.halfminer.hmb;

import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.abs.GameMode;
import de.halfminer.hmb.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HalfminerBattle main class
 *
 * @author Fabian Prieto Wunderlich / Kakifrucht
 */
public class HalfminerBattle extends JavaPlugin {

    public final static String PACKAGE_PATH = "de.halfminer.hmb";
    private static HalfminerBattle instance;

    public static HalfminerBattle getInstance() {
        return instance;
    }

    private final PlayerManager playerManager = new PlayerManager();
    private ArenaManager arenaManager;

    // Modes
    private final Map<GameModeType, GameMode> gameModes = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveAndReloadConfig();

        try {
            arenaManager = new ArenaManager();
        } catch (IOException e) {
            setDisabledAfterException(e);
            return;
        }

        try {
            for (GameModeType type : GameModeType.values()) {
                GameMode mode = (GameMode) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".mode." + type.getModeClassName()).newInstance();

                mode.onConfigReload();
                gameModes.put(type, mode);
            }
        } catch (Exception e) {
            setDisabledAfterException(e);
            return;
        }

        gameModes.values().forEach(mode -> getServer().getPluginManager().registerEvents(mode, this));
        getLogger().info("HalfminerBattle enabled");
    }

    private void saveAndReloadConfig() {
        saveDefaultConfig(); // Save default config.yml if not yet done
        reloadConfig(); // Make sure that if the file changed, it is reread
        getConfig().options().copyDefaults(true); // If parameters are missing, add them
        saveConfig(); // Save config.yml to disk
    }

    private void setDisabledAfterException(Exception e) {
        getLogger().severe("An error while enabling HalfminerBattle occurred, see stacktrace for information");
        e.printStackTrace();
        setEnabled(false);
    }

    @Override
    public void onDisable() {
        gameModes.values().forEach(GameMode::onPluginDisable);

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("HalfminerBattle disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // admin commands
        if (cmd.getName().equalsIgnoreCase("hmb")
                && sender.hasPermission("hmb.admin")) {

            if (args.length > 0) {

                if (args[0].equalsIgnoreCase("reload")) {
                    saveAndReloadConfig();
                    gameModes.values().forEach(GameMode::onConfigReload);
                    try {
                        arenaManager.reloadConfig();
                        Util.sendMessage(sender, "adminSettingsReloaded");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Util.sendMessage(sender, "adminSettingsReloadedError");
                    }
                    return true;
                }

                GameMode called = getGameMode(args[0]);
                if (called != null)
                    return called.onAdminCommand(sender, args);
            }

            Util.sendMessage(sender, "adminHelp");
            return false;
        }

        return getGameMode(cmd.getName()).onCommand(sender, args);
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameMode getGameMode(GameModeType type) {
        return gameModes.get(type);
    }

    private GameMode getGameMode(String toResolve) {
        return gameModes.get(GameModeType.getGameMode(toResolve));
    }
}
