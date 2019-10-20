package de.halfminer.hmh.cmd;

import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hms.util.MessageBuilder;

/**
 * - Start a game, at least two players must be added to the game.
 * - Will run custom commands defined in the config file once when the game starts.
 * - Will run custom commands to initialize all online players, players who are not online during the start will be initialized after their first join.
 *   - Configurable max health on game start.
 * - Will teleport all players to the specified spawn point, if their distance to it is higher than specified in the config file.
 * - Will add the starting time (specified in config file) to all players as their remaining time left.
 */
public class Cmdstart extends HaroCommand {

    public Cmdstart() {
        super("start");
    }

    @Override
    protected void execute() {

        if (haroStorage.isGameRunning()) {
            MessageBuilder.create("cmdStartGameIsRunning", hmh).sendMessage(sender);
            return;
        }

        if (haroStorage.getAddedPlayers(false).size() < 2) {
            MessageBuilder.create("cmdStartNotEnoughPlayers", hmh).sendMessage(sender);
            return;
        }

        // execute init commands
        for (String gameInitCommand : hmh.getHaroStorage().getHaroConfig().getGameInitCommands()) {
            server.dispatchCommand(server.getConsoleSender(), gameInitCommand);
        }

        int timeAtStart = haroStorage.getHaroConfig().getTimeAtStart();
        for (HaroPlayer haroPlayer : haroStorage.getAddedPlayers(false)) {
            haroPlayer.setTimeLeftSeconds(timeAtStart);
            if (haroPlayer.isOnline()) {
                hmh.getPlayerInitializer().initializePlayer(haroPlayer.getBase());
            }
        }

        haroStorage.setGameRunning(true);

        hmh.getTitleUpdateTask().updateTitles();
        MessageBuilder.create("cmdStartBroadcast", hmh).broadcastMessage(true);
    }
}
