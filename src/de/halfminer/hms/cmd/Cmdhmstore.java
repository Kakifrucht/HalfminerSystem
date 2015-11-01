package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;

import java.util.UUID;

@SuppressWarnings("unused")
public class Cmdhmstore extends BaseCommand {

    public Cmdhmstore() {
        this.permission = "hms.admin";
    }

    @Override
    public void run(final CommandSender sender, String label, String[] args) {

        if (args.length > 2 && args[0].equalsIgnoreCase("set")) {

            String path = validatePlayer(args[1]);
            String setTo = Language.arrayToString(args, 2, false);
            storage.set(path, setTo);
            sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSet", true, "%PREFIX%", "Info",
                    "%PATH%", path, "%VALUE%", setTo));

        } else if (args.length == 3 && args[0].equalsIgnoreCase("setint")) {

            String path = validatePlayer(args[1]);
            int setTo;
            try {
                setTo = Integer.decode(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSetError", true, "%PREFIX%", "Info"));
                return;
            }

            storage.set(path, setTo);
            sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSet", true, "%PREFIX%", "Info",
                    "%PATH%", path, "%VALUE%", String.valueOf(setTo)));

        } else if (args.length == 3 && args[0].equalsIgnoreCase("setbool")) {

            String path = validatePlayer(args[1]);
            boolean setTo = Boolean.parseBoolean(args[2]);
            storage.set(path, setTo);
            sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSet", true, "%PREFIX%", "Info",
                    "%PATH%", path, "%VALUE%", String.valueOf(setTo)));

        } else if (args.length == 2 && args[0].equalsIgnoreCase("get")) {

            String path = validatePlayer(args[1]);
            String value = storage.getString(path);
            sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreGet", true, "%PREFIX%", "Info",
                    "%PATH%", path, "%VALUE%", value));

        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {

            String path = validatePlayer(args[1]);
            storage.set(path, null);
            sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreRemove", true, "%PREFIX%", "Info",
                    "%PATH%", path));

        } else if (args.length == 1 && args[0].equalsIgnoreCase("save")) {
            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    storage.saveConfig();
                    sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSave", true, "%PREFIX%", "Info"));
                }
            });
        } else showUsage(sender);
    }

    /**
     * This will check if a path specification contains a player name, and if it does, replace it with the players
     * uid. At the same time it will lowercase the input, since paths are always lowercase to prevent issues.
     *
     * @param path String containing the provided path
     * @return String with the parsed path, containing uid, or at least, lowercased path
     */
    private String validatePlayer(String path) {

        String toReturn = "";
        String[] split = path.toLowerCase().split("[.]");

        try {
            UUID playerUid = storage.getUUID(split[0]);
            split[0] = playerUid.toString();
            for (String str : split) toReturn += str + ".";
            return toReturn.substring(0, toReturn.length() - 1);
        } catch (PlayerNotFoundException e) {
            return path.toLowerCase();
        }

    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreUsage", true, "%PREFIX%", "Info"));
    }

}
