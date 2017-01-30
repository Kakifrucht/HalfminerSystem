package de.halfminer.hmb.mode.abs;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;


public interface GameMode extends Listener {

    boolean onCommand(CommandSender sender, String[] args);

    boolean onAdminCommand(CommandSender sender, String[] args);

    void onPluginDisable();

    void onConfigReload();
}
