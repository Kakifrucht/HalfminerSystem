package de.halfminer.hml.cmd;

import com.google.common.base.CharMatcher;
import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;

import java.util.List;

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
            showUsage();
            return;
        }

        String teleportName = args[1].toLowerCase();
        Land teleportLand = board.getLandFromTeleport(teleportName);
        Location location = player.getLocation();

        Land land = board.getLandAt(player);

        if (args[0].equalsIgnoreCase("buy")) {

            boolean removeTeleport = false;
            if (teleportLand != null) {

                boolean isStealingEnabled = hml.getConfig().getBoolean("teleport.allowStealingAbandoned", false);
                if (isStealingEnabled && teleportLand.isAbandoned()) {
                    removeTeleport = true;
                } else {
                    Message.create("cmdTeleportBuyAlreadyExists" + (teleportLand.isOwner(player) ? "Owned" : ""), hml)
                            .addPlaceholder("%TELEPORT%", teleportLand.getTeleportName())
                            .send(player);
                    return;
                }
            }

            if (!player.hasPermission("hml.bypass.teleportblacklist")) {
                List<String> blacklist = hml.getConfig().getStringList("teleport.name.blacklist");

                for (String blacklistedName : blacklist) {
                    if (blacklistedName.equalsIgnoreCase(teleportName)) {
                        Message.create("cmdTeleportBuyBlacklisted", hml).send(player);
                        return;
                    }
                }
            }

            if (!player.hasPermission("hml.bypass.teleportlength")) {

                int minLength = hml.getConfig().getInt("teleport.name.minLength", 4);
                int maxLength = hml.getConfig().getInt("teleport.name.maxLength", 15);

                if (teleportName.length() < minLength
                        || teleportName.length() > maxLength
                        || !CharMatcher.ascii().matchesAllOf(teleportName)) {

                    Message.create("cmdTeleportBuyNameFormat", hml)
                            .addPlaceholder("%MINLENGTH%", minLength)
                            .addPlaceholder("%MAXLENGTH%", maxLength)
                            .send(player);
                    return;
                }
            }

            if (!land.isOwner(player)) {
                Message.create("landNotOwned", hml).send(player);
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
                    Message.create("cmdTeleportBuyLimitReached", hml)
                            .addPlaceholder("%LIMIT%", maximumTeleports)
                            .send(player);
                    return;
                }
            }

            double cost = hml.getConfig().getDouble("teleport.price", 10d);
            if (takeMoney(cost)) {

                if (removeTeleport) {
                    teleportLand.removeTeleport();
                }

                land.setTeleport(teleportName, location);
                board.landWasUpdated(land);

                Message.create("cmdTeleportBuySuccess", hml)
                        .addPlaceholder("%TELEPORT%", teleportName)
                        .addPlaceholder("%COST%", cost)
                        .send(player);

                hml.getLogger().info(player.getName() + " bought teleport "
                        + teleportName + " at " + Utils.getStringFromLocation(location) + " for $" + cost);
            }

        } else if (args[0].equalsIgnoreCase("set")) {

            // check if land is owned by player setting the teleport
            if (!land.isOwner(player)) {
                Message.create("landNotOwned", hml).send(player);
                return;
            }

            if (isLandNotOwned(teleportLand)) {
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
                    teleportLand.removeTeleport();
                    board.landWasUpdated(teleportLand);
                }

                Message.create("cmdTeleportSetSuccess", hml)
                        .addPlaceholder("%TELEPORT%", teleportName)
                        .addPlaceholder("%COST%", cost)
                        .send(player);

                hml.getLogger().info(player.getName() + " set teleport "
                        + teleportName + " at " + Utils.getStringFromLocation(location) + " for $" + cost);
            }

        } else if (args[0].equalsIgnoreCase("delete")) {

            boolean canDeleteOthers = player.hasPermission("hml.cmd.teleport.deleteothers");
            if (teleportLand != null && (teleportLand.isOwner(player) || canDeleteOthers)) {

                teleportLand.removeTeleport();
                board.landWasUpdated(teleportLand);

                Message.create("cmdTeleportDeleteSuccess", hml)
                        .addPlaceholder("%TELEPORT%", teleportName)
                        .send(player);

                hml.getLogger().info(player.getName() + " deleted teleport " + teleportName);
            } else {
                Message.create(canDeleteOthers ? "teleportNotExist" : "cmdTeleportNotOwned", hml)
                        .send(player);
            }

        } else if (args[0].equalsIgnoreCase("show")) {

            if (isLandNotOwned(teleportLand)) {
                return;
            }

            LandPlayer landPlayer = hml.getLandStorage().getLandPlayer(player);

            String setTo;
            String localeKey = "cmdTeleportShow";

            String currentlyShown = landPlayer.getShownTeleport();
            if (currentlyShown != null && currentlyShown.equalsIgnoreCase(teleportName)) {
                // unset teleport if given teleport is currently shown
                setTo = null;
                localeKey += "Unset";
            } else {
                setTo = teleportName;
            }

            landPlayer.setShownTeleport(setTo);
            Message.create(localeKey, hml)
                    .addPlaceholder("%TELEPORT%", teleportName)
                    .send(player);

        } else {
            showUsage();
        }
    }

    private boolean takeMoney(double cost) {
        if (hms.getHooksHandler().getMoney(player) < cost) {
            Message.create("notEnoughMoney", hml)
                    .addPlaceholder("%COST%", cost)
                    .send(player);
            return false;
        }

        hms.getHooksHandler().addMoney(player, -cost);
        return true;
    }

    private boolean isLandNotOwned(Land teleportLand) {
        if (teleportLand == null || !teleportLand.isOwner(player)) {
            Message.create("cmdTeleportNotOwned", hml).send(player);
            return true;
        }

        return false;
    }

    private void sendLandAlreadyHasTeleportMessage(Land land) {
        Message.create("cmdTeleportLandAlreadyHasTeleport", hml)
                .addPlaceholder("%TELEPORT%", land.getTeleportName())
                .send(player);
    }

    private void showUsage() {
        Message.create("cmdTeleportUsage", hml)
                .togglePrefix()
                .send(player);
    }
}
