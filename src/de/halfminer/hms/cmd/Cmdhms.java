package de.halfminer.hms.cmd;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.handlers.HanHooks;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.modules.ModAntiXray;
import de.halfminer.hms.modules.ModSkillLevel;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * - Copy a WorldEdit schematic to another directory (copyschematic)
 * - Reload config (reload)
 * - Rename items, supports lore (rename)
 * - Ring players to get their attention (ring)
 * - Remove a players /home block (rmhomeblock)
 * - Search for homes in a given radius, hooking into Essentials (searchhomes)
 * - Edit skillelo of player (updateskill)
 * - List all currently by antixray watched players (xraybypass)
 *   - Exempt a player from AntiXRay
 */
@SuppressWarnings("unused")
public class Cmdhms extends HalfminerCommand {

    private static final String prefix = "HMS";

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
            MessageBuilder.create(hms, "cmdHmsCopySchematicNotSpecified", "HMS").sendMessage(sender);
            return;
        }

        String destinationPath = hms.getConfig().getString("command.hms.remoteSchematicPath", "");
        if (destinationPath.length() == 0 || destinationPath.equals("/put/your/path/here")) {
            MessageBuilder.create(hms, "cmdHmsCopySchematicNotConfigured", prefix).sendMessage(sender);
            return;
        }

        File toCopy = new File("plugins/WorldEdit/schematics/", args[1] + ".schematic");
        File destination = new File(destinationPath, args[1] + ".schematic");

        // check if destination path exists first
        if (!Files.exists(new File(destinationPath).toPath())) {
            MessageBuilder.create(hms, "cmdHmsCopySchematicNotConfigured", prefix).sendMessage(sender);
            return;
        }

        if (toCopy.exists()) {

            if (!destination.exists()) {
                scheduler.runTaskAsynchronously(hms, () -> {
                    try {
                        Files.copy(toCopy.toPath(), destination.toPath());
                        MessageBuilder.create(hms, "cmdHmsCopySchematicCopySuccess", prefix).sendMessage(sender);

                        // auto delete after 10 minutes
                        if (args.length > 2 && args[2].startsWith("auto")) {

                            MessageBuilder.create(hms, "cmdHmsCopySchematicDelete", prefix).sendMessage(sender);
                            scheduler.runTaskLaterAsynchronously(hms, () -> {

                                try {
                                    Files.delete(toCopy.toPath());
                                    MessageBuilder.create(hms, "cmdHmsCopySchematicDeleted")
                                            .addPlaceholderReplace("%PATH%", toCopy.toPath().toString())
                                            .logMessage(Level.INFO);
                                } catch (Exception e) {
                                    MessageBuilder.create(hms, "cmdHmsCopySchematicDeletedError")
                                            .addPlaceholderReplace("%PATH%", toCopy.toPath().toString())
                                            .logMessage(Level.WARNING);
                                    e.printStackTrace();
                                }

                                try {
                                    Files.delete(destination.toPath());
                                    MessageBuilder.create(hms, "cmdHmsCopySchematicDeleted")
                                            .addPlaceholderReplace("%PATH%", destination.toPath().toString())
                                            .logMessage(Level.INFO);
                                } catch (Exception e) {
                                    MessageBuilder.create(hms, "cmdHmsCopySchematicDeletedError")
                                            .addPlaceholderReplace("%PATH%", destination.toPath().toString())
                                            .logMessage(Level.WARNING);
                                    e.printStackTrace();
                                }
                            }, 12000L);
                        }
                    } catch (IOException e) {
                        MessageBuilder.create(hms, "cmdHmsCopySchematicCopyError", prefix).sendMessage(sender);
                        e.printStackTrace();
                    }
                });
            } else MessageBuilder.create(hms, "cmdHmsCopySchematicAlreadyExists", prefix).sendMessage(sender);
        } else MessageBuilder.create(hms, "cmdHmsCopySchematicDoesntExist", prefix).sendMessage(sender);
    }

    private void reload() {
        hms.loadConfig();
        MessageBuilder.create(hms, "cmdHmsConfigReloaded", prefix).sendMessage(sender);
    }

    private void renameItem() {

        if (!isPlayer) {
            sendNotAPlayerMessage("HMS");
            return;
        }

        if (args.length > 1) {

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {
                MessageBuilder.create(hms, "cmdHmsRenameFailed", prefix).sendMessage(player);
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

                    newName = Language.arrayToString(args, 1, true);
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
                        String[] loreToArray = Language.arrayToString(args, i + 1, true).split("[|]");
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

            MessageBuilder.create(hms, "cmdHmsRenameDone", prefix)
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
        String senderName = Language.getPlayername(sender);

        if (toRing == null) {
            MessageBuilder.create(hms, "playerNotOnline", prefix).sendMessage(sender);
            return;
        }

        ((HanTitles) hms.getHandler(HandlerType.TITLES)).sendTitle(toRing,
                MessageBuilder.create(hms, "cmdHmsRingTitle")
                        .addPlaceholderReplace("%PLAYER%", senderName)
                        .returnMessage());

        MessageBuilder.create(hms, "cmdHmsRingMessage", prefix)
                .addPlaceholderReplace("%PLAYER%", senderName)
                .sendMessage(toRing);

        MessageBuilder.create(hms, "cmdHmsRingSent", prefix)
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

                MessageBuilder.create(hms, "cmdHmsHomeblockRemove", prefix)
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .sendMessage(sender);
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, prefix);
            }
        } else showUsage();
    }

    private void searchHomes() {

        if (!isPlayer) {
            sendNotAPlayerMessage("HMS");
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

            MessageBuilder.create(hms, "cmdHmsSearchhomesStarted", prefix)
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

                            homeMessages.add(MessageBuilder.create(hms, "cmdHmsSearchhomesResults")
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
                MessageBuilder.create(hms, "cmdHmsSearchhomesNoneFound", prefix).sendMessage(player);
            else homeMessages.forEach(player::sendMessage);
        });
    }

    private void updateSkill() {

        if (args.length < 2) {
            MessageBuilder.create(hms, "cmdHmsSkillUsage", "Skilllevel").sendMessage(sender);
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

                MessageBuilder.create(hms, "cmdHmsSkillUpdated", "Skilllevel")
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .addPlaceholderReplace("%SKILLLEVEL%", p.getString(DataType.SKILL_LEVEL))
                        .addPlaceholderReplace("%OLDELO%", String.valueOf(oldValue))
                        .addPlaceholderReplace("%NEWELO%", p.getString(DataType.SKILL_ELO))
                        .sendMessage(sender);
            } catch (NumberFormatException e) {
                MessageBuilder.create(hms, "cmdHmsSkillUsage", "Skilllevel").sendMessage(sender);
            }
        } else {
            MessageBuilder.create(hms, "cmdHmsSkillShow", "Skilllevel")
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
                MessageBuilder.create(hms, antiXray.setBypassed(p) ?
                        "cmdHmsXrayBypassSet" : "cmdHmsXrayBypassUnset", "AntiXRay")
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .sendMessage(sender);

            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "AntiXRay");
            }
        } else {

            String information = antiXray.getInformationString();

            if (information.length() > 0) {
                MessageBuilder.create(hms, "cmdHmsXrayShow", "AntiXRay").sendMessage(sender);
                MessageBuilder.create(hms, information)
                        .setMode(MessageBuilder.MessageMode.DIRECT_STRING)
                        .sendMessage(sender);
                MessageBuilder.create(hms, "cmdHmsXrayLegend").sendMessage(sender);
            } else {
                MessageBuilder.create(hms, "cmdHmsXrayShowEmpty", "AntiXRay").sendMessage(sender);
            }
        }
    }

    private void showUsage() {
        MessageBuilder.create(hms, "cmdHmsUsage", prefix)
                .addPlaceholderReplace("%VERSION%", hms.getDescription().getVersion())
                .sendMessage(sender);
    }
}
