package de.halfminer.hml.cmd;

import de.halfminer.hml.WorldGuardHelper;
import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Cmdhml extends LandCommand {


    public Cmdhml() {
        super("hml");
    }

    @Override
    protected void execute() {

        if (args.length < 1) {
            showUsage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "customtitle":
                customTitle();
                break;
            case "forcewgrefresh":
                forceWGRefresh();
                break;
            case "free":
                free();
                break;
            case "info":
                info();
                break;
            case "reload":
                reload();
                break;
            case "save":
                save();
                break;
            case "status":
                status();
                break;
            default:
                showUsage();
        }
    }

    private void customTitle() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        Land land = board.getLandAt(player);
        if (!land.hasOwner()) {
            MessageBuilder.create("cmdHmlCustomTitleNotOwned", hml).sendMessage(player);
            return;
        }

        if (args[1].equalsIgnoreCase("-c")) {

            if (!land.hasTitle()) {
                MessageBuilder.create("cmdHmlCustomTitleNoneSet", hml).sendMessage(player);
                return;
            }

            String title = land.getTitle();
            land.setTitle(null);
            MessageBuilder.create("cmdHmlCustomTitleRemoved", hml)
                    .addPlaceholderReplace("%TITLE%", title)
                    .sendMessage(player);
            return;
        }

        String title = Utils.arrayToString(args, 1, true);
        land.setTitle(title);
        MessageBuilder.create("cmdHmlCustomTitleSet", hml)
                .addPlaceholderReplace("%TITLE%", title)
                .sendMessage(player);
    }

    private void forceWGRefresh() {
        MessageBuilder.create("cmdHmlRefreshStarted", hml).sendMessage(sender);

        WorldGuardHelper wgh = hml.getWorldGuardHelper();
        for (Land land : board.getOwnedLandSet()) {
            wgh.updateRegionOfLand(land, true, true);
        }

        MessageBuilder.create("cmdHmlRefreshDone", hml).sendMessage(sender);
    }

    private void free() {
        if (args.length < 2) {
            showUsage();
            return;
        }

        HalfminerPlayer toEdit;
        LandPlayer landPlayer;
        try {
            toEdit = hms.getStorageHandler().getPlayer(args[1]);
            landPlayer = hml.getLandStorage().getLandPlayer(toEdit);
        } catch (PlayerNotFoundException e) {
            e.sendNotFoundMessage(sender, "Land");
            return;
        }

        int setTo = -1;
        if (args.length > 2) {
            try {
                setTo = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        int setFreeAmount = landPlayer.getFreeLands();
        int hasFreeAmount = board.getLands(toEdit)
                .stream()
                .filter(Land::isFreeLand)
                .collect(Collectors.toList())
                .size();

        if (setTo >= 0) {

            landPlayer.setFreeLands(setTo);
            hml.getLogger().info("Set free lands for " + toEdit.getName() + " from "
                    + setFreeAmount + " to " + setTo);
        }

        MessageBuilder.create(setTo >= 0 ? "cmdHmlFreeSet" : "cmdHmlFreeShow", hml)
                .addPlaceholderReplace("%PLAYER%", toEdit.getName())
                .addPlaceholderReplace("%HASFREE%", String.valueOf(hasFreeAmount))
                .addPlaceholderReplace("%CURRENTFREE%", String.valueOf(setFreeAmount))
                .addPlaceholderReplace("%SETFREE%", String.valueOf(setTo))
                .sendMessage(sender);
    }

    private void info() {
        if (args.length < 2) {
            showUsage();
            return;
        }

        Set<Land> lands;
        String landOwner;

        if (args[1].equalsIgnoreCase("-s")) {

            lands = board.getLandsOfServer();
            landOwner = hml.getConfig().getString("serverName");

        } else {
            try {
                HalfminerPlayer hPlayer = hms.getStorageHandler().getPlayer(args[1]);
                lands = board.getLands(hPlayer);
                landOwner = hPlayer.getName();
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(player, "Land");
                return;
            }
        }

        if (lands.isEmpty()) {
            MessageBuilder.create("cmdHmlInfoPlayerNoLand", hml)
                    .addPlaceholderReplace("%PLAYER%", landOwner)
                    .sendMessage(sender);
        } else {

            int teleportCount = 0;
            StringBuilder landListStringBuilder = new StringBuilder();
            for (Land land : lands) {

                MessageBuilder toAppendBuilder = MessageBuilder
                        .create("cmdHmlInfoPlayerLandFormat" + (land.hasTeleportLocation() ? "Teleport" : ""), hml)
                        .addPlaceholderReplace("%WORLD%", land.getWorld().getName())
                        .addPlaceholderReplace("%X%", String.valueOf(land.getXLandCorner()))
                        .addPlaceholderReplace("%Z%", String.valueOf(land.getZLandCorner()));

                if (land.hasTeleportLocation()) {
                    toAppendBuilder.addPlaceholderReplace("%TELEPORT%", land.getTeleportName());
                    teleportCount++;
                }

                landListStringBuilder
                        .append(toAppendBuilder.togglePrefix().returnMessage())
                        .append(" ");
            }

            MessageBuilder.create("cmdHmlInfoPlayer", hml)
                    .addPlaceholderReplace("%PLAYER%", landOwner)
                    .addPlaceholderReplace("%TELEPORTAMOUNT%", String.valueOf(teleportCount))
                    .addPlaceholderReplace("%LANDAMOUNT%", String.valueOf(lands.size()))
                    .addPlaceholderReplace("%LANDLIST%", landListStringBuilder.toString().trim())
                    .sendMessage(sender);
        }
    }


    private void reload() {
        hml.reload();
        MessageBuilder.create("pluginReloaded", "Land")
                .addPlaceholderReplace("%PLUGINNAME%", hml.getName())
                .sendMessage(sender);
    }

    private void save() {
        hml.saveLandStorage();
        MessageBuilder.create("cmdHmlSave", hml).sendMessage(sender);
    }

    private void status() {
        Set<Land> allOwnedLands = board.getOwnedLandSet();
        int totalLands = allOwnedLands.size();
        int totalTeleports = 0;
        int totalFree = 0;
        int totalServer = 0;
        int totalAbandoned = 0;
        Map<World, Integer> worldCountMap = new HashMap<>();

        for (Land land : allOwnedLands) {

            World world = land.getWorld();
            if (!worldCountMap.containsKey(world)) {
                worldCountMap.put(world, 1);
            } else {
                worldCountMap.put(world, worldCountMap.get(world) + 1);
            }

            if (land.hasTeleportLocation()) {
                totalTeleports++;
            }

            // server land is always free land aswell, don't count it as free land if it is server land
            if (land.isServerLand()) {
                totalServer++;
            } else if (land.isFreeLand()) {
                totalFree++;
            }

            if (land.isAbandoned()) {
                totalAbandoned++;
            }
        }

        StringBuilder worldListBuilder = new StringBuilder();
        for (Map.Entry<World, Integer> worldIntegerEntry : worldCountMap.entrySet()) {
            worldListBuilder.append(MessageBuilder.create("cmdHmlStatusWorldListEntry", hml)
                    .togglePrefix()
                    .addPlaceholderReplace("%WORLD%", worldIntegerEntry.getKey().getName())
                    .addPlaceholderReplace("%AMOUNT%", String.valueOf(worldIntegerEntry.getValue()))
                    .returnMessage()).append(" ");
        }

        MessageBuilder.create("cmdHmlStatus", hml)
                .addPlaceholderReplace("%TOTALLANDS%", String.valueOf(totalLands))
                .addPlaceholderReplace("%TOTALTELEPORTS%", String.valueOf(totalTeleports))
                .addPlaceholderReplace("%TOTALFREE%", String.valueOf(totalFree))
                .addPlaceholderReplace("%TOTALSERVER%", String.valueOf(totalServer))
                .addPlaceholderReplace("%TOTALABANDONED%", String.valueOf(totalAbandoned))
                .addPlaceholderReplace("%TOTALWORLDLIST%", worldListBuilder.toString().trim())
                .sendMessage(sender);
    }

    private void showUsage() {
        MessageBuilder.create("cmdHmlUsage", hml)
                .addPlaceholderReplace("%VERSION%", hml.getDescription().getVersion())
                .sendMessage(sender);
    }
}
