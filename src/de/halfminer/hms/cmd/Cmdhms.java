package de.halfminer.hms.cmd;

import de.halfminer.hms.modules.ModStorage;
import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@SuppressWarnings("unused")
public class Cmdhms extends BaseCommand {

    public Cmdhms() {
        this.permission = "hms.admin";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "setmotd":
                    updateMotd(sender, args);
                    return;
                case "rename":
                    renameItem(sender, args);
                    return;
                case "rmhomeblock":
                    rmHomeBlock(sender, args);
                    return;
                case "updateskill":
                    updateSkill(sender, args);
                    return;
                case "reload":
                    reload(sender);
                    return;
            }
        }
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Hinweis"));
    }

    private void rmHomeBlock(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String playerUid = hms.getModStorage().getString("uid." + args[1].toLowerCase());
            if (playerUid.length() == 0) {
                sender.sendMessage(Language.getMessagePlaceholderReplace("playerDoesNotExist", true, "%PREFIX%", "Hinweis"));
                return;
            }
            hms.getModStorage().set("vote." + playerUid, Long.MAX_VALUE);
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsHomeblockRemove", true, "%PREFIX%", "Hinweis",
                    "%PLAYER%", hms.getModStorage().getString(playerUid + ".lastname")));
        } else sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Hinweis"));
    }

    private void updateMotd(CommandSender sender, String[] args) {
        if (args.length < 2)
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsMotdFailed", true, "%PREFIX%", "Hinweis"));
        else {
            String motd = Language.arrayToString(args, 1, false);
            hms.getModMotd().updateMotd(motd);
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsMotdUpdated", true, "%PREFIX%", "Hinweis", "%NEWMOTD%", ChatColor.translateAlternateColorCodes('&', motd)));
        }
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

        ModStorage storage = hms.getModStorage();
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

    private void reload(CommandSender sender) {
        hms.loadConfig();
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsConfigReloaded", true, "%PREFIX%", "Hinweis"));
    }

}
