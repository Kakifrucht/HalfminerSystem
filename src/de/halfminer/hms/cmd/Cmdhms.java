package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

@SuppressWarnings("unused")
public class Cmdhms extends BaseCommand {

    public Cmdhms() {
        this.permission = "hms.moderator";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "rename":
                    renameItem(sender, args);
                    return;
                case "rmhomeblock":
                    rmHomeBlock(sender, args);
                    return;
                case "updateskill":
                    updateSkill(sender, args);
                    return;
                case "ring":
                    ringPlayer(sender, args);
                    return;
                case "reload":
                    reload(sender);
                    return;
            }
        }
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Info"));
    }

    private void renameItem(CommandSender sender, String[] args) {
        if (sender instanceof Player) {

            Player player = (Player) sender;
            ItemStack item = player.getItemInHand();

            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRenameFailed", true, "%PREFIX%", "Info"));
                return;
            }

            String newName = ChatColor.RESET.toString();
            if (args.length > 1) newName = Language.arrayToString(args, 1, true);

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(newName);
            item.setItemMeta(meta);
            player.updateInventory();

            player.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRenameDone", true, "%PREFIX%",
                    "Info", "%NAME%", newName));

        } else sender.sendMessage(Language.getMessage("notAPlayer"));
    }

    private void rmHomeBlock(CommandSender sender, String[] args) {

        if (args.length == 2) {
            try {
                UUID playerUid = storage.getUUID(args[1]);
                storage.set("vote." + playerUid, Long.MAX_VALUE);
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsHomeblockRemove", true, "%PREFIX%", "Info",
                        "%PLAYER%", hms.getServer().getOfflinePlayer(playerUid).getName()));
            } catch (PlayerNotFoundException e) {
                sender.sendMessage(Language.getMessagePlaceholderReplace("playerDoesNotExist", true, "%PREFIX%", "Info"));
            }

        } else
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Info"));
    }

    private void updateSkill(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
            return;
        }

        Player player = hms.getServer().getPlayer(args[1]);

        if (player == null) {
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
            return;
        }

        int oldValue = storage.getStatsInt(player, StatsType.SKILL_ELO);
        int modifier = -oldValue;

        if (args.length > 2) {
            try {
                modifier += Integer.decode(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsSkillUsage", true, "%PREFIX%", "Skilllevel"));
                return;
            }
        }

        hms.getModSkillLevel().updateSkill(player, modifier);

        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsSkillUpdated", true, "%PREFIX%", "Skilllevel",
                "%PLAYER%", player.getName(), "%SKILLLEVEL%", String.valueOf(storage.getStatsInt(player, StatsType.SKILL_LEVEL)),
                "%OLDELO%", String.valueOf(oldValue), "%NEWELO%", String.valueOf(storage.getStatsInt(player, StatsType.SKILL_ELO))));
    }

    private void ringPlayer(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Info"));
            return;
        }

        final Player toRing = hms.getServer().getPlayer(args[1]);
        String senderName = sender.getName();
        if (senderName.equals("CONSOLE")) senderName = Language.getMessage("consoleName");

        if (toRing == null) {

            sender.sendMessage(Language.getMessagePlaceholderReplace("playerNotOnline", true, "%PREFIX%", "Info"));

        } else {

            TitleSender.sendTitle(toRing, Language.getMessagePlaceholderReplace("commandHmsRingTitle", false, "%PLAYER%", senderName));
            toRing.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRingMessage", true, "%PREFIX%", "Info",
                    "%PLAYER%", senderName));

            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRingSent", true, "%PREFIX%", "Info",
                    "%PLAYER%", toRing.getName()));

            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    float ringHeight = 2.0f;
                    boolean drop = true;
                    for (int i = 0; i < 19; i++) {

                        toRing.playSound(toRing.getLocation(), Sound.ORB_PICKUP, 1.0f, ringHeight);

                        if (ringHeight == 2.0f) drop = true;
                        else if (ringHeight == 0.5f) drop = false;

                        if (drop) ringHeight -= 0.5f;
                        else ringHeight += 0.5f;

                        try {
                            Thread.sleep(110l);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void reload(CommandSender sender) {
        hms.loadConfig();
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsConfigReloaded", true, "%PREFIX%", "Info"));
    }

}
