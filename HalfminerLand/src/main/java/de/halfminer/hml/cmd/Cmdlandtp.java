package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.MessageBuilder;

public class Cmdlandtp extends LandCommand {


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
                hms.getTeleportHandler().startTeleport(player, teleportTo.getTeleportLocation());
            } else {
                MessageBuilder.create("teleportNotExist", hml).sendMessage(player);
            }
        } else {
            MessageBuilder.create("cmdLandtpUsage", hml).sendMessage(player);
        }
    }
}
