package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hmc.module.ModAntiXray;
import de.halfminer.hmc.module.ModSkillLevel;
import de.halfminer.hms.cache.CustomAction;
import de.halfminer.hms.cache.CustomitemCache;
import de.halfminer.hms.cache.CustomtextCache;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.exceptions.CachingException;
import de.halfminer.hms.exceptions.ItemCacheException;
import de.halfminer.hms.exceptions.PlayerNotFoundException;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
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
 * - Remove a players /home block (rmhomeblock)
 * - Run an action defined in customactions.txt (runaction)
 * - Search for homes in a given radius, hooking into Essentials (searchhomes)
 * - Edit skillelo of player (updateskill)
 * - List all currently by antixray watched players (xraybypass)
 *   - Exempt a player from AntiXRay
 */
@SuppressWarnings("unused")
public class Cmdhmc extends HalfminerCommand {

    private static final String PREFIX = "HMC";

    public Cmdhmc() {
        this.permission = "hmc.moderator";
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
            MessageBuilder.create("cmdHmcCopySchematicNotSpecified", hmc, PREFIX).sendMessage(sender);
            return;
        }

        String destinationPath = hmc.getConfig().getString("command.hmc.remoteSchematicPath", "");
        if (destinationPath.length() == 0 || destinationPath.equals("/put/your/path/here")) {
            MessageBuilder.create("cmdHmcCopySchematicNotConfigured", hmc, PREFIX).sendMessage(sender);
            return;
        }

        File toCopy = new File("plugins/WorldEdit/schematics/", args[1] + ".schematic");
        File destination = new File(destinationPath, args[1] + ".schematic");

        // check if destination path exists first
        if (!Files.exists(new File(destinationPath).toPath())) {
            MessageBuilder.create("cmdHmcCopySchematicNotConfigured", hmc, PREFIX).sendMessage(sender);
            return;
        }

        if (toCopy.exists()) {

            if (!destination.exists()) {
                scheduler.runTaskAsynchronously(hmc, () -> {
                    try {
                        Files.copy(toCopy.toPath(), destination.toPath());
                        MessageBuilder.create("cmdHmcCopySchematicCopySuccess", hmc, PREFIX).sendMessage(sender);

                        // auto delete after 10 minutes
                        if (args.length > 2 && args[2].startsWith("auto")) {

                            MessageBuilder.create("cmdHmcCopySchematicDelete", hmc, PREFIX).sendMessage(sender);

                            scheduler.runTaskLaterAsynchronously(hmc, () -> {
                                deleteFile(toCopy);
                                deleteFile(destination);
                            }, 12000L);
                        }
                    } catch (IOException e) {
                        MessageBuilder.create("cmdHmcCopySchematicCopyError", hmc, PREFIX).sendMessage(sender);
                        hmc.getLogger().log(Level.WARNING, "Could not copy schematic due", e);
                    }
                });
            } else MessageBuilder.create("cmdHmcCopySchematicAlreadyExists", hmc, PREFIX).sendMessage(sender);
        } else MessageBuilder.create("cmdHmcCopySchematicDoesntExist", hmc, PREFIX).sendMessage(sender);
    }

    private void deleteFile(File toDelete) {

        boolean success = false;

        try {
            Files.delete(toDelete.toPath());
            success = true;
        } catch (Exception e) {
            hmc.getLogger().log(Level.WARNING, "Could not delete file", e);
        }

        MessageBuilder.create(success ? "cmdHmcCopySchematicDeleted" : "cmdHmcCopySchematicDeletedError", hmc)
                .addPlaceholderReplace("%PATH%", toDelete.toPath().toString())
                .logMessage(success ? Level.INFO : Level.WARNING);
    }

    private void give() {

        CustomtextCache cache;
        try {
            cache = coreStorage.getCache("customitems.txt");
        } catch (CachingException e) {
            MessageBuilder.create("cmdCustomtextCacheParseError", hmc, PREFIX)
                    .addPlaceholderReplace("%ERROR%", e.getCleanReason())
                    .sendMessage(sender);
            return;
        }

        CustomitemCache itemCache = new CustomitemCache(cache);
        if (args.length < 3) {

            Set<String> allItems = itemCache.getAllItems();
            if (!allItems.isEmpty()) {

                StringBuilder allItemString = new StringBuilder();
                for (String itemKey : allItems) allItemString.append(itemKey).append(" ");
                allItemString = new StringBuilder(allItemString.substring(0, allItemString.length() - 1));

                MessageBuilder.create("cmdHmcGiveList", hmc, PREFIX)
                        .addPlaceholderReplace("%LIST%", allItemString.toString())
                        .sendMessage(sender);

            } else MessageBuilder.create("cmdHmcGiveListNone", hmc, PREFIX).sendMessage(sender);

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
            MessageBuilder.create("cmdHmcGiveSuccessful", hmc, PREFIX)
                    .addPlaceholderReplace("%PLAYER%", giveTo.getName())
                    .addPlaceholderReplace("%ITEM%", itemName)
                    .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                    .sendMessage(sender);
        } catch (ItemCacheException e) {

            if (e.getReason().equals(ItemCacheException.Reason.INVENTORY_FULL)) {

                int totalLost = 0;
                for (ItemStack lost : e.getNotGivenItems().values()) {
                    totalLost += lost.getAmount();
                }
                MessageBuilder.create("cmdHmcGiveInventoryFull", hmc, PREFIX)
                        .addPlaceholderReplace("%PLAYER%", giveTo.getName())
                        .addPlaceholderReplace("%ITEM%", itemName)
                        .addPlaceholderReplace("%AMOUNT%", String.valueOf(totalLost))
                        .sendMessage(sender);
            } else if (e.getReason().equals(ItemCacheException.Reason.ITEM_NOT_FOUND)) {
                MessageBuilder.create("cmdHmcGiveItemNotFound", hmc, PREFIX)
                        .addPlaceholderReplace("%ITEM%", itemName)
                        .sendMessage(sender);
            } else {
                MessageBuilder.create("cmdHmcGiveError", hmc, PREFIX)
                        .addPlaceholderReplace("%REASON%", e.getCleanReason())
                        .sendMessage(sender);
            }
        }
    }

    private void reload() {
        hms.getHalfminerManager().reloadOcurred(hmc);
        MessageBuilder.create("pluginReloaded", "HMC")
                .addPlaceholderReplace("%PLUGINNAME%", hmc.getName())
                .sendMessage(sender);
    }

    private void renameItem() {

        if (!isPlayer) {
            sendNotAPlayerMessage(PREFIX);
            return;
        }

        if (args.length > 1) {

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {
                MessageBuilder.create("cmdHmcRenameFailed", hmc, PREFIX).sendMessage(player);
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

            MessageBuilder.create("cmdHmcRenameDone", hmc, PREFIX)
                    .addPlaceholderReplace("%NAME%", newName)
                    .sendMessage(player);
        } else showUsage();
    }

    private void rmHomeBlock() {

        if (args.length == 2) {

            try {
                HalfminerPlayer p = storage.getPlayer(args[1]);
                UUID playerUid = p.getUniqueId();
                coreStorage.set("vote." + playerUid, Long.MAX_VALUE);

                MessageBuilder.create("cmdHmcHomeblockRemove", hmc, PREFIX)
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
            CustomAction action = new CustomAction(args[1], coreStorage);
            boolean success = action.runAction(players);

            MessageBuilder.create(success ? "cmdHmcRunActionSuccess" : "cmdHmcRunActionExecuteError", hmc, PREFIX)
                    .addPlaceholderReplace("%ACTIONNAME%", args[1])
                    .sendMessage(sender);

        } catch (CachingException e) {
            MessageBuilder.create("cmdHmcRunActionCacheError", hmc, PREFIX)
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

        final IEssentials ess = hms.getHooksHandler().getEssentialsHook();
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

        MessageBuilder.create("cmdHmcSearchhomesStarted", hmc, PREFIX)
                .addPlaceholderReplace("%RADIUS%", String.valueOf(checkRadius))
                .sendMessage(player);

        scheduler.runTaskAsynchronously(hmc, () -> {

            for (UUID uuid : ess.getUserMap().getAllUniqueUsers()) {

                IUser user = ess.getUser(uuid);
                for (String homeName : user.getHomes()) {

                    try {
                        Location loc = user.getHome(homeName);
                        int xHome = loc.getBlockX();
                        int zHome = loc.getBlockZ();
                        if (loc.getWorld().equals(world)
                                && x - checkRadius < xHome && x + checkRadius > xHome
                                && z - checkRadius < zHome && z + checkRadius > zHome) {

                            homeMessages.add(MessageBuilder.create("cmdHmcSearchhomesResults", hmc)
                                    .addPlaceholderReplace("%PLAYER%", user.getName())
                                    .addPlaceholderReplace("%HOMENAME%", homeName)
                                    .returnMessage());
                        }
                    } catch (Exception ignored) {
                        // Should not occur, as we know the home will exist
                    }
                }
            }

            if (homeMessages.size() == 0)
                MessageBuilder.create("cmdHmcSearchhomesNoneFound", hmc, PREFIX).sendMessage(player);
            else homeMessages.forEach(player::sendMessage);
        });
    }

    private void updateSkill() {

        if (args.length < 2) {
            MessageBuilder.create("cmdHmcSkillUsage", hmc, "Skilllevel").sendMessage(sender);
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
                ((ModSkillLevel) hmc.getModule(ModuleType.SKILL_LEVEL)).updateSkill(p.getBase(), modifier);

                MessageBuilder.create("cmdHmcSkillUpdated", hmc, "Skilllevel")
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .addPlaceholderReplace("%SKILLLEVEL%", p.getString(DataType.SKILL_LEVEL))
                        .addPlaceholderReplace("%OLDELO%", String.valueOf(oldValue))
                        .addPlaceholderReplace("%NEWELO%", p.getString(DataType.SKILL_ELO))
                        .sendMessage(sender);
            } catch (NumberFormatException e) {
                MessageBuilder.create("cmdHmcSkillUsage", hmc, "Skilllevel").sendMessage(sender);
            }
        } else {
            MessageBuilder.create("cmdHmcSkillShow", hmc, "Skilllevel")
                    .addPlaceholderReplace("%PLAYER%", p.getName())
                    .addPlaceholderReplace("%SKILLLEVEL%", p.getString(DataType.SKILL_LEVEL))
                    .addPlaceholderReplace("%ELO%", String.valueOf(oldValue))
                    .sendMessage(sender);
        }
    }

    private void xrayBypass() {

        ModAntiXray antiXray = (ModAntiXray) hmc.getModule(ModuleType.ANTI_XRAY);

        if (args.length > 1) {

            OfflinePlayer p;
            try {
                p = storage.getPlayer(args[1]).getBase();
                MessageBuilder.create(antiXray.setBypassed(p) ?
                        "cmdHmcXrayBypassSet" : "cmdHmcXrayBypassUnset", hmc, "AntiXRay")
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .sendMessage(sender);

            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "AntiXRay");
            }
        } else {

            String information = antiXray.getInformationString();

            if (information.length() > 0) {
                MessageBuilder.create("cmdHmcXrayShow", hmc, "AntiXRay").sendMessage(sender);
                MessageBuilder.create(information, hmc)
                        .setDirectString()
                        .sendMessage(sender);
                MessageBuilder.create("cmdHmcXrayLegend", hmc).sendMessage(sender);
            } else {
                MessageBuilder.create("cmdHmcXrayShowEmpty", hmc, "AntiXRay").sendMessage(sender);
            }
        }
    }

    private void showUsage() {
        MessageBuilder.create("cmdHmcUsage", hmc, PREFIX)
                .addPlaceholderReplace("%VERSION%", hmc.getDescription().getVersion())
                .addPlaceholderReplace("%SYSTEMVERSION%", hms.getDescription().getVersion())
                .sendMessage(sender);
    }
}
