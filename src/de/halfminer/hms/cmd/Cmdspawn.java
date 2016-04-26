package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.handlers.HanTeleport;
import de.halfminer.hms.modules.ModRespawn;
import de.halfminer.hms.util.Language;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * - Teleport player to spawn
 * - Teleport other players to spawn with permission
 * - Teleport offline players to spawn once they login
 * - Use /spawn s to set the spawn (only with permission)
 */
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

                p.sendMessage(Language.getMessagePlaceholders("cmdSpawnSet", true, "%PREFIX%", "Spawn"));
                respawn.setSpawn(p.getLocation());

            } else if (sender.hasPermission("hms.spawn.others")) {

                Player toTeleport = server.getPlayer(args[0]);
                if (toTeleport != null) {

                    if (toTeleport.equals(sender)) teleport(toTeleport, false);
                    else {

                        teleport(toTeleport, true);
                        sender.sendMessage(Language.getMessagePlaceholders("cmdSpawnOthers", true, "%PREFIX%", "Spawn",
                                "%PLAYER%", toTeleport.getName()));
                    }
                }
                else {
                    try {
                        OfflinePlayer p = storage.getPlayer(args[0]).getBase();

                        if (respawn.teleportToSpawnOnJoin(p)) {
                            sender.sendMessage(Language.getMessagePlaceholders("cmdSpawnOthersOfflineAdd",
                                    true, "%PREFIX%", "Spawn", "%PLAYER%", p.getName()));
                        } else {
                            sender.sendMessage(Language.getMessagePlaceholders("cmdSpawnOthersOfflineRemove",
                                    true, "%PREFIX%", "Spawn", "%PLAYER%", p.getName()));
                        }
                    } catch (PlayerNotFoundException e) {
                        e.sendNotFoundMessage(sender, "Spawn");
                    }
                }
            } else teleport(sender, false);
        }
    }

    private void teleport(CommandSender toTeleport, boolean forced) {

        if (toTeleport instanceof Player) {

            HanTeleport tp = (HanTeleport) hms.getHandler(HandlerType.TELEPORT);

            if (forced) {
                toTeleport.sendMessage(Language.getMessagePlaceholders("modRespawnForced", true, "%PREFIX%", "Spawn"));
                tp.startTeleport((Player) toTeleport, respawn.getSpawn(), 0);
            }
            else tp.startTeleport((Player) toTeleport, respawn.getSpawn());

        } else toTeleport.sendMessage(Language.getMessage("notAPlayer"));
    }
}
