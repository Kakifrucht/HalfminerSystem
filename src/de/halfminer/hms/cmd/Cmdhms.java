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
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
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

    private CommandSender sender;
    private Player p;
    private String[] args;

    private static final String prefix = "HMS";

    public Cmdhms() {
        this.permission = "hms.moderator";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        this.sender = sender;
        if (sender instanceof Player) p = (Player) sender;
        this.args = args;

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "copyschematic":
                    //copySchematic();
                    sender.sendMessage("Not yet implemented");
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

        //TODO

        String path = hms.getConfig().getString("command.hms.remoteSchematicPath", "");

        if (path.length() > 0) {
            System.out.println( new File("./plugins/WorldGuard/schematics/", "").getAbsolutePath());
            //File toCopy = new File(server.getF)
        }

    }

    private void reload() {
        hms.loadConfig();
        sender.sendMessage(Language.getMessagePlaceholders("cmdHmsConfigReloaded", true, "%PREFIX%", prefix));
    }

    private void renameItem() {

        if (p == null) {
            sender.sendMessage(Language.getMessage("notAPlayer"));
            return;
        }

        if (args.length > 1) {

            ItemStack item = p.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {
                p.sendMessage(Language.getMessagePlaceholders("cmdHmsRenameFailed", true, "%PREFIX%", prefix));
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
            p.updateInventory();

            if (newName == null) newName = "";

            p.sendMessage(Language.getMessagePlaceholders("cmdHmsRenameDone", true, "%PREFIX%",
                    prefix, "%NAME%", newName));
        } else showUsage();
    }

    private void ringPlayer() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        final Player toRing = server.getPlayer(args[1]);
        String senderName = sender.getName();
        if (senderName.equals("CONSOLE")) senderName = Language.getMessage("consoleName");

        if (toRing == null) {

            sender.sendMessage(Language.getMessagePlaceholders("playerNotOnline", true, "%PREFIX%", prefix));

        } else {

            ((HanTitles) hms.getHandler(HandlerType.TITLES)).sendTitle(toRing,
                    Language.getMessagePlaceholders("cmdHmsRingTitle", false, "%PLAYER%", senderName));
            toRing.sendMessage(Language.getMessagePlaceholders("cmdHmsRingMessage", true, "%PREFIX%", prefix,
                    "%PLAYER%", senderName));

            sender.sendMessage(Language.getMessagePlaceholders("cmdHmsRingSent", true, "%PREFIX%", prefix,
                    "%PLAYER%", toRing.getName()));

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
    }

    private void rmHomeBlock() {

        if (args.length == 2) {
            try {
                HalfminerPlayer p = storage.getPlayer(args[1]);
                UUID playerUid = p.getUniqueId();
                storage.set("vote." + playerUid, Long.MAX_VALUE);
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmsHomeblockRemove", true, "%PREFIX%", prefix,
                        "%PLAYER%", p.getName()));
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, prefix);
            }

        } else showUsage();
    }

    private void searchHomes() {

        if (p == null) {
            sender.sendMessage(Language.getMessage("notAPlayer"));
            return;
        }

        final Player player = (Player) sender;

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

            player.sendMessage(Language.getMessagePlaceholders("cmdHmsSearchhomesStarted", true, "%PREFIX%", prefix,
                    "%RADIUS%", String.valueOf(checkRadius)));

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
                            homeMessages.add(Language.getMessagePlaceholders("cmdHmsSearchhomesResults", false,
                                    "%PLAYER%", user.getName(), "%HOMENAME%", homeName));
                        }
                    } catch (Exception e) {
                        // Should not happen, as we know the home will exist
                        e.printStackTrace();
                    }
                }
            }

            if (homeMessages.size() == 0) player.sendMessage(
                    Language.getMessagePlaceholders("cmdHmsSearchhomesNoneFound", true, "%PREFIX%", prefix));
            else homeMessages.forEach(player::sendMessage);
        });
    }

    private void updateSkill() {

        if (args.length < 2) {
            sender.sendMessage(Language.getMessagePlaceholders("cmdHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
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

                sender.sendMessage(Language.getMessagePlaceholders("cmdHmsSkillUpdated", true, "%PREFIX%", "Skilllevel",
                        "%PLAYER%", p.getName(), "%SKILLLEVEL%", p.getString(DataType.SKILL_LEVEL),
                        "%OLDELO%", String.valueOf(oldValue), "%NEWELO%", p.getString(DataType.SKILL_ELO)));
            } catch (NumberFormatException e) {
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
            }
        } else {
            sender.sendMessage(Language.getMessagePlaceholders("cmdHmsSkillShow", true, "%PREFIX%", "Skilllevel",
                    "%PLAYER%", p.getName(), "%SKILLLEVEL%", p.getString(DataType.SKILL_LEVEL),
                    "%ELO%", String.valueOf(oldValue)));
        }
    }

    private void xrayBypass() {

        ModAntiXray antiXray = (ModAntiXray) hms.getModule(ModuleType.ANTI_XRAY);

        if (args.length > 1) {

            OfflinePlayer p;
            try {

                p = storage.getPlayer(args[1]).getBase();

                if (antiXray.setBypassed(p))
                    sender.sendMessage(Language.getMessagePlaceholders("cmdHmsXrayBypassSet",
                            true, "%PREFIX%", "AntiXRay", "%PLAYER%", p.getName()));
                else sender.sendMessage(Language.getMessagePlaceholders("cmdHmsXrayBypassUnset",
                        true, "%PREFIX%", "AntiXRay", "%PLAYER%", p.getName()));

            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "AntiXRay");
            }
        } else {

            String toSend = Language.getMessagePlaceholders("cmdHmsXrayShowEmpty", true, "%PREFIX%", "AntiXRay");
            String information = antiXray.getInformationString();

            if (information.length() > 0)
                toSend = Language.getMessagePlaceholders("cmdHmsXrayShow", true, "%PREFIX%", "AntiXRay")
                        + "\n" + ChatColor.RESET + information + "\n" + ChatColor.RESET
                        + Language.getMessage("cmdHmsXrayLegend");

            sender.sendMessage(toSend);
        }
    }

    private void showUsage() {
        sender.sendMessage(Language.getMessagePlaceholders("cmdHmsUsage", true, "%PREFIX%", prefix,
                "%VERSION%", hms.getDescription().getVersion()));
    }
}
