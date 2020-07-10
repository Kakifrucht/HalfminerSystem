package de.halfminer.hml.cmd;

import de.halfminer.hml.land.FlyBoard;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.Message;
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
            Message.create("cmdFlyDisable", hml)
                    .addPlaceholder("%TIME%", flyBoard.getFlyTimeLeft(player))
                    .send(player);
        } else {

            if (player.isFlying()
                    || player.getAllowFlight()
                    || player.getGameMode().equals(GameMode.CREATIVE)
                    || player.getGameMode().equals(GameMode.SPECTATOR)) {
                Message.create("cmdFlyAlreadyFlying", hml).send(player);
                return;
            }

            Land land = board.getLandAt(player);
            if (flyBoard.hasFlyPermission(player, land)) {

                // player must stand still to start flying
                if (player.getVelocity().length() > 0.1d) {
                    Message.create("cmdFlyNotStandingStill", hml).send(player);
                    return;
                }

                boolean flyEnabled = flyBoard.togglePlayerFlying(player);
                if (flyEnabled) {
                    Message.create("cmdFlyEnable", hml)
                            .addPlaceholder("%TIME%", flyBoard.getFlyTimeLeft(player))
                            .send(player);
                } else {
                    Message.create("cmdFlyNotEnoughMoney", hml)
                            .addPlaceholder("%COST%", flyBoard.getCost())
                            .addPlaceholder("%TIME%", flyBoard.getFlyDurationSeconds())
                            .send(player);
                }

            } else {
                Message.create("cmdFlyNoPermission", hml).send(player);
            }
        }
    }
}
