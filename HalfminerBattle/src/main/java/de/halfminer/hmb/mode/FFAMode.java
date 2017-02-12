package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.FFAArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.logging.Level;

/**
 * Implementing free for all games with automatic respawns in kit arenas and killstreaks
 */
@SuppressWarnings("unused")
public class FFAMode extends AbstractMode {

    private int removeAfterDeaths;
    private int removeForMinutes;

    public FFAMode() {
        super(BattleModeType.FFA);
    }

    public int getRemoveAfterDeaths() {
        return removeAfterDeaths;
    }

    public int getRemoveForMinutes() {
        return removeForMinutes;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            MessageBuilder.create(hmb, "notAPlayer", HalfminerBattle.PREFIX).sendMessage(sender);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            MessageBuilder.create(hmb, "modeFFAUsage", HalfminerBattle.PREFIX).sendMessage(sender);
            return true;
        }

        if (!sender.hasPermission("hmb.mode.ffa.use")) {
            MessageBuilder.create(hmb, "noPermission", HalfminerBattle.PREFIX).sendMessage(sender);
            return true;
        }

        if (pm.hasQueueCooldown(player)) {
            MessageBuilder.create(hmb, "modeGlobalQueueCooldown", HalfminerBattle.PREFIX).sendMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                // recheck because ffa does not cause command blockage
                if (pm.isNotIdle(player)) {
                    MessageBuilder.create(hmb, "modeGlobalNotIdle", HalfminerBattle.PREFIX).sendMessage(player);
                    return true;
                }

                List<Arena> freeArenas = am.getFreeArenasFromType(type);
                if (freeArenas.size() == 0) {
                    MessageBuilder.create(hmb, "modeGlobalBattleModeDisabled", HalfminerBattle.PREFIX).sendMessage(sender);
                } else if (freeArenas.size() == 1) {
                    ((FFAArena) freeArenas.get(0)).addPlayer(player);
                } else {
                    MessageBuilder.create(hmb, "modeFFAChooseArena", HalfminerBattle.PREFIX).sendMessage(player);
                    am.sendArenaSelection(player, freeArenas, "/ffa choose ", "");
                }
                break;
            case "leave":
                if (!pm.isInBattle(type, player)) {
                    MessageBuilder.create(hmb, "modeFFANotInArena", HalfminerBattle.PREFIX).sendMessage(player);
                    return true;
                }

                ((FFAArena) pm.getArena(player)).removePlayer(player);
                MessageBuilder.create(hmb, "modeFFAArenaLeft", HalfminerBattle.PREFIX).sendMessage(player);
                break;
            case "choose":
                if (args.length > 1) {
                    if (pm.isNotIdle(player)) {
                        MessageBuilder.create(hmb, "modeGlobalNotIdle", HalfminerBattle.PREFIX).sendMessage(player);
                        return true;
                    }

                    Arena selected = am.getArena(type, args[1]);
                    if (selected != null) {
                        ((FFAArena) selected).addPlayer(player);
                        break;
                    }
                }
            default:
                MessageBuilder.create(hmb, "modeFFAUsage", HalfminerBattle.PREFIX).sendMessage(sender);
        }

        return true;
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public void onPluginDisable() {
        hmb.getServer().getOnlinePlayers()
                .stream()
                .filter(p -> pm.isInBattle(type, p))
                .forEach(p -> ((FFAArena) pm.getArena(p)).removePlayer(p));
    }

    @Override
    public void onConfigReload() {
        removeAfterDeaths = hmb.getConfig().getInt("battleMode.ffa.removeAfterDeaths", 4);
        removeForMinutes = hmb.getConfig().getInt("battleMode.ffa.removeForMinutes", 3);
    }

    @EventHandler
    public void onDeathRespawn(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (pm.isInBattle(type, p)) {
            FFAArena arena = (FFAArena) pm.getArena(p);
            arena.hasDied(p);
        }
    }

    @EventHandler
    public void onQuitKillAndRemove(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (pm.isInBattle(type, p)) {
            FFAArena arena = (FFAArena) pm.getArena(p);
            arena.removePlayer(p);
            if (!p.isDead()) p.setHealth(0.0d);
            MessageBuilder.create(hmb, "modeFFALoggedOutLog")
                    .addPlaceholderReplace("%PLAYER%", p.getName())
                    .addPlaceholderReplace("%ARENA%", arena.getName())
                    .logMessage(Level.INFO);
        }
    }
}
