package de.halfminer.hms.cmd;

import de.halfminer.hms.modules.ModStorage;
import de.halfminer.hms.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdhome extends BaseCommand {

    public Cmdhome() {
        this.permission = "hms.home";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender instanceof Player) {

            Player player = (Player) sender;
            ModStorage storage = hms.getModStorage();
            String command = "ehome" + ' ' + Language.arrayToString(args, 0, false);

            if (player.hasPermission("essentials.home")) hms.getServer().dispatchCommand(player, command);
            else if (storage.getLong("vote." + player.getUniqueId().toString()) > (System.currentTimeMillis() / 1000)
                    || storage.getPlayerInt(player, "timeonline") < 18000
                    || storage.getInt("vote.ip" + player.getAddress().getAddress().toString().replace('.', 'i').substring(1)) == 2) {

                ConsoleCommandSender console = hms.getServer().getConsoleSender();
                hms.getServer().dispatchCommand(console, "manuaddp " + player.getName() + " essentials.home");
                hms.getServer().dispatchCommand(player, command);
                hms.getServer().dispatchCommand(console, "manudelp " + player.getName() + " essentials.home");
            } else {

                player.sendMessage(Language.getMessagePlaceholderReplace("commandHomeDenied", true, "%PREFIX%", "Home",
                        "%PLAYER%", player.getName()));

                hms.getLogger().info(Language.getMessagePlaceholderReplace("commandHomeDeniedLog", false, "%PLAYER%", player.getName()));
            }

        }

    }
}
