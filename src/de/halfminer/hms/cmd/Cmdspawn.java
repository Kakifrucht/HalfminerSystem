package de.halfminer.hms.cmd;

import de.halfminer.hms.modules.ModRespawn;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.ModuleType;
import de.halfminer.hms.util.Teleport;
import org.bukkit.Location;
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
            Player p = (Player) sender;
            Location loc = respawn.getSpawn();
            if (forced) new Teleport(p, loc, 0).startTeleport();
            else new Teleport(p, respawn.getSpawn()).startTeleport();
        } else sender.sendMessage(Language.getMessage("notAPlayer"));
    }
}
