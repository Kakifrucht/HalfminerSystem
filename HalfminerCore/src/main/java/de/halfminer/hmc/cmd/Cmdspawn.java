package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hmc.module.ModRespawn;
import de.halfminer.hms.exceptions.PlayerNotFoundException;
import de.halfminer.hms.handler.HanTeleport;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * - Teleport player to spawn
 * - Teleport other players to spawn with permission
 * - Teleport offline players to spawn once they login
 * - Set the spawnpoint with /setspawn
 */
@SuppressWarnings("unused")
public class Cmdspawn extends HalfminerCommand {

    private final ModRespawn respawn = (ModRespawn) hmc.getModule(ModuleType.RESPAWN);

    public Cmdspawn() {
        this.permission = "hmc.spawn";
    }

    @Override
    public void execute() {

        if (isPlayer && label.equals("setspawn") && sender.hasPermission("hmc.spawn.set")) {
            respawn.setSpawn(player.getLocation());
            MessageBuilder.create("cmdSpawnSet", hmc, "Spawn").sendMessage(player);
            return;
        }

        if (args.length > 0 && sender.hasPermission("hmc.spawn.others")) {
            Player toTeleport = server.getPlayer(args[0]);
            if (toTeleport != null) {

                if (toTeleport.equals(sender)) teleport(toTeleport, false);
                else {

                    teleport(toTeleport, true);
                    MessageBuilder.create("cmdSpawnOthers", hmc, "Spawn")
                            .addPlaceholderReplace("%PLAYER%", toTeleport.getName())
                            .sendMessage(sender);
                }
            } else {
                try {

                    OfflinePlayer p = storage.getPlayer(args[0]).getBase();
                    MessageBuilder.create(respawn.teleportToSpawnOnJoin(p) ?
                            "cmdSpawnOthersOfflineAdd" : "cmdSpawnOthersOfflineRemove", hmc, "Spawn")
                            .addPlaceholderReplace("%PLAYER%", p.getName())
                            .sendMessage(sender);
                } catch (PlayerNotFoundException e) {
                    e.sendNotFoundMessage(sender, "Spawn");
                }
            }
        } else {
            teleport(sender, false);
        }
    }

    private void teleport(CommandSender toTeleport, boolean forced) {

        if (toTeleport instanceof Player) {

            HanTeleport tp = hms.getTeleportHandler();

            if (forced) {
                MessageBuilder.create("modRespawnForced", hmc, "Spawn").sendMessage(toTeleport);
                tp.startTeleport((Player) toTeleport, respawn.getSpawn(), 0);
            }
            else tp.startTeleport((Player) toTeleport, respawn.getSpawn());

        } else sendNotAPlayerMessage("Spawn");
    }
}
