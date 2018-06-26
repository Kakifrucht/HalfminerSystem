package de.halfminer.hml.cmd;

import de.halfminer.hml.land.FlyBoard;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.MessageBuilder;

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

        FlyBoard flyBoard = board.getFlyBoard();
        if (flyBoard.isPlayerFlying(player)) {
            flyBoard.togglePlayerFlying(player);
            MessageBuilder.create("cmdFlyDisable", hml)
                    .addPlaceholderReplace("%TIME%", flyBoard.getFlyTimeLeft(player))
                    .sendMessage(player);
        } else {

            Land land = board.getLandAt(player);
            if (land.isOwner(player)) {

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
                MessageBuilder.create("cmdFlyNotOwned", hml).sendMessage(player);
            }
        }
    }
}
