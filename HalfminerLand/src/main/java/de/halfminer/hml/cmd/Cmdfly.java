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
            if (flyBoard.hasFlyPermission(player, land)) {

                // player must stand still to start flying
                if (player.getVelocity().length() > 0.1d) {
                    MessageBuilder.create("cmdFlyNotStandingStill", hml).sendMessage(player);
                    return;
                }

                boolean flyEnabled = flyBoard.togglePlayerFlying(player);
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
