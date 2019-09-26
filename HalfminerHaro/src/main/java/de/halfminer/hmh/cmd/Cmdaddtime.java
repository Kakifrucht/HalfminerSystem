package de.halfminer.hmh.cmd;

import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

public class Cmdaddtime extends HaroCommand {

    public Cmdaddtime() {
        super("addtime");
    }

    @Override
    protected void execute() {

        if (!haroStorage.isGameRunning()) {
            MessageBuilder.create("cmdAddtimeNotRunning", hmh).sendMessage(sender);
            return;
        }

        if (args.length < 1) {
            sendUsage();
            return;
        }

        List<HaroPlayer> playersToUpdate = new ArrayList<>();
        int timeToAdd;

        if (args[0].equalsIgnoreCase("-day")) {
            playersToUpdate = haroStorage.getAddedPlayers(true);
            timeToAdd = haroStorage.getHaroConfig().getTimePerDay();
        } else /* set to specific amount, requires one more argument */ {

            if (args.length < 2) {
                sendUsage();
                return;
            }

            try {
                timeToAdd = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendUsage();
                return;
            }

            if (args[0].equalsIgnoreCase("-all")) {
                playersToUpdate = haroStorage.getAddedPlayers(true);
            } else /* add to specific player */ {
                try {
                    HalfminerPlayer hPlayer = hms.getStorageHandler().getPlayer(args[0]);
                    HaroPlayer haroPlayer = haroStorage.getHaroPlayer(hPlayer);
                    if (!haroPlayer.isAdded()) {
                        MessageBuilder.create("cmdAddtimeNotAdded", hmh).sendMessage(sender);
                        return;
                    }

                    if (haroPlayer.isDead()) {
                        MessageBuilder.create("cmdAddtimeDead", hmh).sendMessage(sender);
                        return;
                    }

                    playersToUpdate.add(haroPlayer);

                } catch (PlayerNotFoundException e) {
                    e.sendNotFoundMessage(sender, "Haro");
                    return;
                }
            }
        }

        int maxTotalTime = haroStorage.getHaroConfig().getMaxTime();
        for (HaroPlayer haroPlayer : playersToUpdate) {

            if (haroPlayer.isDead()) {
                continue;
            }

            int timeToSet = Math.min(haroPlayer.getTimeLeftSeconds() + timeToAdd, maxTotalTime);
            timeToSet = Math.max(0, timeToSet);
            haroPlayer.setTimeLeftSeconds(timeToSet);

            if (haroPlayer.isOnline()) {
                MessageBuilder.create("cmdAddtimePlayerMessage", hmh)
                        .addPlaceholder("NEWTIMEMINUTES", timeToSet / 60)
                        .sendMessage(haroPlayer.getBase().getPlayer());
            }

            if (playersToUpdate.size() == 1) {
                MessageBuilder.create("cmdAddtimePlayer", hmh)
                        .addPlaceholder("PLAYER", haroPlayer.getName())
                        .addPlaceholder("SECONDS", timeToSet)
                        .sendMessage(sender);
            }
        }

        if (playersToUpdate.size() > 1) {
            MessageBuilder.create("cmdAddtimeAll", hmh)
                    .addPlaceholder("SECONDS", timeToAdd)
                    .addPlaceholder("AMOUNT", playersToUpdate.size())
                    .sendMessage(sender);
        }

        hmh.getTitleUpdateTask().updateTitles();
    }

    private void sendUsage() {
        MessageBuilder.create("cmdAddtimeUsage", hmh).sendMessage(sender);
    }
}