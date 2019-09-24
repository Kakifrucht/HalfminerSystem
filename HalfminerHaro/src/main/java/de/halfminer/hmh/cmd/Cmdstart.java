package de.halfminer.hmh.cmd;

import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hms.util.MessageBuilder;

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
                haroStorage.initializePlayer(haroPlayer.getBase());
            }
        }

        haroStorage.setGameRunning(true);

        hmh.getTitleUpdateTask().updateTitles();
        MessageBuilder.create("cmdStartBroadcast", hmh).broadcastMessage(true);
    }
}
