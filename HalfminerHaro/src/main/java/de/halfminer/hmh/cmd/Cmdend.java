package de.halfminer.hmh.cmd;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.entity.Player;

public class Cmdend extends HaroCommand {

    public Cmdend() {
        super("end");
    }

    @Override
    protected void execute() {

        if (!haroStorage.isGameRunning()) {
            MessageBuilder.create("cmdEndNotRunning", hmh).sendMessage(sender);
            return;
        }

        boolean isGameOver = haroStorage.isGameOver();
        if (isGameOver || (args.length > 0 && args[0].equalsIgnoreCase("-force"))) {

            haroStorage.setGameRunning(false);
            haroStorage.removeAllPlayers();

            for (Player onlinePlayer : server.getOnlinePlayers()) {
                if (!onlinePlayer.hasPermission("hmh.admin")) {
                    String kickMessage = MessageBuilder.returnMessage("cmdEndPlayerKick", hmh, false);
                    onlinePlayer.kickPlayer(kickMessage);
                }
            }

            hmh.getLogger().info("The game has finished and was successfully reset");
            MessageBuilder.create("cmdEndSuccess", hmh).sendMessage(sender);

        } else {
            MessageBuilder.create("cmdEndPromptForce", hmh).sendMessage(sender);
        }
    }
}
