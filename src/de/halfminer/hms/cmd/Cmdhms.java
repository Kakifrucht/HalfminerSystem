package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

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
            String motd = "";
            for (int i = 1; i < args.length; i++) {
                motd += args[i] + ' ';
            }
            motd = motd.substring(0, motd.length() - 1);
            hms.getMotd().updateMotd(motd);
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsMotdUpdated", true, "%PREFIX%", "Hinweis", "%NEWMOTD%", motd));
        }
    }

    private void reload(CommandSender sender) {
        hms.loadConfig();
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandHmsConfigReloaded", true, "%PREFIX%", "Hinweis"));
    }

}
