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

        if (args.length > 1) { // Set and get variables

            // Replace playername with UUID if found
            String path = args[1].toLowerCase();
            String[] split = path.split("[.]");
            // Only replace if the path is longer than 1 and it is not sys, as this var is reserved
            if (split.length > 1 && !split[0].equals("sys")) {

                try {
                    // Get UUID of player
                    UUID playerUid = storage.getUUID(split[0]);
                    split[0] = playerUid.toString();
                    for (String str : split) path += str + ".";
                    path = path.substring(0, path.length() - 1);
                } catch (PlayerNotFoundException e) {
                    // Player not found, use lowercase path
                }
            }

            if (args.length > 2 && args[0].equalsIgnoreCase("set")) {

                String setTo = Language.arrayToString(args, 2, false);
                storage.set(path, setTo);
                sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", setTo));

            } else if (args[0].equalsIgnoreCase("setint")) {

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

            } else if (args[0].equalsIgnoreCase("setbool")) {

                boolean setTo = Boolean.parseBoolean(args[2]);
                storage.set(path, setTo);
                sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", String.valueOf(setTo)));

            } else if (args[0].equalsIgnoreCase("setdouble")) {

                double setTo = Double.parseDouble(args[2]);
                storage.set(path, setTo);
                sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", String.valueOf(setTo)));

            } else if (args[0].equalsIgnoreCase("get")) {

                String value = storage.getString(path);
                sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreGet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", value));

            } else if (args[0].equalsIgnoreCase("remove")) {

                storage.set(path, null);
                sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreRemove", true, "%PREFIX%", "Info",
                        "%PATH%", path));

            }

        } else if (args.length > 0 && args[0].equalsIgnoreCase("save")) {

            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    storage.saveConfig();
                    sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreSave", true, "%PREFIX%", "Info"));
                }
            });
        } else sender.sendMessage(Language.getMessagePlaceholders("commandHmstoreUsage", true, "%PREFIX%", "Info"));
    }
}
