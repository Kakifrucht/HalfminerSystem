package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdhome extends HalfminerCommand {

    public Cmdhome() {
        this.permission = "hms.home";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (sender instanceof Player) {

            Player player = (Player) sender;
            String command = "ehome" + ' ' + Language.arrayToString(args, 0, false);

            if (player.hasPermission("hms.moderator") || player.hasPermission("essentials.home"))
                server.dispatchCommand(player, command);
            else if (storage.getLong("vote." + player.getUniqueId().toString()) > (System.currentTimeMillis() / 1000)
                    || storage.getPlayer(player).getInt(DataType.TIME_ONLINE) < 18000
                    || storage.getInt("vote.ip" + player.getAddress().getAddress().toString().replace('.', 'i').substring(1)) > 1) {

                player.addAttachment(hms, "essentials.home", true, 0);
                server.dispatchCommand(player, command);

            } else {

                player.sendMessage(Language.getMessagePlaceholders("commandHomeDenied", true, "%PREFIX%", "Home",
                        "%PLAYER%", player.getName()));

                hms.getLogger().info(Language.getMessagePlaceholders("commandHomeDeniedLog", false, "%PLAYER%", player.getName()));
            }

        }

    }
}
