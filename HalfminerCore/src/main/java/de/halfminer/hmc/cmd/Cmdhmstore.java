package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.types.DataType;
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

    private HalfminerPlayer player = null;
    private DataType type = null;

    public Cmdhmstore() {
        this.permission = "hmc.admin";
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

                String setTo = Utils.arrayToString(args, 2, false);
                set(path, setTo);
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

                set(path, setTo);
                MessageBuilder.create("cmdHmstoreSet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", String.valueOf(setTo))
                        .sendMessage(sender);
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setbool")) {

                boolean setTo = Boolean.parseBoolean(args[2]);
                set(path, setTo);
                MessageBuilder.create("cmdHmstoreSet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", String.valueOf(setTo))
                        .sendMessage(sender);
            } else if (args.length > 2 && args[0].equalsIgnoreCase("setdouble")) {

                double setTo = Double.parseDouble(args[2]);
                set(path, setTo);
                MessageBuilder.create("cmdHmstoreSet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", String.valueOf(setTo))
                        .sendMessage(sender);
            } else if (args[0].equalsIgnoreCase("get")) {

                String value;
                if (player == null) value = coreStorage.getString(path);
                else value = player.getString(type);
                MessageBuilder.create("cmdHmstoreGet", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .addPlaceholderReplace("%VALUE%", value)
                        .sendMessage(sender);
            } else if (args[0].equalsIgnoreCase("remove")) {

                set(path, null);
                MessageBuilder.create("cmdHmstoreRemove", hmc, "Info")
                        .addPlaceholderReplace("%PATH%", path)
                        .sendMessage(sender);
            } else MessageBuilder.create("cmdHmstoreUsage", hmc, "Info").sendMessage(sender);
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("save")) {

            scheduler.runTaskAsynchronously(hmc, () -> {
                coreStorage.saveConfig();
                storage.saveConfig();
                MessageBuilder.create("cmdHmstoreSave", hmc, "Info").sendMessage(sender);
            });
        } else MessageBuilder.create("cmdHmstoreUsage", hmc, "Info").sendMessage(sender);
    }

    private void set(String path, Object setTo) {
        if (player != null) player.set(type, setTo);
        else coreStorage.set(path, setTo);
    }
}
