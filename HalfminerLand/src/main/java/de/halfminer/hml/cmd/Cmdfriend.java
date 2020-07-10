package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;

import java.util.List;
import java.util.UUID;

public class Cmdfriend extends LandCommand {


    public Cmdfriend() {
        super("friend");
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        if (args.length < 2) {
            sendUsage();
            return;
        }

        boolean addFriend = args[0].equalsIgnoreCase("add");
        if (!addFriend && !args[0].equalsIgnoreCase("remove")) {
            sendUsage();
            return;
        }

        HalfminerPlayer toModify;
        UUID uuid;
        try {
            toModify = hms.getStorageHandler().getPlayer(args[1]);
            uuid = toModify.getUniqueId();
        } catch (PlayerNotFoundException e) {
            e.sendNotFoundMessage(sender, "Land");
            return;
        }

        if (player.equals(toModify.getBase())) {
            MessageBuilder.create("cmdFriendSelf", hml).sendMessage(player);
            return;
        }

        String messageLocale = addFriend ? "cmdFriendAdd" : "cmdFriendRemove";
        Land land = board.getLandAt(player);
        boolean success = false;
        int modifiedCount = 0;

        // add to all or connected land
        if (args.length > 2 &&
                (args[2].equalsIgnoreCase("all")
                || args[2].equalsIgnoreCase("connected"))) {

            boolean doForAll = args[2].equalsIgnoreCase("all");

            if (!doForAll && !land.isOwner(player)) {
                MessageBuilder.create("landNotOwned", hml).sendMessage(player);
                return;
            }

            List<Land> lands = doForAll ? board.getLands(player) : board.getConnectedLand(land);
            if (lands.size() == 0) {
                MessageBuilder.create("noLandOwned", hml).sendMessage(player);
                return;
            }

            for (Land landToModify : lands) {

                if (isFriendLimitReachedAndMessage(landToModify, addFriend, false)) {
                    continue;
                }

                boolean wasModified = addFriend ? landToModify.addMember(uuid) : landToModify.removeMember(uuid);
                success |= wasModified;

                if (wasModified) {
                    modifiedCount++;
                }
            }

            messageLocale += success ? "Success" : "Failure";
            messageLocale += doForAll ? "All" : "Connected";

        } else { // add to current land

            if (land.isOwner(player)) {

                if (isFriendLimitReachedAndMessage(land, addFriend, true)) {
                    return;
                }

                success = addFriend ? land.addMember(uuid) : land.removeMember(uuid);
                messageLocale += success ? "Success" : "Failure";

            } else {
                MessageBuilder.create("landNotOwned", hml).sendMessage(player);
                return;
            }
        }

        MessageBuilder.create(messageLocale, hml)
                .addPlaceholder("%PLAYER%", toModify.getName())
                .addPlaceholder("%COUNT%", modifiedCount)
                .sendMessage(player);

        if (success) {
            hml.getLogger().info(player.getName() + " successfully modified "
                    + toModify.getName() + " land friendship status");
        }
    }

    private boolean isFriendLimitReachedAndMessage(Land land, boolean addFriend, boolean sendMessage) {

        if (!addFriend || player.hasPermission("hml.bypass.friendlimit")) {
            return false;
        }

        int friendLimit = hml.getConfig().getInt("friendLimit", 6);

        boolean limitReached = land.getMemberSet().size() >= friendLimit;
        if (limitReached && sendMessage) {
            MessageBuilder.create("cmdFriendAddLimitReached", hml)
                    .addPlaceholder("%LIMIT%", friendLimit)
                    .sendMessage(player);
        }

        return limitReached;
    }

    private void sendUsage() {
        MessageBuilder.create("cmdFriendUsage", hml)
                .togglePrefix()
                .sendMessage(player);
    }
}
