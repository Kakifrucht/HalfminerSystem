package de.halfminer.hmb.mode.abs;

import de.halfminer.hmb.enums.GameModeType;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;


public interface GameMode extends Listener {

    /**
     * @return this gamemodes type
     */
    GameModeType getType();

    /**
     * Called if gamemode specific command was executed
     *
     * @param sender    sender of  command
     * @param args      given arguments
     * @return true if command is implemented
     */
    boolean onCommand(CommandSender sender, String[] args);

    /**
     * Called if gamemode specific admin command was executed, such as <i>/hmb <mode> (args[])</i>
     *
     * @param sender    sender of  command
     * @param args      given arguments
     * @return true if command is implemented
     */
    boolean onAdminCommand(CommandSender sender, String[] args);

    /**
     * Called if plugin is disabled, to gracefully reset ingame states
     */
    void onPluginDisable();

    /**
     * Called if plugins config was reloaded
     */
    void onConfigReload();
}
