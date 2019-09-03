package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;

import java.util.Set;
import java.util.UUID;

public class Cmdinfo extends LandCommand {


    public Cmdinfo() {
        super("info");
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        Land land = board.getLandAt(player);
        board.showChunkParticles(player, land);

        if (land.hasOwner()) {

            StringBuilder friendString = new StringBuilder();
            Set<UUID> members = land.getMemberSet();
            if (members.isEmpty()) {
                friendString.append(MessageBuilder.returnMessage("cmdInfoNoFriends", hml, false));
            } else {

                for (UUID member : members) {
                    try {
                        friendString.append(hms.getStorageHandler().getPlayer(member).getName()).append(" ");
                    } catch (PlayerNotFoundException e) {
                        hml.getLogger().warning("Player with UUID '" + member.toString() + "' not found in storage");
                    }
                }
            }

            String teleportName = land.hasTeleportLocation() ?
                    land.getTeleportName() : MessageBuilder.returnMessage("cmdInfoNoTeleport", hml, false);

            MessageBuilder.create("cmdInfoOwned" + (land.isAbandoned() ? "Abandoned" : ""), hml)
                    .addPlaceholder("%OWNER%", land.getOwnerName())
                    .addPlaceholder("%OWNEDLANDS%", land.isServerLand() ? "1" : board.getLands(land.getOwner()).size())
                    .addPlaceholder("%FRIENDS%", friendString.toString().trim())
                    .addPlaceholder("%TELEPORT%", teleportName)
                    .sendMessage(player);

        } else {
            MessageBuilder.create("cmdInfoFree", hml).sendMessage(player);
        }
    }
}
