package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;

import java.util.Set;
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

        // add to all
        boolean success = false;
        String messageLocale = addFriend ? "cmdFriendAdd" : "cmdFriendRemove";
        if (args.length > 2 && args[2].equalsIgnoreCase("all")) {

            Set<Land> lands = board.getLands(player);
            if (lands.size() == 0) {
                MessageBuilder.create("cmdFriendNoLandOwned", hml).sendMessage(player);
                return;
            }

            for (Land land : lands) {

                if (isFriendLimitReachedAndMessage(land, addFriend, false)) {
                    continue;
                }

                success |= addFriend ? land.addMember(uuid) : land.removeMember(uuid);
            }

            messageLocale += success ? "SuccessAll" : "FailureAll";

        } else {

            // add to current land
            Land land = board.getLandAt(player);
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
                .addPlaceholderReplace("%PLAYER%", toModify.getName())
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
                    .addPlaceholderReplace("%LIMIT%", String.valueOf(friendLimit))
                    .sendMessage(player);
        }

        return limitReached;
    }

    private void sendUsage() {
        MessageBuilder.create("cmdFriendUsage", hml).sendMessage(player);
    }
}
