package de.halfminer.hml.cmd;

import de.halfminer.hml.WorldGuardHelper;
import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.data.PinnedTeleport;
import de.halfminer.hml.land.FlyBoard;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.Message;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.*;

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
            case "removeallwgregions":
                removeAllWGRegions();
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

    private void forceWGRefresh() {
        Message.create("cmdHmlRefreshStarted", hml).send(sender);

        WorldGuardHelper wgh = hml.getWorldGuardHelper();
        for (Land land : board.getOwnedLandSet()) {
            wgh.updateRegionOfLand(land, true, true);
        }

        Message.create("cmdHmlRefreshDone", hml).send(sender);
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
        FlyBoard flyBoard = hml.getBoard().getFlyBoard();
        OfflinePlayer offlinePlayer = halfminerPlayer.getBase();
        boolean isFlying = offlinePlayer.isOnline() && flyBoard.isPlayerFlying(offlinePlayer.getPlayer());

        int flyTimeLeft = isFlying ? flyBoard.getFlyTimeLeft(offlinePlayer.getPlayer()) : landPlayer.getFlyTimeLeft();

        int setTo = -1;
        if (args.length > 2) {
            try {
                setTo = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        boolean setFlyTime = setTo >= 0;
        if (setFlyTime) {

            // if player is currently flying set fly time while fly is not active
            if (isFlying) {
                flyBoard.togglePlayerFlying(offlinePlayer.getPlayer());
            }

            landPlayer.setFlyTimeLeft(setTo);

            if (isFlying) {
                flyBoard.togglePlayerFlying(offlinePlayer.getPlayer());
            }
        }

        Message.create("cmdHmlFlyTime" + (setFlyTime ? "Set" : ""), hml)
                .addPlaceholder("%PLAYER%", halfminerPlayer.getName())
                .addPlaceholder("%TIMELEFT%", flyTimeLeft)
                .addPlaceholder("%TIMESET%", setTo)
                .send(sender);
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
        int hasFreeAmount = (int) board.getLands(toEdit)
                .stream()
                .filter(Land::isFreeLand)
                .count();

        boolean setFreeLands = setTo >= 0;
        if (setFreeLands) {
            landPlayer.setFreeLands(setTo);
            hml.getLogger().info("Set free lands for " + toEdit.getName() + " from "
                    + setFreeAmount + " to " + setTo);
        }

        Message.create(setFreeLands ? "cmdHmlFreeSet" : "cmdHmlFreeShow", hml)
                .addPlaceholder("%PLAYER%", toEdit.getName())
                .addPlaceholder("%HASFREE%", hasFreeAmount)
                .addPlaceholder("%CURRENTFREE%", setFreeAmount)
                .addPlaceholder("%SETFREE%", setTo)
                .send(sender);
    }

    private void pinTp() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        Land land = board.getLandFromTeleport(args[1]);
        if (land == null) {
            Message.create("cmdHmlPinTpNotFound", hml).send(sender);
            return;
        }

        LandStorage landStorage = hml.getLandStorage();

        // check for removal
        List<PinnedTeleport> pinnedTeleports = landStorage.getPinnedTeleportList();
        for (PinnedTeleport pinnedTeleport : new ArrayList<>(pinnedTeleports)) {
            if (pinnedTeleport.getTeleport().equals(land.getTeleportName())) {

                pinnedTeleports.remove(pinnedTeleport);
                landStorage.setPinnedTeleportList(pinnedTeleports);

                Message.create("cmdHmlPinTpRemoved", hml)
                        .addPlaceholder("%TELEPORT%", land.getTeleportName())
                        .send(sender);
                return;
            }
        }

        Material material = null;
        if (args.length > 2) {
            material = Material.matchMaterial(args[2]);
            if (material == null) {
                Message.create("cmdHmlPinTpUnknownMaterial", hml).send(sender);
                return;
            }
        }

        pinnedTeleports.add(new PinnedTeleport(land, material));
        landStorage.setPinnedTeleportList(pinnedTeleports);
        Message.create("cmdHmlPinTpPinned", hml)
                .addPlaceholder("%TELEPORT%", land.getTeleportName())
                .send(sender);
    }

    private void reload() {
        hml.reload();
        Message.create("pluginReloaded", PREFIX)
                .addPlaceholder("%PLUGINNAME%", hml.getName())
                .send(sender);
    }

    private void removeAllWGRegions() {
        int count = hml.getWorldGuardHelper().removeAllRegions(server.getWorlds());
        Message.create("cmdHmlRegionsRemoved" + (count == 0 ? "None" : ""), hml)
                .addPlaceholder("%COUNT%", count)
                .send(sender);
    }

    private void save() {
        hml.saveLandStorage();
        Message.create("cmdHmlSave", hml).send(sender);
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
            worldListBuilder.append(Message.create("cmdHmlStatusWorldListEntry", hml)
                    .togglePrefix()
                    .addPlaceholder("%WORLD%", worldIntegerEntry.getKey().getName())
                    .addPlaceholder("%AMOUNT%", worldIntegerEntry.getValue())
                    .returnMessage()).append(" ");
        }

        Message.create("cmdHmlStatus", hml)
                .addPlaceholder("%TOTALLANDS%", totalLands)
                .addPlaceholder("%TOTALTELEPORTS%", totalTeleports)
                .addPlaceholder("%TOTALFREE%", totalFree)
                .addPlaceholder("%TOTALSERVER%", totalServer)
                .addPlaceholder("%TOTALABANDONED%", totalAbandoned)
                .addPlaceholder("%TOTALWORLDLIST%", worldListBuilder.toString().trim())
                .send(sender);
    }

    private void showUsage() {
        Message.create("cmdHmlUsage", hml)
                .addPlaceholder("%VERSION%", hml.getDescription().getVersion())
                .send(sender);
    }
}
