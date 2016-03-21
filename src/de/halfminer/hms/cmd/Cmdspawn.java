package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.handlers.HanTeleport;
import de.halfminer.hms.modules.ModRespawn;
import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdspawn extends HalfminerCommand {

    private final ModRespawn respawn = (ModRespawn) hms.getModule(ModuleType.RESPAWN);

    public Cmdspawn() {
        this.permission = "hms.spawn";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length == 0) teleport(sender, false);
        else {

            if ((sender instanceof Player) && args[0].equalsIgnoreCase("s") && sender.hasPermission("hms.spawn.set")) {

                Player p = (Player) sender;

                p.sendMessage(Language.getMessagePlaceholders("commandSpawnSet", true, "%PREFIX%", "Spawn"));
                respawn.setSpawn(p.getLocation());

            } else if (sender.hasPermission("hms.spawn.others")) {

                Player toTeleport = hms.getServer().getPlayer(args[0]);
                if (toTeleport != null) teleport(toTeleport, true);
                else sender.sendMessage(Language.getMessagePlaceholders("playerNotOnline", true, "%PREFIX%", "Spawn"));
            } else teleport(sender, false);
        }
    }

    private void teleport(CommandSender sender, boolean forced) {

        if (sender instanceof Player) {

            HanTeleport tp = (HanTeleport) hms.getHandler(HandlerType.TELEPORT);

            if (forced) tp.startTeleport((Player) sender, respawn.getSpawn(), 0);
            else tp.startTeleport((Player) sender, respawn.getSpawn());

        } else sender.sendMessage(Language.getMessage("notAPlayer"));
    }
}
