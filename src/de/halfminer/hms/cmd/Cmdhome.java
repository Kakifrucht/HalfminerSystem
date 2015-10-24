package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdhome extends BaseCommand {

    public Cmdhome() {
        this.permission = "hms.home";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (sender instanceof Player) {

            Player player = (Player) sender;
            String command = "ehome" + ' ' + Language.arrayToString(args, 0, false);

            if (player.hasPermission("essentials.home")) hms.getServer().dispatchCommand(player, command);
            else if (storage.getLong("vote." + player.getUniqueId().toString()) > (System.currentTimeMillis() / 1000)
                    || storage.getStatsInt(player, StatsType.TIME_ONLINE) < 18000
                    || storage.getInt("vote.ip" + player.getAddress().getAddress().toString().replace('.', 'i').substring(1)) > 1) {

                player.addAttachment(hms, "essentials.home", true, 0);
                hms.getServer().dispatchCommand(player, command);

            } else {

                player.sendMessage(Language.getMessagePlaceholderReplace("commandHomeDenied", true, "%PREFIX%", "Home",
                        "%PLAYER%", player.getName()));

                hms.getLogger().info(Language.getMessagePlaceholderReplace("commandHomeDeniedLog", false, "%PLAYER%", player.getName()));
            }

        }

    }
}
