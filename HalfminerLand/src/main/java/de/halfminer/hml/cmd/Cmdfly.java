package de.halfminer.hml.cmd;

import de.halfminer.hml.land.FlyBoard;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.GameMode;

public class Cmdfly extends LandCommand {


    public Cmdfly() {
        super("fly");
    }

    @Override
    protected void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        if (!server.getAllowFlight()) {
            MessageBuilder.create("cmdFlyServerDisabled", hml).sendMessage(player);
            return;
        }

        FlyBoard flyBoard = board.getFlyBoard();
        if (flyBoard.isPlayerFlying(player)) {
            flyBoard.togglePlayerFlying(player);
            MessageBuilder.create("cmdFlyDisable", hml)
                    .addPlaceholderReplace("%TIME%", flyBoard.getFlyTimeLeft(player))
                    .sendMessage(player);
        } else {

            if (player.isFlying()
                    || player.getAllowFlight()
                    || player.getGameMode().equals(GameMode.CREATIVE)
                    || player.getGameMode().equals(GameMode.SPECTATOR)) {
                MessageBuilder.create("cmdFlyAlreadyFlying", hml).sendMessage(player);
                return;
            }

            Land land = board.getLandAt(player);
            if (land.hasOwner() && land.hasPermission(player)) {

                boolean flyEnabled = board.getFlyBoard().togglePlayerFlying(player);
                if (flyEnabled) {
                    MessageBuilder.create("cmdFlyEnable", hml)
                            .addPlaceholderReplace("%TIME%", flyBoard.getFlyTimeLeft(player))
                            .sendMessage(player);
                } else {
                    MessageBuilder.create("cmdFlyNotEnoughMoney", hml)
                            .addPlaceholderReplace("%COST%", flyBoard.getCost())
                            .addPlaceholderReplace("%TIME%", flyBoard.getFlyDurationSeconds())
                            .sendMessage(player);
                }

            } else {
                MessageBuilder.create("cmdFlyNoPermission", hml).sendMessage(player);
            }
        }
    }
}
