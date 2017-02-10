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

/**
 * TODO
 */
@SuppressWarnings("unused")
public class FFAMode extends AbstractMode {
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
                List<Arena> arenas = am.getArenasFromType(GameModeType.FFA);
                if (arenas.size() == 0) {
                    //TODO show disabled status
                } else if (arenas.size() == 1) {
                    //TODO add to first
                } else {
                    //TODO showselection
                }
                break;
            case "leave":
                if (!pm.isInBattle(GameModeType.FFA, player)) {
                    //TODO show error
                    return true;
                }
                //TODO messages
                ((FFAArena) pm.getArena(player)).removePlayer(player);
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
                .filter(p -> pm.isInBattle(GameModeType.FFA, p))
                .forEach(p -> ((FFAArena) pm.getArena(p)).removePlayer(p));
    }

    @Override
    public void onConfigReload() {
        //TODO action list to pass to arenas on reload for kill rewards
    }

    @EventHandler
    public void onDeathRespawn(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (pm.isInBattle(GameModeType.FFA, p)) {
            FFAArena arena = (FFAArena) pm.getArena(p);
            arena.hasDied(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCancelledCommand(PlayerCommandPreprocessEvent e) {
        if (e.isCancelled() && pm.isInBattle(GameModeType.FFA, e.getPlayer())) {
            e.setCancelled(e.getMessage().toLowerCase().startsWith("/ffa leave"));
        }
    }

    @EventHandler
    public void onQuitKillAndRemove(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (pm.isInBattle(GameModeType.FFA, p)) {
            ((FFAArena) pm.getArena(p)).removePlayer(p);
            if (!p.isDead()) p.setHealth(0.0d);
            //TODO logging
        }
    }
}
