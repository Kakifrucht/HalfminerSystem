package de.halfminer.hmb.mode.abs;

import de.halfminer.hmb.enums.BattleModeType;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;


public interface BattleMode extends Listener {

    /**
     * @return this BattleMode's type
     */
    BattleModeType getType();

    /**
     * Called if battle mode specific command was executed
     *
     * @param sender    sender of  command
     * @param args      given arguments
     * @return true if command is implemented
     */
    boolean onCommand(CommandSender sender, String[] args);

    /**
     * Called if battle mode specific admin command was executed, such as <i>/hmb <mode> (args[])</i>
     *
     * @param sender    sender of  command
     * @param args      given arguments
     * @return true if command is implemented
     */
    boolean onAdminCommand(CommandSender sender, String[] args);

    /**
     * Called if plugins config was reloaded
     */
    void onConfigReload();
}
