package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Hinweis"));
    }

    private void renameItem(CommandSender sender, String[] args) {
        if (sender instanceof Player) {

            Player player = (Player) sender;
            ItemStack item = player.getItemInHand();

            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRenameFailed", true, "%PREFIX%", "Hinweis"));
                return;
            }

            String newName = ChatColor.RESET.toString();
            if (args.length > 1) newName = Language.arrayToString(args, 1, true);

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(newName);
            item.setItemMeta(meta);
            player.updateInventory();

            player.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRenameDone", true, "%PREFIX%",
                    "Hinweis", "%NAME%", newName));

        } else sender.sendMessage(Language.getMessage("notAPlayer"));
    }

    private void rmHomeBlock(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String playerUid = storage.getString("uid." + args[1].toLowerCase());
            if (playerUid.length() == 0) {
                sender.sendMessage(Language.getMessagePlaceholderReplace("playerDoesNotExist", true, "%PREFIX%", "Hinweis"));
                return;
            }
            storage.set("vote." + playerUid, Long.MAX_VALUE);
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsHomeblockRemove", true, "%PREFIX%", "Hinweis",
                    "%PLAYER%", storage.getString(playerUid + ".lastname")));
        } else
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Hinweis"));
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

        storage.getPlayerInt(player, "skillelo");
        int modifier = -storage.getPlayerInt(player, "skillelo");

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
                "%PLAYER%", player.getName(), "%SKILLLEVEL%", String.valueOf(storage.getPlayerInt(player, "skilllevel")),
                "%SKILLELO%", String.valueOf(storage.getPlayerInt(player, "skillelo"))));
    }

    private void ringPlayer(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Hinweis"));
            return;
        }

        final Player toRing = hms.getServer().getPlayer(args[1]);
        String senderName = sender.getName();
        if (senderName.equals("CONSOLE")) senderName = Language.getMessage("consoleName");

        if (toRing == null) {

            sender.sendMessage(Language.getMessagePlaceholderReplace("playerNotOnline", true, "%PREFIX%", "Hinweis"));

        } else {

            TitleSender.sendTitle(toRing, Language.getMessagePlaceholderReplace("commandHmsRingTitle", false, "%PLAYER%", senderName));
            toRing.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRingMessage", true, "%PREFIX%", "Hinweis",
                    "%PLAYER%", senderName));

            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRingSent", true, "%PREFIX%", "Hinweis",
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
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsConfigReloaded", true, "%PREFIX%", "Hinweis"));
    }

}
