package de.halfminer.hms.cmd;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import de.halfminer.hms.cmd.abs.HalfminerCommand;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.exception.GiveItemException;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.handlers.HanHooks;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.modules.ModAntiXray;
import de.halfminer.hms.modules.ModSkillLevel;
import de.halfminer.hms.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

/**
 * - Copy a WorldEdit schematic to another directory (copyschematic)
 * - Give a custom item defined in customitems.txt to a player (give)
 * - Reload config (reload)
 * - Rename items, supports lore (rename)
 * - Ring players to get their attention (ring)
 * - Remove a players /home block (rmhomeblock)
 * - Run an action defined in customactions.txt (runaction)
 * - Search for homes in a given radius, hooking into Essentials (searchhomes)
 * - Edit skillelo of player (updateskill)
 * - List all currently by antixray watched players (xraybypass)
 *   - Exempt a player from AntiXRay
 */
@SuppressWarnings("unused")
public class Cmdhms extends HalfminerCommand {

    private static final String PREFIX = "HMS";

    public Cmdhms() {
        this.permission = "hms.moderator";
    }

    @Override
    public void execute() {

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "copyschematic":
                    copySchematic();
                    return;
                case "give":
                    give();
                    return;
                case "reload":
                    reload();
                    return;
                case "rename":
                    renameItem();
                    return;
                case "ring":
                    ringPlayer();
                    return;
                case "rmhomeblock":
                    rmHomeBlock();
                    return;
                case "runaction":
                    runAction();
                    return;
                case "searchhomes":
                    searchHomes();
                    return;
                case "updateskill":
                    updateSkill();
                    return;
                case "xraybypass":
                    xrayBypass();
                    return;
            }
        }
        showUsage();
    }

    private void copySchematic() {

        if (args.length < 2) {
            MessageBuilder.create("cmdHmsCopySchematicNotSpecified", hms, PREFIX).sendMessage(sender);
            return;
        }

        String destinationPath = hms.getConfig().getString("command.hms.remoteSchematicPath", "");
        if (destinationPath.length() == 0 || destinationPath.equals("/put/your/path/here")) {
            MessageBuilder.create("cmdHmsCopySchematicNotConfigured", hms, PREFIX).sendMessage(sender);
            return;
        }

        File toCopy = new File("plugins/WorldEdit/schematics/", args[1] + ".schematic");
        File destination = new File(destinationPath, args[1] + ".schematic");

        // check if destination path exists first
        if (!Files.exists(new File(destinationPath).toPath())) {
            MessageBuilder.create("cmdHmsCopySchematicNotConfigured", hms, PREFIX).sendMessage(sender);
            return;
        }

        if (toCopy.exists()) {

            if (!destination.exists()) {
                scheduler.runTaskAsynchronously(hms, () -> {
                    try {
                        Files.copy(toCopy.toPath(), destination.toPath());
                        MessageBuilder.create("cmdHmsCopySchematicCopySuccess", hms, PREFIX).sendMessage(sender);

                        // auto delete after 10 minutes
                        if (args.length > 2 && args[2].startsWith("auto")) {

                            MessageBuilder.create("cmdHmsCopySchematicDelete", hms, PREFIX).sendMessage(sender);

                            scheduler.runTaskLaterAsynchronously(hms, () -> {
                                deleteFile(toCopy);
                                deleteFile(destination);
                            }, 12000L);
                        }
                    } catch (IOException e) {
                        MessageBuilder.create("cmdHmsCopySchematicCopyError", hms, PREFIX).sendMessage(sender);
                        e.printStackTrace();
                    }
                });
            } else MessageBuilder.create("cmdHmsCopySchematicAlreadyExists", hms, PREFIX).sendMessage(sender);
        } else MessageBuilder.create("cmdHmsCopySchematicDoesntExist", hms, PREFIX).sendMessage(sender);
    }

    private void deleteFile(File toDelete) {

        boolean success = false;

        try {
            Files.delete(toDelete.toPath());
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        MessageBuilder.create(success ? "cmdHmsCopySchematicDeleted" : "cmdHmsCopySchematicDeletedError", hms)
                .addPlaceholderReplace("%PATH%", toDelete.toPath().toString())
                .logMessage(success ? Level.INFO : Level.WARNING);
    }

    private void give() {

        CustomtextCache cache;
        try {
            cache = storage.getCache("customitems.txt");
            cache.reCacheFile();
        } catch (CachingException e) {
            MessageBuilder.create("cmdCustomtextCacheParseError", hms, PREFIX)
                    .addPlaceholderReplace("%ERROR%", e.getCleanReason())
                    .sendMessage(sender);
            return;
        }

        CustomitemCache itemCache = new CustomitemCache(cache);
        if (args.length < 3) {

            Set<String> allItems = itemCache.getAllItems();
            if (!allItems.isEmpty()) {

                String allItemString = "";
                for (String itemKey : allItems) allItemString += itemKey + " ";
                allItemString = allItemString.substring(0, allItemString.length() - 1);

                MessageBuilder.create("cmdHmsGiveList", hms, PREFIX)
                        .addPlaceholderReplace("%LIST%", allItemString)
                        .sendMessage(sender);

            } else MessageBuilder.create("cmdHmsGiveListNone", hms, PREFIX).sendMessage(sender);

            return;
        }

        Player giveTo = server.getPlayer(args[1]);

        if (giveTo == null) {
            MessageBuilder.create("playerNotOnline", PREFIX).sendMessage(sender);
            return;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
            }
        }

        String itemName = args[2].toLowerCase();
        try {
            itemCache.giveItem(itemName, giveTo, amount);
            MessageBuilder.create("cmdHmsGiveSuccessful", hms, PREFIX)
                    .addPlaceholderReplace("%PLAYER%", giveTo.getName())
                    .addPlaceholderReplace("%ITEM%", itemName)
                    .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                    .sendMessage(sender);
        } catch (GiveItemException e) {

            if (e.getReason().equals(GiveItemException.Reason.INVENTORY_FULL)) {

                int totalLost = 0;
                for (ItemStack lost : e.getNotGivenItems().values()) {
                    totalLost += lost.getAmount();
                }
                MessageBuilder.create("cmdHmsGiveInventoryFull", hms, PREFIX)
                        .addPlaceholderReplace("%PLAYER%", giveTo.getName())
                        .addPlaceholderReplace("%ITEM%", itemName)
                        .addPlaceholderReplace("%AMOUNT%", String.valueOf(totalLost))
                        .sendMessage(sender);
            } else if (e.getReason().equals(GiveItemException.Reason.ITEM_NOT_FOUND)) {
                MessageBuilder.create("cmdHmsGiveItemNotFound", hms, PREFIX)
                        .addPlaceholderReplace("%ITEM%", itemName)
                        .sendMessage(sender);
            } else {
                MessageBuilder.create("cmdHmsGiveError", hms, PREFIX)
                        .addPlaceholderReplace("%REASON%", e.getCleanReason())
                        .sendMessage(sender);
            }
        }
    }

    private void reload() {
        hms.loadConfig();
        MessageBuilder.create("cmdHmsConfigReloaded", hms, PREFIX).sendMessage(sender);
    }

    private void renameItem() {

        if (!isPlayer) {
            sendNotAPlayerMessage(PREFIX);
            return;
        }

        if (args.length > 1) {

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {
                MessageBuilder.create("cmdHmsRenameFailed", hms, PREFIX).sendMessage(player);
                return;
            }

            ItemMeta meta = item.getItemMeta();

            // Default parameters, clear item name if not specified but keep lore
            String newName = meta.getDisplayName();
            List<String> lore = meta.getLore();

            // Item name must start at argument 1 only if it is not the -lore flag
            if (!args[1].equalsIgnoreCase("-lore")) {

                if (args[1].equalsIgnoreCase("reset")) newName = "";
                else {

                    newName = Utils.arrayToString(args, 1, true);
                    // Cut new string at -lore
                    for (int i = 0; i < newName.length(); i++) {
                        if (newName.substring(i).toLowerCase().startsWith("-lore")) {
                            newName = newName.substring(0, i);
                            break;
                        }
                    }
                    // Cut spaces at the end of the name
                    while (newName.endsWith(" ")) {
                        newName = newName.substring(0, newName.length() - 1);
                    }
                }
            }

            // Iterate over args and check if lore flag is set
            for (int i = 1; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-lore")) {
                    // Check if new lore was specified, else just clear it
                    if (args.length > i + 1) {
                        // Split lines of lore at | character, set the lore list
                        String[] loreToArray = Utils.arrayToString(args, i + 1, true).split("[|]");
                        lore = Arrays.asList(loreToArray);
                        break;
                    } else {
                        if (lore != null) lore.clear();
                    }
                }
            }

            // Update item
            meta.setDisplayName(newName);
            meta.setLore(lore);
            item.setItemMeta(meta);
            player.updateInventory();

            if (newName == null) newName = "";

            MessageBuilder.create("cmdHmsRenameDone", hms, PREFIX)
                    .addPlaceholderReplace("%NAME%", newName)
                    .sendMessage(player);
        } else showUsage();
    }

    private void ringPlayer() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        final Player toRing = server.getPlayer(args[1]);
        String senderName = Utils.getPlayername(sender);

        if (toRing == null) {
            MessageBuilder.create("playerNotOnline", PREFIX).sendMessage(sender);
            return;
        }

        ((HanTitles) hms.getHandler(HandlerType.TITLES)).sendTitle(toRing,
                MessageBuilder.create("cmdHmsRingTitle", hms)
                        .addPlaceholderReplace("%PLAYER%", senderName)
                        .returnMessage());

        MessageBuilder.create("cmdHmsRingMessage", hms, PREFIX)
                .addPlaceholderReplace("%PLAYER%", senderName)
                .sendMessage(toRing);

        MessageBuilder.create("cmdHmsRingSent", hms, PREFIX)
                .addPlaceholderReplace("%PLAYER%", toRing.getName())
                .sendMessage(sender);

        scheduler.runTaskAsynchronously(hms, () -> {
            float ringHeight = 2.0f;
            boolean drop = true;
            for (int i = 0; i < 19; i++) {

                toRing.playSound(toRing.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, ringHeight);

                if (ringHeight == 2.0f) drop = true;
                else if (ringHeight == 0.5f) drop = false;

                if (drop) ringHeight -= 0.5f;
                else ringHeight += 0.5f;

                try {
                    Thread.sleep(110L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void rmHomeBlock() {

        if (args.length == 2) {

            try {
                HalfminerPlayer p = storage.getPlayer(args[1]);
                UUID playerUid = p.getUniqueId();
                storage.set("vote." + playerUid, Long.MAX_VALUE);

                MessageBuilder.create("cmdHmsHomeblockRemove", hms, PREFIX)
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .sendMessage(sender);
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, PREFIX);
            }
        } else showUsage();
    }

    private void runAction() {

        if (args.length < 3) {
            showUsage();
            return;
        }

        // build player param
        Player[] players = new Player[args.length - 2];
        for (int i = 2; i < args.length; i++) {
            Player selected = server.getPlayer(args[i]);
            if (selected != null) players[i - 2] = selected;
            else {
                MessageBuilder.create("playerNotOnline", PREFIX).sendMessage(sender);
                return;
            }
        }

        try {
            CustomAction action = new CustomAction(args[1], storage);
            boolean success = action.runAction(players);

            MessageBuilder.create(success ? "cmdHmsRunActionSuccess" : "cmdHmsRunActionExecuteError", hms, PREFIX)
                    .addPlaceholderReplace("%ACTIONNAME%", args[1])
                    .sendMessage(sender);

        } catch (CachingException e) {
            MessageBuilder.create("cmdHmsRunActionCacheError", hms, PREFIX)
                    .addPlaceholderReplace("%ACTIONNAME%", args[1])
                    .addPlaceholderReplace("%REASON%", e.getCleanReason())
                    .sendMessage(sender);
        }
    }

    private void searchHomes() {

        if (!isPlayer) {
            sendNotAPlayerMessage(PREFIX);
            return;
        }

        final Essentials ess = ((HanHooks) hms.getHandler(HandlerType.HOOKS)).getEssentialsHook();
        final ArrayList<String> homeMessages = new ArrayList<>();

        final World world = player.getLocation().getWorld();
        final int x = player.getLocation().getBlockX();
        final int z = player.getLocation().getBlockZ();
        final int checkRadius;

        if (args.length > 1) {
            int setTo;
            try {
                setTo = Integer.decode(args[1]);
            } catch (NumberFormatException e) {
                setTo = 5;
            }
            checkRadius = setTo;
        } else checkRadius = 5;

        scheduler.runTaskAsynchronously(hms, () -> {

            MessageBuilder.create("cmdHmsSearchhomesStarted", hms, PREFIX)
                    .addPlaceholderReplace("%RADIUS%", String.valueOf(checkRadius))
                    .sendMessage(player);

            for (UUID uuid : ess.getUserMap().getAllUniqueUsers()) {

                User user = ess.getUser(uuid);
                for (String homeName : user.getHomes()) {

                    try {
                        Location loc = user.getHome(homeName);
                        int xHome = loc.getBlockX();
                        int zHome = loc.getBlockZ();
                        if (loc.getWorld().equals(world)
                                && x - checkRadius < xHome && x + checkRadius > xHome
                                && z - checkRadius < zHome && z + checkRadius > zHome) {

                            homeMessages.add(MessageBuilder.create("cmdHmsSearchhomesResults", hms)
                                    .addPlaceholderReplace("%PLAYER%", user.getName())
                                    .addPlaceholderReplace("%HOMENAME%", homeName)
                                    .returnMessage());
                        }
                    } catch (Exception e) {
                        // Should not happen, as we know the home will exist
                        e.printStackTrace();
                    }
                }
            }

            if (homeMessages.size() == 0)
                MessageBuilder.create("cmdHmsSearchhomesNoneFound", hms, PREFIX).sendMessage(player);
            else homeMessages.forEach(player::sendMessage);
        });
    }

    private void updateSkill() {

        if (args.length < 2) {
            MessageBuilder.create("cmdHmsSkillUsage", hms, "Skilllevel").sendMessage(sender);
            return;
        }

        HalfminerPlayer p;
        try {
            p = storage.getPlayer(args[1]);
        } catch (PlayerNotFoundException e) {
            e.sendNotFoundMessage(sender, "Skilllevel");
            return;
        }

        int oldValue = p.getInt(DataType.SKILL_ELO);

        if (args.length > 2) {
            try {
                int modifier = Integer.decode(args[2]) - oldValue;
                ((ModSkillLevel) hms.getModule(ModuleType.SKILL_LEVEL)).updateSkill(p.getBase(), modifier);

                MessageBuilder.create("cmdHmsSkillUpdated", hms, "Skilllevel")
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .addPlaceholderReplace("%SKILLLEVEL%", p.getString(DataType.SKILL_LEVEL))
                        .addPlaceholderReplace("%OLDELO%", String.valueOf(oldValue))
                        .addPlaceholderReplace("%NEWELO%", p.getString(DataType.SKILL_ELO))
                        .sendMessage(sender);
            } catch (NumberFormatException e) {
                MessageBuilder.create("cmdHmsSkillUsage", hms, "Skilllevel").sendMessage(sender);
            }
        } else {
            MessageBuilder.create("cmdHmsSkillShow", hms, "Skilllevel")
                    .addPlaceholderReplace("%PLAYER%", p.getName())
                    .addPlaceholderReplace("%SKILLLEVEL%", p.getString(DataType.SKILL_LEVEL))
                    .addPlaceholderReplace("%ELO%", String.valueOf(oldValue))
                    .sendMessage(sender);
        }
    }

    private void xrayBypass() {

        ModAntiXray antiXray = (ModAntiXray) hms.getModule(ModuleType.ANTI_XRAY);

        if (args.length > 1) {

            OfflinePlayer p;
            try {
                p = storage.getPlayer(args[1]).getBase();
                MessageBuilder.create(antiXray.setBypassed(p) ?
                        "cmdHmsXrayBypassSet" : "cmdHmsXrayBypassUnset", hms, "AntiXRay")
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .sendMessage(sender);

            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "AntiXRay");
            }
        } else {

            String information = antiXray.getInformationString();

            if (information.length() > 0) {
                MessageBuilder.create("cmdHmsXrayShow", hms, "AntiXRay").sendMessage(sender);
                MessageBuilder.create(information, hms)
                        .setDirectString()
                        .sendMessage(sender);
                MessageBuilder.create("cmdHmsXrayLegend", hms).sendMessage(sender);
            } else {
                MessageBuilder.create("cmdHmsXrayShowEmpty", hms, "AntiXRay").sendMessage(sender);
            }
        }
    }

    private void showUsage() {
        MessageBuilder.create("cmdHmsUsage", hms, PREFIX)
                .addPlaceholderReplace("%VERSION%", hms.getDescription().getVersion())
                .sendMessage(sender);
    }
}
