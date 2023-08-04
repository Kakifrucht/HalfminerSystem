package de.halfminer.hml.cmd;

import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;

public class Cmdtitle extends LandCommand {


    public Cmdtitle() {
        super("title");
    }

    @Override
    protected void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        if (args.length < 2) {
            sendUsage();
            return;
        }

        boolean canSetOthers = player.hasPermission("hml.cmd.title.others");
        if (canSetOthers && args[0].equalsIgnoreCase("clearall")) {
            HalfminerPlayer halfminerPlayer;
            LandPlayer landPlayer;
            try {
                halfminerPlayer = hms.getStorageHandler().getPlayer(args[1]);
                landPlayer = hml.getLandStorage().getLandPlayer(halfminerPlayer);
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(player, "Land");
                return;
            }

            if (!landPlayer.hasTitle()) {
                Message.create("cmdTitleClearAllNoTitle", hml)
                        .addPlaceholder("%PLAYER%", halfminerPlayer.getName())
                        .send(player);
                return;
            }

            landPlayer.setTitle(null);

            Message.create("cmdTitleClearAllSuccess", hml)
                    .addPlaceholder("%PLAYER%", halfminerPlayer.getName())
                    .send(player);
            return;
        }

        String title = Utils.arrayToString(args, 1, args.length, false);
        if (!player.hasPermission("hml.cmd.title.formatting")) {
            title = title.replaceAll("(&[k-or])(?!.*\\1)", "");
        }

        title = ChatColor.translateAlternateColorCodes('&', title);

        boolean clear = title.equalsIgnoreCase("-clear");
        if (clear) {
            title = null;
        }

        Land land = board.getLandAt(player);
        if (args[0].equalsIgnoreCase("all")) {
            LandPlayer landPlayer = hml.getLandStorage().getLandPlayer(player);
            landPlayer.setTitle(title);
            Message.create("cmdTitleSetAll" + (clear ? "Clear" : ""), hml).send(player);
        } else if (args[0].equalsIgnoreCase("chunk")) {
            if (land.isOwner(player) || canSetOthers) {
                land.setTitle(title);
                Message.create("cmdTitleSet" + (clear ? "Clear" : ""), hml).send(player);
            } else {
                Message.create("landNotOwned", hml).send(player);
                return;
            }
        } else {
            sendUsage();
            return;
        }

        hml.getTitleActionbarHandler().updateLocation(player, land);
    }

    private void sendUsage() {
        Message.create("cmdTitleUsage", hml, "Land").send(player);
    }
}
