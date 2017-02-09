package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.FFAArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Created by fabpw on 08.02.2017.
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

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (pm.isInBattle(GameModeType.FFA, p)) {
            FFAArena arena = (FFAArena) pm.getArena(p);
        }
    }
}
