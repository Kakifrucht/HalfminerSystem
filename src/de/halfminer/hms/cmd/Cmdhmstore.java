package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;

/**
 * - Edit HalfminerSystem storage
 * - Set, get and delete variables
 * - Check if playerdata is being edited
 * - Save to disk
 */
@SuppressWarnings("unused")
public class Cmdhmstore extends HalfminerCommand {

    private HalfminerPlayer player = null;
    private DataType type = null;

    public Cmdhmstore() {
        this.permission = "hms.admin";
    }

    @Override
    public void execute() {

        if (args.length > 1) {

            String path = args[1].toLowerCase();
            String[] split = path.split("[.]");

            // Check if we edit/view a player
            if (split.length > 1) {

                try {

                    player = storage.getPlayer(split[0]);
                    for (DataType typeQuery : DataType.values()) {
                        if (typeQuery.toString().equalsIgnoreCase(split[1])) {
                            type = typeQuery;
                            break;
                        }
                    }
                    if (type == null) player = null;
                    else path = ChatColor.GOLD.toString()
                            + ChatColor.ITALIC + split[0] + ChatColor.GRAY + ChatColor.ITALIC + '.' + split[1];

                } catch (PlayerNotFoundException ignored) {
                }
            }

            if (args.length > 2 && args[0].equalsIgnoreCase("set")) {

                String setTo = Language.arrayToString(args, 2, false);
                set(path, setTo);
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreSet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", setTo));
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setint")) {

                int setTo;
                try {
                    setTo = Integer.decode(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreSetError", true, "%PREFIX%", "Info"));
                    return;
                }

                set(path, setTo);
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreSet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", String.valueOf(setTo)));
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setbool")) {

                boolean setTo = Boolean.parseBoolean(args[2]);
                set(path, setTo);
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreSet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", String.valueOf(setTo)));
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setdouble")) {

                double setTo = Double.parseDouble(args[2]);
                set(path, setTo);
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreSet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", String.valueOf(setTo)));
            } else if (args[0].equalsIgnoreCase("get")) {

                String value;
                if (player == null) value = storage.getString(path);
                else value = player.getString(type);
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreGet", true, "%PREFIX%", "Info",
                        "%PATH%", path, "%VALUE%", value));
            } else if (args[0].equalsIgnoreCase("remove")) {

                set(path, null);
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreRemove", true, "%PREFIX%", "Info",
                        "%PATH%", path));
            } else {

                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreUsage", true, "%PREFIX%", "Info"));
            }
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("save")) {

            scheduler.runTaskAsynchronously(hms, () -> {
                storage.saveConfig();
                sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreSave", true, "%PREFIX%", "Info"));
            });
        } else sender.sendMessage(Language.getMessagePlaceholders("cmdHmstoreUsage", true, "%PREFIX%", "Info"));
    }

    private void set(String path, Object setTo) {
        if (player != null) player.set(type, setTo);
        else storage.set(path, setTo);
    }
}
