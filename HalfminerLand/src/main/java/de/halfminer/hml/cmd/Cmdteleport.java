package de.halfminer.hml.cmd;

import com.google.common.base.CharMatcher;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;

public class Cmdteleport extends LandCommand {


    public Cmdteleport() {
        super("teleport");
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

        String teleportName = args[1].toLowerCase();
        Land teleportLand = board.getLandFromTeleport(teleportName);
        Location location = player.getLocation();

        Land land = board.getLandAt(player);

        if (args[0].equalsIgnoreCase("buy")) {

            if (teleportLand != null) {
                MessageBuilder.create("cmdTeleportBuyAlreadyExists" + (teleportLand.isOwner(player) ? "Owned" : ""), hml)
                        .addPlaceholderReplace("%TELEPORT%", teleportLand.getTeleportName())
                        .sendMessage(player);
                return;
            }

            if (teleportName.length() < 4
                    || teleportName.length() > 15
                    || !CharMatcher.ascii().matchesAllOf(teleportName)) {
                MessageBuilder.create("cmdTeleportBuyNameFormat", hml).sendMessage(player);
                return;
            }

            if (!land.isOwner(player)) {
                MessageBuilder.create("landNotOwned", hml).sendMessage(player);
                return;
            }

            if (land.hasTeleportLocation()) {
                sendLandAlreadyHasTeleportMessage(land);
                return;
            }

            if (!player.hasPermission("hml.cmd.teleport.unlimited")) {
                int maximumTeleports = hml.getConfig().getInt("teleport.maxAmount", 3);

                int hasTeleports = 0;
                for (Land landOfPlayer : board.getLands(player)) {
                    if (landOfPlayer.hasTeleportLocation()) {
                        hasTeleports++;
                    }
                }

                if (hasTeleports >= maximumTeleports) {
                    MessageBuilder.create("cmdTeleportBuyLimitReached", hml)
                            .addPlaceholderReplace("%LIMIT%", String.valueOf(maximumTeleports))
                            .sendMessage(player);
                    return;
                }
            }

            double cost = hml.getConfig().getDouble("teleport.price", 10d);
            if (takeMoney(cost)) {
                land.setTeleport(teleportName, location);
                board.landWasUpdated(land);

                MessageBuilder.create("cmdTeleportBuySuccess", hml)
                        .addPlaceholderReplace("%TELEPORT%", teleportName)
                        .addPlaceholderReplace("%COST%", String.valueOf(cost))
                        .sendMessage(player);

                hml.getLogger().info(player.getName() + " bought teleport "
                        + teleportName + " at " + Utils.getStringFromLocation(location) + " for $" + cost);
            }

        } else if (args[0].equalsIgnoreCase("set")) {

            // check if land is owned by player setting the teleport
            if (!land.isOwner(player)) {
                MessageBuilder.create("landNotOwned", hml).sendMessage(player);
                return;
            }

            // check if player owns teleport point
            if (teleportLand == null || !teleportLand.isOwner(player)) {
                MessageBuilder.create("cmdTeleportNotOwned", hml).sendMessage(player);
                return;
            }

            boolean isDifferentLand = !teleportLand.equals(land);
            if (isDifferentLand && land.hasTeleportLocation()) {
                sendLandAlreadyHasTeleportMessage(land);
                return;
            }

            double cost = hml.getConfig().getDouble("teleport.priceMove", 5d);
            if (takeMoney(cost)) {

                land.setTeleport(teleportName, location);
                board.landWasUpdated(land);

                if (isDifferentLand) {
                    teleportLand.setTeleport(null, null);
                    board.landWasUpdated(teleportLand);
                }

                MessageBuilder.create("cmdTeleportSetSuccess", hml)
                        .addPlaceholderReplace("%TELEPORT%", teleportName)
                        .addPlaceholderReplace("%COST%", String.valueOf(cost))
                        .sendMessage(player);

                hml.getLogger().info(player.getName() + " set teleport "
                        + teleportName + " at " + Utils.getStringFromLocation(location) + " for $" + cost);
            }

        } else if (args[0].equalsIgnoreCase("delete")) {

            boolean canDeleteOthers = player.hasPermission("hml.cmd.teleport.deleteothers");
            if (teleportLand != null && (teleportLand.isOwner(player) || canDeleteOthers)) {

                teleportLand.setTeleport(null, null);
                board.landWasUpdated(teleportLand);

                MessageBuilder.create("cmdTeleportDeleteSuccess", hml)
                        .addPlaceholderReplace("%TELEPORT%", teleportName)
                        .sendMessage(player);

                hml.getLogger().info(player.getName() + " deleted teleport " + teleportName);
            } else {
                MessageBuilder.create(canDeleteOthers ? "teleportNotExist" : "cmdTeleportNotOwned", hml)
                        .sendMessage(player);
            }

        } else {
            sendUsage();
        }
    }

    private boolean takeMoney(double cost) {
        if (hms.getHooksHandler().getMoney(player) < cost) {
            MessageBuilder.create("notEnoughMoney", hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(cost))
                    .sendMessage(player);
            return false;
        }

        hms.getHooksHandler().addMoney(player, -cost);
        return true;
    }

    private void sendLandAlreadyHasTeleportMessage(Land land) {
        MessageBuilder.create("cmdTeleportLandAlreadyHasTeleport", hml)
                .addPlaceholderReplace("%TELEPORT%", land.getTeleportName())
                .sendMessage(player);
    }

    private void sendUsage() {
        MessageBuilder.create("cmdTeleportUsage", hml).sendMessage(player);
    }
}
