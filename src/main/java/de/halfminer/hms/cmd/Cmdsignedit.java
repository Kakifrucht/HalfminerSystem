package de.halfminer.hms.cmd;

import de.halfminer.hms.cmd.abs.HalfminerPersistenceCommand;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
    private String lineText;

    // Sign copy data
    private int amountToCopy;
    private String[] signToBeCopied;

    public Cmdsignedit() {
        this.permission = "hms.signedit";
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

            MessageBuilder.create(hms, "cmdSigneditCopy", PREFIX)
                    .addPlaceholderReplace("%AMOUNT%", String.valueOf(amountToCopy))
                    .sendMessage(player);
            setPersistent(PersistenceMode.EVENT_PLAYER_INTERACT, player.getUniqueId());
            return;

        } else {

            try {
                lineNumber = Integer.parseInt(args[0]) - 1;
                if (lineNumber >= 0 && lineNumber < 4) {
                    String setTo = "";
                    if (args.length > 1) {
                        setTo = Utils.arrayToString(args, 1, true);
                        if (setTo.length() > 15) setTo = setTo.substring(0, 15); // truncate if necessary
                    }

                    this.lineText = setTo;

                    MessageBuilder.create(hms, "cmdSigneditSet", PREFIX)
                            .addPlaceholderReplace("%LINE%", String.valueOf(lineNumber + 1))
                            .addPlaceholderReplace("%TEXT%", setTo)
                            .sendMessage(player);
                    setPersistent(PersistenceMode.EVENT_PLAYER_INTERACT, player.getUniqueId());
                    return;
                } else {
                    showUsage();
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }

        showUsage();
    }

    @Override
    public boolean execute(Event e) {

        Player player = getPersistencePlayer();

        boolean isDone = false;
        PlayerInteractEvent interactEvent = (PlayerInteractEvent) e;

        Block block = interactEvent.getClickedBlock();
        if (block != null && (block.getType() == Material.SIGN
                        || block.getType() == Material.SIGN_POST
                        || block.getType() == Material.WALL_SIGN)) {

            Sign sign = (Sign) block.getState();

            if (amountToCopy > 0 && signToBeCopied == null) {
                signToBeCopied = sign.getLines();
                MessageBuilder.create(hms, "cmdSigneditSignCopied", PREFIX).sendMessage(player);
            } else {

                if (signToBeCopied != null) {
                    for (int i = 0; i < 4; i++) sign.setLine(i, signToBeCopied[i]);
                    if (--amountToCopy == 0) isDone = true;
                    MessageBuilder.create(hms, "cmdSigneditSignPasted", PREFIX)
                            .addPlaceholderReplace("%AMOUNT%", String.valueOf(amountToCopy))
                            .sendMessage(player);
                } else {
                    sign.setLine(lineNumber, lineText);
                    MessageBuilder.create(hms, "cmdSigneditLinePasted", PREFIX).sendMessage(player);
                    isDone = true;
                }

                sign.update();
            }
            interactEvent.setCancelled(true);
        }
        return isDone;
    }

    @Override
    public void onDisable() {
    }

    private void showUsage() {
        MessageBuilder.create(hms, "cmdSigneditUsage", PREFIX).sendMessage(player);
    }
}
