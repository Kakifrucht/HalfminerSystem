package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModuleDisabledException;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hmc.module.ModRespawn;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.handler.HanTeleport;
import de.halfminer.hms.util.Message;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * - Teleport player to spawn
 * - Teleport other players to spawn with permission
 * - Teleport offline players to spawn once they login
 * - Set the spawnpoint with command alias /setspawn
 *   - Set the ender portal spawnpoint with /setspawn end
 */
@SuppressWarnings("unused")
public class Cmdspawn extends HalfminerCommand {

    private static final String PREFIX = "Spawn";

    public Cmdspawn() {
        this.permission = "hmc.spawn";
    }

    @Override
    public void execute() throws ModuleDisabledException {

        if (isPlayer && label.equals("setspawn") && sender.hasPermission("hmc.spawn.set")) {

            ModRespawn respawn = (ModRespawn) hmc.getModule(ModuleType.RESPAWN);
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("end")) {
                    boolean success = respawn.setEndPortalSpawn(player.getLocation());
                    Message.create( "cmdSpawnSetEnd" + (success ? "" : "WrongWorld"), hmc, PREFIX).send(player);
                } else {
                    Message.create("cmdSpawnSetUsage", hmc, PREFIX).send(player);
                }
            } else {
                respawn.setSpawn(player.getLocation());
                Message.create("cmdSpawnSet", hmc, PREFIX).send(player);
            }

            return;
        }

        if (args.length > 0 && sender.hasPermission("hmc.spawn.others")) {
            Player toTeleport = server.getPlayer(args[0]);
            if (toTeleport != null) {

                if (toTeleport.equals(sender)) teleport(toTeleport, false);
                else {

                    teleport(toTeleport, true);
                    Message.create("cmdSpawnOthers", hmc, PREFIX)
                            .addPlaceholder("%PLAYER%", toTeleport.getName())
                            .send(sender);
                }
            } else {
                try {
                    OfflinePlayer p = storage.getPlayer(args[0]).getBase();
                    ModRespawn respawn = (ModRespawn) hmc.getModule(ModuleType.RESPAWN);
                    Message.create(respawn.teleportToSpawnOnJoin(p) ?
                            "cmdSpawnOthersOfflineAdd" : "cmdSpawnOthersOfflineRemove", hmc, PREFIX)
                            .addPlaceholder("%PLAYER%", p.getName())
                            .send(sender);
                } catch (PlayerNotFoundException e) {
                    e.sendNotFoundMessage(sender, PREFIX);
                }
            }
        } else {
            teleport(sender, false);
        }
    }

    private void teleport(CommandSender toTeleport, boolean forced) throws ModuleDisabledException {

        if (toTeleport instanceof Player) {

            ModRespawn respawn = (ModRespawn) hmc.getModule(ModuleType.RESPAWN);
            HanTeleport tp = hms.getTeleportHandler();

            if (forced) {
                Message.create("modRespawnForced", hmc, PREFIX).send(toTeleport);
                tp.startTeleport((Player) toTeleport, respawn.getSpawn(), 0);
            }
            else tp.startTeleport((Player) toTeleport, respawn.getSpawn());

        } else sendNotAPlayerMessage(PREFIX);
    }
}
