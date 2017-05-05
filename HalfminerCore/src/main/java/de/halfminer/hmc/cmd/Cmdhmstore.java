package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.exceptions.PlayerNotFoundException;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;

/**
 * - Edit HalfminerSystem storage
 * - Set, get and delete variables
 * - Check if playerdata is being edited
 * - Save to disk
 */
@SuppressWarnings("unused")
public class Cmdhmstore extends HalfminerCommand {

    private String path;
    private HalfminerPlayer player = null;
    private DataType type = null;

    public Cmdhmstore() {
        this.permission = "hmc.admin";
    }

    @Override
    public void execute() {

        if (args.length == 0) {
            MessageBuilder.create("cmdHmstoreUsage", hmc, "Info").sendMessage(sender);
            return;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("save")) {
            scheduler.runTaskAsynchronously(hmc, () -> {
                coreStorage.saveConfig();
                storage.saveConfig();
                MessageBuilder.create("cmdHmstoreSave", hmc, "Info").sendMessage(sender);
            });
            return;
        }

        if (args.length > 1) {

            path = args[1];

            // check if we edit/read a player
            String[] playerSplit = path.toLowerCase().split("[.]");
            if (playerSplit.length > 1) {

                try {
                    player = storage.getPlayer(playerSplit[0]);
                    for (DataType typeQuery : DataType.values()) {
                        if (typeQuery.toString().equalsIgnoreCase(playerSplit[1])) {
                            type = typeQuery;
                            break;
                        }
                    }

                    if (type == null) {
                        player = null;
                    } else {
                        path = "" + ChatColor.GOLD + ChatColor.ITALIC + playerSplit[0]
                                + ChatColor.GRAY + ChatColor.ITALIC + '.' + playerSplit[1];
                    }

                } catch (PlayerNotFoundException ignored) {}
            }

            if (args.length > 2 && args[0].equalsIgnoreCase("set")) {

                String setTo = Utils.arrayToString(args, 2, false);
                set(setTo);
                MessageBuilder.create("cmdHmstoreSet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", setTo)
                        .sendMessage(sender);
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setint")) {

                int setTo;
                try {
                    setTo = Integer.decode(args[2]);
                } catch (NumberFormatException e) {
                    MessageBuilder.create("cmdHmstoreSetError", hmc, "Info").sendMessage(sender);
                    return;
                }

                set(setTo);
                MessageBuilder.create("cmdHmstoreSet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", String.valueOf(setTo))
                        .sendMessage(sender);
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setbool")) {

                boolean setTo = Boolean.parseBoolean(args[2]);
                set(setTo);
                MessageBuilder.create("cmdHmstoreSet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", String.valueOf(setTo))
                        .sendMessage(sender);
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setdouble")) {

                double setTo = Double.parseDouble(args[2]);
                set(setTo);
                MessageBuilder.create("cmdHmstoreSet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", String.valueOf(setTo))
                        .sendMessage(sender);
            } else if (args[0].equalsIgnoreCase("get")) {

                String value;
                if (player == null) {
                    value = coreStorage.getString(path);
                } else {
                    value = player.getString(type);
                }
                MessageBuilder.create("cmdHmstoreGet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", value)
                        .sendMessage(sender);
            } else if (args[0].equalsIgnoreCase("remove")) {

                if (player != null) {
                    showUsage();
                    return;
                }

                set(null);
                storage.set(path, null); // also clear central storage of HalfminerSystem
                MessageBuilder.create("cmdHmstoreRemove", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .sendMessage(sender);
            } else showUsage();
        } else showUsage();
    }

    private void showUsage() {
        MessageBuilder.create("cmdHmstoreUsage", hmc, "Info").sendMessage(sender);
    }

    private void set(Object setTo) {
        if (player != null) {
            player.set(type, setTo);
        } else {
            coreStorage.set(path, setTo);
        }
    }
}
