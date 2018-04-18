package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Chunk;

import java.util.Set;
import java.util.UUID;

public class Cmdinfo extends LandCommand {

    private static final int CHUNK_WIDTH_BLOCKS = 16;


    public Cmdinfo() {
        super("info");
    }

    @Override
    public void execute() {

        if (args.length > 0
                && sender.hasPermission("hml.cmd.info.others")) {

            HalfminerPlayer toLookup;
            try {
                toLookup = hms.getStorageHandler().getPlayer(args[0]);
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "Land");
                return;
            }

            Set<Land> lands = board.getLands(toLookup.getUniqueId());

            if (lands.isEmpty()) {
                MessageBuilder.create("cmdInfoPlayerNoLand", hml)
                        .addPlaceholderReplace("%PLAYER%", toLookup.getName())
                        .sendMessage(sender);
            } else {

                int teleportCount = 0;
                StringBuilder landListStringBuilder = new StringBuilder();
                for (Land land : lands) {

                    Chunk chunk = land.getChunk();
                    MessageBuilder toAppendBuilder = MessageBuilder
                            .create("cmdInfoPlayerLandFormat" + (land.hasTeleportLocation() ? "Teleport" : ""), hml)
                            .addPlaceholderReplace("%WORLD%", chunk.getWorld().getName())
                            .addPlaceholderReplace("%X%", String.valueOf(chunk.getX() * CHUNK_WIDTH_BLOCKS))
                            .addPlaceholderReplace("%Z%", String.valueOf(chunk.getZ() * CHUNK_WIDTH_BLOCKS));

                    if (land.hasTeleportLocation()) {
                        toAppendBuilder.addPlaceholderReplace("%TELEPORT%", land.getTeleportName());
                        teleportCount++;
                    }

                    landListStringBuilder
                            .append(toAppendBuilder.togglePrefix().returnMessage())
                            .append(" ");
                }

                MessageBuilder.create("cmdInfoPlayer", hml)
                        .addPlaceholderReplace("%PLAYER%", toLookup.getName())
                        .addPlaceholderReplace("%TELEPORTAMOUNT%", String.valueOf(teleportCount))
                        .addPlaceholderReplace("%LANDAMOUNT%", String.valueOf(lands.size()))
                        .addPlaceholderReplace("%LANDLIST%", landListStringBuilder.toString().trim())
                        .sendMessage(sender);
            }

            return;
        }

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

            MessageBuilder.create(land.hasTeleportLocation() ? "cmdInfoOwnedTeleport" : "cmdInfoOwned", hml)
                    .addPlaceholderReplace("%OWNER%", land.getOwner().getName())
                    .addPlaceholderReplace("%OWNEDLANDS%", String.valueOf(board.getLands(land.getOwner().getUniqueId()).size()))
                    .addPlaceholderReplace("%FRIENDS%", friendString.toString().trim())
                    .addPlaceholderReplace("%TELEPORT%", land.getTeleportName())
                    .sendMessage(player);

        } else {
            MessageBuilder.create("cmdInfoFree", hml).sendMessage(player);
        }
    }
}
