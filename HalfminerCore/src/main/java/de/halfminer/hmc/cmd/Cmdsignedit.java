package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerPersistenceCommand;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * - Copy signs, define copy amount
 * - Edit signs, define line number
 */
@SuppressWarnings("unused")
public class Cmdsignedit extends HalfminerPersistenceCommand {

    private final static String PREFIX = "Signedit";

    // Single line editing data
    private int lineNumber;
    private String lineText = "";

    // Sign copy data
    private int amountToCopy;
    private String[] signToBeCopied;

    public Cmdsignedit() {
        this.permission = "hmc.signedit";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage(PREFIX);
            return;
        }

        if (args.length <= 0) {
            showUsage();
            return;
        }

        if (args[0].equalsIgnoreCase("copy")) {

            amountToCopy = 1;

            if (args.length > 1) {
                try {
                    amountToCopy = Byte.parseByte(args[1]);
                    if (amountToCopy > 9 || amountToCopy < 1) amountToCopy = 1;
                } catch (NumberFormatException ignored) {
                }
            }

            MessageBuilder.create("cmdSigneditCopy", hmc, PREFIX)
                    .addPlaceholder("%AMOUNT%", amountToCopy)
                    .sendMessage(player);
            setPersistent(player.getUniqueId());
            return;

        } else {

            try {
                lineNumber = Integer.parseInt(args[0]) - 1;
                if (lineNumber >= 0 && lineNumber < 4) {

                    if (args.length > 1) {
                        lineText = Utils.arrayToString(args, 1, true);
                    }

                    MessageBuilder.create("cmdSigneditSet", hmc, PREFIX)
                            .addPlaceholder("%LINE%", lineNumber + 1)
                            .addPlaceholder("%TEXT%", lineText)
                            .sendMessage(player);
                    setPersistent(player.getUniqueId());
                    return;
                } else {
                    showUsage();
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }

        showUsage();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignInteract(PlayerInteractEvent e) {

        Player player = e.getPlayer();
        if (!isPersistenceOwner(e.getPlayer()))
            return;

        boolean isDone = false;

        Block block = e.getClickedBlock();
        if (block != null
                && /* block.getType().toString().endsWith("_SIGN") */block.getState() instanceof Sign) {

            Sign sign = (Sign) block.getState();

            if (amountToCopy > 0 && signToBeCopied == null) {
                signToBeCopied = sign.getLines();
                MessageBuilder.create("cmdSigneditSignCopied", hmc, PREFIX).sendMessage(player);
            } else {

                if (signToBeCopied != null) {
                    for (int i = 0; i < 4; i++) sign.setLine(i, signToBeCopied[i]);
                    if (--amountToCopy == 0) isDone = true;
                    MessageBuilder.create("cmdSigneditSignPasted", hmc, PREFIX)
                            .addPlaceholder("%AMOUNT%", amountToCopy)
                            .sendMessage(player);
                } else {
                    sign.setLine(lineNumber, lineText);
                    String messageKey = lineText.length() > 15 ? "cmdSigneditLinePastedWarn" : "cmdSigneditLinePasted";
                    MessageBuilder.create(messageKey, hmc, PREFIX).sendMessage(player);
                    isDone = true;
                }

                sign.update();
            }
            e.setCancelled(true);
        }

        if (isDone) {
            unregisterClass();
        }
    }

    private void showUsage() {
        MessageBuilder.create("cmdSigneditUsage", hmc, PREFIX).sendMessage(player);
    }
}
