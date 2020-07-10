package de.halfminer.hmb;

import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.mode.abs.BattleModeType;
import de.halfminer.hmb.mode.abs.BattleMode;
import de.halfminer.hms.manageable.HalfminerManager;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.cache.CacheHolder;
import de.halfminer.hms.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * HalfminerBattle main class, battle arena Bukkit/Spigot plugin implementing various arena based game modes
 *
 * @author Fabian Prieto Wunderlich / Kakifrucht
 */
public class HalfminerBattle extends JavaPlugin {

    public final static String PACKAGE_PATH = "de.halfminer.hmb";
    private static HalfminerBattle instance;

    public static HalfminerBattle getInstance() {
        return instance;
    }

    private HalfminerManager manager;
    private CacheHolder cacheHolder;
    private PlayerManager playerManager;
    private ArenaManager arenaManager;

    private final Map<BattleModeType, BattleMode> battleModes = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        manager = HalfminerSystem.getInstance().getHalfminerManager();
        cacheHolder = new HanStorage(this, false);
        playerManager = new PlayerManager();
        arenaManager = new ArenaManager();

        try {
            for (BattleModeType type : BattleModeType.values()) {
                BattleMode mode = (BattleMode) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".mode." + type.getModeClassName())
                        .getDeclaredConstructor()
                        .newInstance();
                battleModes.put(type, mode);
            }
        } catch (Exception e) {
            setDisabledAfterException(e);
            return;
        }

        if (!saveAndReloadConfig()) {
            return;
        }

        getLogger().info("HalfminerBattle enabled");
    }

    public boolean saveAndReloadConfig() {

        manager.reload(this);

        try {
            arenaManager.reloadConfig();
            return true;
        } catch (Exception e) {
            setDisabledAfterException(e);
            return false;
        }
    }

    private void setDisabledAfterException(Exception caught) {
        getLogger().log(Level.SEVERE,
                "An error occurred while enabling HalfminerBattle, see stacktrace for information", caught);
        setEnabled(false);
    }

    @Override
    public void onDisable() {
        arenaManager.endAllGames();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // admin commands
        if (cmd.getName().equalsIgnoreCase("hmb")) {

            if (args.length > 0) {
                BattleMode called = getBattleMode(args[0]);
                if (called != null) {
                    if (!called.onAdminCommand(sender, args)) {
                        Message.create("adminNotDefined", this)
                                .addPlaceholder("%BATTLEMODE%", args[0])
                                .send(sender);
                    }
                    return true;
                }
            }

            // by default use admin commands of GlobalMode
            getBattleMode(BattleModeType.GLOBAL).onAdminCommand(sender, args);
            return true;
        }

        // no battleMode specific commands in bed, as teleports are not possible while sleeping
        if (sender instanceof Player && ((Player) sender).isSleeping()) {
            Message.create("modeGlobalCommandsInBedDisabled", this).send(sender);
            return true;
        }

        BattleMode calledMode = getBattleMode(cmd.getName());
        if (arenaManager.getArenasFromType(calledMode.getType()).size() == 0) {
            Message.create("modeGlobalBattleModeDisabled", this).send(sender);
            return true;
        }

        return calledMode.onCommand(sender, args);
    }

    public CacheHolder getCacheHolder() {
        return cacheHolder;
    }

    PlayerManager getPlayerManager() {
        return playerManager;
    }

    ArenaManager getArenaManager() {
        return arenaManager;
    }

    public BattleMode getBattleMode(BattleModeType type) {
        return battleModes.get(type);
    }

    private BattleMode getBattleMode(String toResolve) {
        return battleModes.get(BattleModeType.getBattleMode(toResolve));
    }
}
