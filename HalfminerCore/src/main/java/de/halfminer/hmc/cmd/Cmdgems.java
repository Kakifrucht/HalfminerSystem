package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.cache.CustomAction;
import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * - Shows the players gem account
 * - Allows players to pay gems via /gems pay command
 *   - Executes custom action with amount as placeholder (optional)
 * - View and modify players gem accounts, if permission was granted
 *   - Notifies edited players if they are online
 * - All account changes are being logged
 */
@SuppressWarnings("unused")
public class Cmdgems extends HalfminerCommand {

    private HalfminerPlayer hPlayer = null;


    public Cmdgems() {
        this.permission = "hmc.gems";
    }

    @Override
    protected void execute() {

        if (isPlayer) {
            hPlayer = storage.getPlayer(player);
        }

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("pay") && args.length > 1) {

                if (hPlayer == null) {
                    sendNotAPlayerMessage("Gems");
                    return;
                }

                if (args.length < 2) {
                    sendGemsMessage();
                    return;
                }

                Integer amount;
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount < 1) {
                        sendGemsMessage();
                        return;
                    }

                    if (doAccountChange(-amount)) {

                        int gemsInAccount = hPlayer.getInt(DataType.GEMS);
                        MessageBuilder.create("cmdGemsPaySuccess", hmc, "Gems")
                                .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                                .addPlaceholderReplace("%TOTALAMOUNT%", String.valueOf(gemsInAccount))
                                .sendMessage(sender);

                        // execute custom action
                        String actionName = hmc.getConfig().getString("command.gems.payCustomAction", "");
                        if (actionName.length() > 0) {
                            try {

                                CustomAction action = new CustomAction(actionName, coreStorage);
                                action.addPlaceholderForNextRun("%AMOUNT%", String.valueOf(amount));
                                action.runAction(player);
                            } catch (CachingException e) {
                                MessageBuilder.create("cmdGemsPayActionInvalidLog", hmc)
                                        .addPlaceholderReplace("%ACTIONNAME%", actionName)
                                        .addPlaceholderReplace("%REASON%", e.getCleanReason())
                                        .logMessage(Level.WARNING);
                            }
                        }
                    } else {
                        MessageBuilder.create("cmdGemsPayNotEnoughGems", hmc, "Gems").sendMessage(sender);
                    }
                } catch (NumberFormatException e) {
                    MessageBuilder.create("cmdGemsPayInvalidParam", hmc, "Gems").sendMessage(sender);
                }

                return;
            } else if (player.hasPermission("hmc.gems.admin")) {

                try {
                    hPlayer = storage.getPlayer(args[0]);
                } catch (PlayerNotFoundException e) {
                    sendGemsMessage();
                    return;
                }

                // command: /gems <player> [give|take] [amount]
                if (args.length > 2
                        && (args[1].equalsIgnoreCase("take") || args[1].equalsIgnoreCase("give"))) {

                    boolean doTake = args[1].equalsIgnoreCase("take");

                    int amount;
                    try {
                        amount = Integer.parseInt(args[2]);
                        if (amount < 1) {
                            sendGemsMessage();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sendGemsMessage();
                        return;
                    }

                    if (!doAccountChange(doTake ? -amount : amount)) {
                        int totalAmount = hPlayer.getInt(DataType.GEMS);
                        MessageBuilder.create("cmdGemsAdminNotEnough", hmc, "Gems")
                                .addPlaceholderReplace("%PLAYER%", hPlayer.getName())
                                .addPlaceholderReplace("%TOTALAMOUNT%", String.valueOf(totalAmount))
                                .sendMessage(sender);
                        return;
                    }

                    int newAmount = hPlayer.getInt(DataType.GEMS);

                    MessageBuilder.create(doTake ? "cmdGemsAdminTake" : "cmdGemsAdminGive", hmc, "Gems")
                            .addPlaceholderReplace("%PLAYER%", hPlayer.getName())
                            .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                            .addPlaceholderReplace("%TOTALAMOUNT%", String.valueOf(newAmount))
                            .sendMessage(sender);

                    // message player if online
                    OfflinePlayer accountChanged = hPlayer.getBase();
                    if (accountChanged instanceof Player) {

                        MessageBuilder.create(doTake ? "cmdGemsAdminAccountChangedTake" : "cmdGemsAdminAccountChangedGive", hmc, "Gems")
                                .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                                .addPlaceholderReplace("%TOTALAMOUNT%", String.valueOf(newAmount))
                                .sendMessage((Player) accountChanged);
                    }

                    return;
                }
            }
        }

        sendGemsMessage();
    }

    private void sendGemsMessage() {

        if (hPlayer == null) {
            sendNotAPlayerMessage("Gems");
            return;
        }

        String prefix = hPlayer.getBase().equals(player) ? "Gems" : hPlayer.getName();
        int gems = hPlayer.getInt(DataType.GEMS);
        if (gems > 0) {
            MessageBuilder.create("cmdGemsShow", hmc, prefix)
                    .addPlaceholderReplace("%GEMS%", String.valueOf(gems))
                    .sendMessage(player);
        } else {
            MessageBuilder.create("cmdGemsHasNone", hmc, prefix).sendMessage(player);
        }
    }

    private boolean doAccountChange(int changeBy) {

        int currentAmount = hPlayer.getInt(DataType.GEMS);
        currentAmount += changeBy;
        if (currentAmount < 0) {
            return false;
        }

        hPlayer.set(DataType.GEMS, currentAmount != 0 ? currentAmount : null);

        MessageBuilder.create("cmdGemsLog", hmc)
                .addPlaceholderReplace("%PLAYER%", hPlayer.getName())
                .addPlaceholderReplace("%TOTALAMOUNT%", String.valueOf(currentAmount))
                .addPlaceholderReplace("%CHANGEDBY%", String.valueOf(changeBy))
                .logMessage(Level.INFO);
        return true;
    }
}
