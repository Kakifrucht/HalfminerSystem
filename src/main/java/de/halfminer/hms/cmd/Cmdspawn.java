package de.halfminer.hms.cmd;

import de.halfminer.hms.cmd.abs.HalfminerCommand;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.handlers.HanTeleport;
import de.halfminer.hms.modules.ModRespawn;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * - Teleport player to spawn
 * - Teleport other players to spawn with permission
 * - Teleport offline players to spawn once they login
 * - Set the spawnpoint (Command /spawn s, only with permission)
 */
@SuppressWarnings("unused")
public class Cmdspawn extends HalfminerCommand {

    private final ModRespawn respawn = (ModRespawn) hms.getModule(ModuleType.RESPAWN);

    public Cmdspawn() {
        this.permission = "hms.spawn";
    }

    @Override
    public void execute() {

        if (args.length == 0) teleport(sender, false);
        else {

            if (isPlayer && args[0].equalsIgnoreCase("s") && sender.hasPermission("hms.spawn.set")) {

                MessageBuilder.create(hms, "cmdSpawnSet", "Spawn").sendMessage(player);
                respawn.setSpawn(player.getLocation());

            } else if (sender.hasPermission("hms.spawn.others")) {

                Player toTeleport = server.getPlayer(args[0]);
                if (toTeleport != null) {

                    if (toTeleport.equals(sender)) teleport(toTeleport, false);
                    else {

                        teleport(toTeleport, true);
                        MessageBuilder.create(hms, "cmdSpawnOthers", "Spawn")
                                .addPlaceholderReplace("%PLAYER%", toTeleport.getName())
                                .sendMessage(sender);
                    }
                }
                else {
                    try {

                        OfflinePlayer p = storage.getPlayer(args[0]).getBase();
                        MessageBuilder.create(hms, respawn.teleportToSpawnOnJoin(p) ?
                                "cmdSpawnOthersOfflineAdd" : "cmdSpawnOthersOfflineRemove", "Spawn")
                                .addPlaceholderReplace("%PLAYER%", p.getName())
                                .sendMessage(sender);
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
                MessageBuilder.create(hms, "modRespawnForced", "Spawn").sendMessage(toTeleport);
                tp.startTeleport((Player) toTeleport, respawn.getSpawn(), 0);
            }
            else tp.startTeleport((Player) toTeleport, respawn.getSpawn());

        } else sendNotAPlayerMessage("Spawn");
    }
}
