package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.MessageBuilder;

public class Cmdlandtp extends LandCommand {

    private static final int OWN_TELEPORT_DELAY_SECONDS = 30;


    public Cmdlandtp() {
        super("landtp");
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        if (args.length > 0) {
            Land teleportTo = board.getLandFromTeleport(args[0].toLowerCase());
            if (teleportTo != null) {

                if (teleportTo.isAbandoned()) {
                    MessageBuilder.create("cmdLandtpIsAbandoned", hml).sendMessage(player);
                } else {

                    if (teleportTo.isOwner(player) && !player.hasPermission("hml.bypass.landtptimer")) {
                        hms.getTeleportHandler().startTeleport(player, teleportTo.getTeleportLocation(), OWN_TELEPORT_DELAY_SECONDS);
                    } else {
                        hms.getTeleportHandler().startTeleport(player, teleportTo.getTeleportLocation());
                    }
                }

            } else {
                MessageBuilder.create("teleportNotExist", hml).sendMessage(player);
            }
        } else {
            MessageBuilder.create("cmdLandtpUsage", hml).sendMessage(player);
        }
    }
}
