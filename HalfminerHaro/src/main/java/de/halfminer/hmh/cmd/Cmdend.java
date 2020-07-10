package de.halfminer.hmh.cmd;

import de.halfminer.hmh.HealthManager;
import de.halfminer.hms.util.Message;
import org.bukkit.entity.Player;

/**
 * - End the currently running game, by resetting it's state and clearing all added players.
 * - Will print a warning, if more than one player is still in the game.
 */
public class Cmdend extends HaroCommand {

    public Cmdend() {
        super("end");
    }

    @Override
    protected void execute() {

        if (!haroStorage.isGameRunning()) {
            Message.create("cmdEndNotRunning", hmh).send(sender);
            return;
        }

        boolean isGameOver = haroStorage.isGameOver();
        if (isGameOver || (args.length > 0 && args[0].equalsIgnoreCase("-force"))) {

            haroStorage.setGameRunning(false);
            haroStorage.removeAllPlayers();

            HealthManager healthManager = hmh.getHealthManager();
            for (Player onlinePlayer : server.getOnlinePlayers()) {

                if (!onlinePlayer.hasPermission("hmh.admin")) {
                    healthManager.resetPlayerHealth(onlinePlayer);

                    String kickMessage = Message.returnMessage("cmdEndPlayerKick", hmh, false);
                    onlinePlayer.kickPlayer(kickMessage);
                }
            }

            hmh.getLogger().info("The game has finished and was successfully reset");
            Message.create("cmdEndSuccess", hmh).send(sender);

        } else {
            Message.create("cmdEndPromptForce", hmh).send(sender);
        }
    }
}
