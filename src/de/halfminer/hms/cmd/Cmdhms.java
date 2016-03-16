package de.halfminer.hms.cmd;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.modules.ModSkillLevel;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.ModuleType;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class Cmdhms extends HalfminerCommand {

    private CommandSender sender;
    private String[] args;

    public Cmdhms() {
        this.permission = "hms.moderator";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        this.sender = sender;
        this.args = args;

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "rename":
                    renameItem();
                    return;
                case "rmhomeblock":
                    rmHomeBlock();
                    return;
                case "updateskill":
                    updateSkill();
                    return;
                case "ring":
                    ringPlayer();
                    return;
                case "searchhomes":
                    searchHomes();
                    return;
                case "reload":
                    reload();
                    return;
            }
        }
        sender.sendMessage(Language.getMessagePlaceholders("commandHmsUsage", true, "%PREFIX%", "HMS"));
    }

    private void renameItem() {

        if (sender instanceof Player) {

            Player player = (Player) sender;

            if (args.length > 1) {

                ItemStack item = player.getInventory().getItemInMainHand();

                if (item == null || item.getType() == Material.AIR) {
                    player.sendMessage(Language.getMessagePlaceholders("commandHmsRenameFailed", true, "%PREFIX%", "HMS"));
                    return;
                }

                ItemMeta meta = item.getItemMeta();

                //default parameters, clear item name if not specified but keep lore
                String newName = meta.getDisplayName();
                List<String> lore = meta.getLore();

                //item name must start at argument 1 only if it is not the -lore flag
                if (!args[1].equalsIgnoreCase("-lore")) {

                    if (args[1].equalsIgnoreCase("reset")) newName = "";
                    else {

                        newName = Language.arrayToString(args, 1, true);
                        //cut new string at -lore
                        for (int i = 0; i < newName.length(); i++) {
                            if (newName.substring(i).toLowerCase().startsWith("-lore")) {
                                newName = newName.substring(0, i);
                                break;
                            }
                        }
                        //cut spaces at the end of the name
                        while (newName.endsWith(" ")) {
                            newName = newName.substring(0, newName.length() - 1);
                        }
                    }
                }

                //iterate over args and check if lore flag is set
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("-lore")) {
                        //check if new lore was specified, else just clear it
                        if (args.length > i + 1) {
                            //split lines of lore at | character, set the lore list
                            String[] loreToArray = Language.arrayToString(args, i + 1, true).split("[|]");
                            lore = Arrays.asList(loreToArray);
                            break;
                        } else {
                            if (lore != null) lore.clear();
                        }
                    }
                }

                //update item
                meta.setDisplayName(newName);
                meta.setLore(lore);
                item.setItemMeta(meta);
                player.updateInventory();

                if (newName == null) newName = "";

                player.sendMessage(Language.getMessagePlaceholders("commandHmsRenameDone", true, "%PREFIX%",
                        "HMS", "%NAME%", newName));
            } else {
                sender.sendMessage(Language.getMessagePlaceholders("commandHmsUsage", true, "%PREFIX%", "HMS"));
            }

        } else sender.sendMessage(Language.getMessage("notAPlayer"));
    }

    private void rmHomeBlock() {

        if (args.length == 2) {
            try {
                UUID playerUid = storage.getUUID(args[1]);
                storage.set("sys.vote." + playerUid, Long.MAX_VALUE);
                sender.sendMessage(Language.getMessagePlaceholders("commandHmsHomeblockRemove", true, "%PREFIX%", "HMS",
                        "%PLAYER%", hms.getServer().getOfflinePlayer(playerUid).getName()));
            } catch (PlayerNotFoundException e) {
                sender.sendMessage(Language.getMessagePlaceholders("playerDoesNotExist", true, "%PREFIX%", "HMS"));
            }

        } else
            sender.sendMessage(Language.getMessagePlaceholders("commandHmsUsage", true, "%PREFIX%", "HMS"));
    }

    private void updateSkill() {

        if (args.length < 2) {
            sender.sendMessage(Language.getMessagePlaceholders("commandHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
            return;
        }

        Player player = hms.getServer().getPlayer(args[1]);

        if (player == null) {
            sender.sendMessage(Language.getMessagePlaceholders("commandHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
            return;
        }

        int oldValue = storage.getStatsInt(player, StatsType.SKILL_ELO);
        int modifier = -oldValue;

        if (args.length > 2) {
            try {
                modifier += Integer.decode(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Language.getMessagePlaceholders("commandHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
                return;
            }
        }

        ((ModSkillLevel) hms.getModule(ModuleType.SKILL_LEVEL)).updateSkill(player, modifier);

        sender.sendMessage(Language.getMessagePlaceholders("commandHmsSkillUpdated", true, "%PREFIX%", "Skilllevel",
                "%PLAYER%", player.getName(), "%SKILLLEVEL%", String.valueOf(storage.getStatsInt(player, StatsType.SKILL_LEVEL)),
                "%OLDELO%", String.valueOf(oldValue), "%NEWELO%", String.valueOf(storage.getStatsInt(player, StatsType.SKILL_ELO))));
    }

    private void ringPlayer() {

        if (args.length < 2) {
            sender.sendMessage(Language.getMessagePlaceholders("commandHmsUsage", true, "%PREFIX%", "HMS"));
            return;
        }

        final Player toRing = hms.getServer().getPlayer(args[1]);
        String senderName = sender.getName();
        if (senderName.equals("CONSOLE")) senderName = Language.getMessage("consoleName");

        if (toRing == null) {

            sender.sendMessage(Language.getMessagePlaceholders("playerNotOnline", true, "%PREFIX%", "HMS"));

        } else {

            TitleSender.sendTitle(toRing, Language.getMessagePlaceholders("commandHmsRingTitle", false, "%PLAYER%", senderName));
            toRing.sendMessage(Language.getMessagePlaceholders("commandHmsRingMessage", true, "%PREFIX%", "HMS",
                    "%PLAYER%", senderName));

            sender.sendMessage(Language.getMessagePlaceholders("commandHmsRingSent", true, "%PREFIX%", "HMS",
                    "%PLAYER%", toRing.getName()));

            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
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
                }
            });
        }
    }

    private void searchHomes() {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Language.getMessage("notAPlayer"));
            return;
        }

        final Player player = (Player) sender;

        final Essentials ess = (Essentials) hms.getServer().getPluginManager().getPlugin("Essentials");
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

        hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
            @Override
            public void run() {

                player.sendMessage(Language.getMessagePlaceholders("commandHmsSearchhomesStarted", true, "%PREFIX%", "HMS",
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
                                homeMessages.add(Language.getMessagePlaceholders("commandHmsSearchhomesResults", false,
                                        "%PLAYER%", user.getName(), "%HOMENAME%", homeName));
                            }
                        } catch (Exception e) {
                            // Should not happen, as we know the home will exist
                            e.printStackTrace();
                        }
                    }
                }

                if (homeMessages.size() == 0) player.sendMessage(
                        Language.getMessagePlaceholders("commandHmsSearchhomesNoneFound", true, "%PREFIX%", "HMS"));
                else for (String message : homeMessages) player.sendMessage(message);
            }
        });
    }

    private void reload() {
        hms.loadConfig();
        sender.sendMessage(Language.getMessagePlaceholders("commandHmsConfigReloaded", true, "%PREFIX%", "HMS"));
    }

}
