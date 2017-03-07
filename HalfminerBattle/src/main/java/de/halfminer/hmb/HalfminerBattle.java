package de.halfminer.hmb;

import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.mode.abs.BattleMode;
import de.halfminer.hms.HalfminerManager;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handlers.HanStorage;
import de.halfminer.hms.interfaces.CacheHolder;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

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
        cacheHolder = new HanStorage(this);
        playerManager = new PlayerManager();
        arenaManager = new ArenaManager();

        try {
            for (BattleModeType type : BattleModeType.values()) {
                BattleMode mode = (BattleMode) this.getClassLoader()
                        .loadClass(PACKAGE_PATH + ".mode." + type.getModeClassName())
                        .newInstance();
                battleModes.put(type, mode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setDisabledAfterException();
            return;
        }

        if (!saveAndReloadConfig()) {
            setDisabledAfterException();
            return;
        }

        getLogger().info("HalfminerBattle enabled");
    }

    public boolean saveAndReloadConfig() {

        manager.reloadOcurred(this);

        try {
            arenaManager.reloadConfig();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setDisabledAfterException() {
        getLogger().severe("An error occurred while enabling HalfminerBattle, see stacktrace for information");
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

            if (!sender.hasPermission("hmb.admin")) {
                MessageBuilder.create("noPermission", "Battle").sendMessage(sender);
                return true;
            }

            if (args.length > 0) {
                BattleMode called = getBattleMode(args[0]);
                if (called != null) {
                    if (!called.onAdminCommand(sender, args)) {
                        MessageBuilder.create("adminNotDefined", this)
                                .addPlaceholderReplace("%BATTLEMODE%", args[0])
                                .sendMessage(sender);
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
            MessageBuilder.create("modeGlobalCommandsInBedDisabled", this).sendMessage(sender);
            return true;
        }

        BattleMode calledMode = getBattleMode(cmd.getName());
        if (arenaManager.getArenasFromType(calledMode.getType()).size() == 0) {
            MessageBuilder.create("modeGlobalBattleModeDisabled", this).sendMessage(sender);
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
