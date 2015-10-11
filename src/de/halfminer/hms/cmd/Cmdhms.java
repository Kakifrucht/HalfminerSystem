package de.halfminer.hms.cmd;

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
                case "reload":
                    reload(sender);
                    return;
            }
        }
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsUsage", true, "%PREFIX%", "Hinweis"));
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

            player.sendMessage(Language.getMessagePlaceholderReplace("commandHmsRenameDone", true, "%PREFIX%", "Hinweis", "%NAME%", newName));

        } else sender.sendMessage(Language.getMessage("notAPlayer"));
    }

    private void reload(CommandSender sender) {
        hms.loadConfig();
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsConfigReloaded", true, "%PREFIX%", "Hinweis"));
    }

}
