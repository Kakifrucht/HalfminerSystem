package de.halfminer.hml.cmd;

import de.halfminer.hml.WorldGuardHelper;
import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.data.PinnedTeleport;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

public class Cmdhml extends LandCommand {

    private static final String PREFIX = "Land";


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
            case "flytime":
                flyTime();
                break;
            case "free":
                free();
                break;
            case "pintp":
                pinTp();
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
                    .addPlaceholder("%TITLE%", title)
                    .sendMessage(player);
            return;
        }

        String title = Utils.arrayToString(args, 1, true);
        land.setTitle(title);
        MessageBuilder.create("cmdHmlCustomTitleSet", hml)
                .addPlaceholder("%TITLE%", title)
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

    private void flyTime() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        HalfminerPlayer halfminerPlayer;
        try {
            halfminerPlayer = hms.getStorageHandler().getPlayer(args[1]);
        } catch (PlayerNotFoundException e) {
            e.sendNotFoundMessage(sender, PREFIX);
            return;
        }

        LandPlayer landPlayer = hml.getLandStorage().getLandPlayer(halfminerPlayer);
        int flyTimeLeft = landPlayer.getFlyTimeLeft();

        int setTo = -1;
        if (args.length > 2) {
            try {
                setTo = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        boolean setFlyTime = setTo >= 0;
        if (setFlyTime) {
            landPlayer.setFlyTimeLeft(setTo);
        }

        MessageBuilder.create("cmdHmlFlyTime" + (setFlyTime ? "Set" : ""), hml)
                .addPlaceholder("%PLAYER%", halfminerPlayer.getName())
                .addPlaceholder("%TIMELEFT%", flyTimeLeft)
                .addPlaceholder("%TIMESET%", setTo)
                .sendMessage(sender);
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
            e.sendNotFoundMessage(sender, PREFIX);
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

        boolean setFreeLands = setTo >= 0;
        if (setFreeLands) {
            landPlayer.setFreeLands(setTo);
            hml.getLogger().info("Set free lands for " + toEdit.getName() + " from "
                    + setFreeAmount + " to " + setTo);
        }

        MessageBuilder.create(setFreeLands ? "cmdHmlFreeSet" : "cmdHmlFreeShow", hml)
                .addPlaceholder("%PLAYER%", toEdit.getName())
                .addPlaceholder("%HASFREE%", hasFreeAmount)
                .addPlaceholder("%CURRENTFREE%", setFreeAmount)
                .addPlaceholder("%SETFREE%", setTo)
                .sendMessage(sender);
    }

    private void pinTp() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        Land land = board.getLandFromTeleport(args[1]);
        if (land == null) {
            MessageBuilder.create("cmdHmlPinTpNotFound", hml).sendMessage(sender);
            return;
        }

        LandStorage landStorage = hml.getLandStorage();

        // check for removal
        List<PinnedTeleport> pinnedTeleports = landStorage.getPinnedTeleportList();
        for (PinnedTeleport pinnedTeleport : new ArrayList<>(pinnedTeleports)) {
            if (pinnedTeleport.getTeleport().equals(land.getTeleportName())) {

                pinnedTeleports.remove(pinnedTeleport);
                landStorage.setPinnedTeleportList(pinnedTeleports);

                MessageBuilder.create("cmdHmlPinTpRemoved", hml)
                        .addPlaceholder("%TELEPORT%", land.getTeleportName())
                        .sendMessage(sender);
                return;
            }
        }

        Material material = null;
        if (args.length > 2) {
            material = Material.matchMaterial(args[2]);
            if (material == null) {
                MessageBuilder.create("cmdHmlPinTpUnknownMaterial", hml).sendMessage(sender);
                return;
            }
        }

        pinnedTeleports.add(new PinnedTeleport(land, material));
        landStorage.setPinnedTeleportList(pinnedTeleports);
        MessageBuilder.create("cmdHmlPinTpPinned", hml)
                .addPlaceholder("%TELEPORT%", land.getTeleportName())
                .sendMessage(sender);
    }

    private void reload() {
        hml.reload();
        MessageBuilder.create("pluginReloaded", PREFIX)
                .addPlaceholder("%PLUGINNAME%", hml.getName())
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
                    .addPlaceholder("%WORLD%", worldIntegerEntry.getKey().getName())
                    .addPlaceholder("%AMOUNT%", worldIntegerEntry.getValue())
                    .returnMessage()).append(" ");
        }

        MessageBuilder.create("cmdHmlStatus", hml)
                .addPlaceholder("%TOTALLANDS%", totalLands)
                .addPlaceholder("%TOTALTELEPORTS%", totalTeleports)
                .addPlaceholder("%TOTALFREE%", totalFree)
                .addPlaceholder("%TOTALSERVER%", totalServer)
                .addPlaceholder("%TOTALABANDONED%", totalAbandoned)
                .addPlaceholder("%TOTALWORLDLIST%", worldListBuilder.toString().trim())
                .sendMessage(sender);
    }

    private void showUsage() {
        MessageBuilder.create("cmdHmlUsage", hml)
                .addPlaceholder("%VERSION%", hml.getDescription().getVersion())
                .sendMessage(sender);
    }
}
