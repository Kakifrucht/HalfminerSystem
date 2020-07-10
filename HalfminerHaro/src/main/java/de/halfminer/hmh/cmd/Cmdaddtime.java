package de.halfminer.hmh.cmd;

import de.halfminer.hmh.HalfminerHaro;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hms.util.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * - Add play time to either all players or a specific player.
 * - When using /haro addtime -day, all players will get the time in seconds specified in config.
 *   - Set this command to execute whenever a new day is starting via some external task scheduler.
 * - To remove time from a player, use a negative value.
 * - Players who are online will receive a notification that their playtime was changed.
 * - Will take into account the maximum time a player can accumulate.
 */
public class Cmdaddtime extends HaroCommand {

    public Cmdaddtime() {
        super("addtime");
    }

    @Override
    protected void execute() {

        if (!haroStorage.isGameRunning()) {
            Message.create("cmdAddtimeNotRunning", hmh).send(sender);
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

                HaroPlayer haroPlayer;
                try {
                    haroPlayer = haroStorage.getHaroPlayer(args[0]);
                } catch (IllegalArgumentException e) {
                    Message.create("playerDoesNotExist", HalfminerHaro.MESSAGE_PREFIX).send(sender);
                    return;
                }

                if (!haroPlayer.isAdded()) {
                    Message.create("cmdAddtimeNotAdded", hmh).send(sender);
                    return;
                }

                if (haroPlayer.isEliminated()) {
                    Message.create("cmdAddtimeEliminated", hmh).send(sender);
                    return;
                }

                playersToUpdate.add(haroPlayer);
            }
        }

        int maxTotalTime = haroStorage.getHaroConfig().getMaxTime();
        for (HaroPlayer haroPlayer : playersToUpdate) {

            if (haroPlayer.isEliminated()) {
                continue;
            }

            int currentTimeLeft = haroPlayer.getTimeLeftSeconds();
            if (timeToAdd > 0 && currentTimeLeft >= maxTotalTime && playersToUpdate.size() > 1) {
                hmh.getLogger().info("Player " + haroPlayer.getName() + " already has the maximum time of " + maxTotalTime + " seconds");
                continue;
            }

            int timeToSet = Math.min(currentTimeLeft + timeToAdd, maxTotalTime);
            timeToSet = Math.max(0, timeToSet);
            haroPlayer.setTimeLeftSeconds(timeToSet);

            if (haroPlayer.isOnline()) {
                Message.create("cmdAddtimePlayerMessage", hmh)
                        .addPlaceholder("NEWTIMEMINUTES", timeToSet / 60)
                        .send(haroPlayer.getBase().getPlayer());
            }

            if (playersToUpdate.size() == 1) {
                Message.create("cmdAddtimePlayer", hmh)
                        .addPlaceholder("PLAYER", haroPlayer.getName())
                        .addPlaceholder("SECONDS", timeToSet)
                        .send(sender);
            }
        }

        if (playersToUpdate.size() > 1) {
            Message.create("cmdAddtimeAll", hmh)
                    .addPlaceholder("SECONDS", timeToAdd)
                    .addPlaceholder("AMOUNT", playersToUpdate.size())
                    .send(sender);
        }

        hmh.getTitleUpdateTask().updateTitles();
    }

    private void sendUsage() {
        Message.create("cmdAddtimeUsage", hmh).send(sender);
    }
}
