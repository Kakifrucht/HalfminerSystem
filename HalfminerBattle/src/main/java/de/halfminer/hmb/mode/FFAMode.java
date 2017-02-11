package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.FFAArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.logging.Level;

/**
 * TODO
 */
@SuppressWarnings("unused")
public class FFAMode extends AbstractMode {

    private int removeAfterDeaths;
    private int removeForMinutes;

    public FFAMode() {
        super(GameModeType.FFA);
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

        switch (args[0].toLowerCase()) {
            case "join":
                List<Arena> freeArenas = am.getFreeArenasFromType(type);
                if (freeArenas.size() == 1) {
                    if (((FFAArena) freeArenas.get(0)).addPlayer(player)) {
                        MessageBuilder.create(hmb, "modeFFAJoined", HalfminerBattle.PREFIX).sendMessage(player);
                    }
                } else {
                    MessageBuilder.create(hmb, "modeFFAChooseArena", HalfminerBattle.PREFIX).sendMessage(player);
                    am.sendArenaSelection(player, freeArenas, "/ffa choose ", "");
                    return true;
                }
                break;
            case "leave":
                if (!pm.isInBattle(type, player)) {
                    MessageBuilder.create(hmb, "modeFFANotInArena", HalfminerBattle.PREFIX).sendMessage(player);
                    return true;
                }
                ((FFAArena) pm.getArena(player)).removePlayer(player);
                MessageBuilder.create(hmb, "modeFFAArenaLeft", HalfminerBattle.PREFIX).sendMessage();
                break;
            case "choose":
                //TODO
                break;
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
        removeAfterDeaths = hmb.getConfig().getInt("mode.ffa.removeAfterDeaths", 4);
        removeForMinutes = hmb.getConfig().getInt("mode.ffa.removeForMinutes", 3);
    }

    @EventHandler
    public void onDeathRespawn(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (pm.isInBattle(type, p)) {
            FFAArena arena = (FFAArena) pm.getArena(p);
            arena.hasDied(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCancelledCommand(PlayerCommandPreprocessEvent e) {
        if (e.isCancelled() && pm.isInBattle(type, e.getPlayer())) {
            e.setCancelled(e.getMessage().toLowerCase().startsWith("/ffa leave"));
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
