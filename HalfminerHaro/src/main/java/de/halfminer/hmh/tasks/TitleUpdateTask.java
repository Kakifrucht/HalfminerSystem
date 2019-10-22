package de.halfminer.hmh.tasks;

import de.halfminer.hmh.HaroClass;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.HaroStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.entity.Player;

/**
 * Updates the players tab titles with the current player count and time left.
 * {@link #updateTitles()} should be called whenever the playercount changes, or the specified a player has left.
 */
public class TitleUpdateTask extends HaroClass {

    private final HaroStorage haroStorage;

    public TitleUpdateTask() {
        super(false);

        haroStorage = hmh.getHaroStorage();
        final long intervalSeconds = 15;
        server.getScheduler().runTaskTimer(hmh, (Runnable) this::updateTitles, 0, intervalSeconds * 20L);
    }

    /**
     * Overload for {@link #updateTitles(boolean)}, with parameter set to false
     */
    public void updateTitles() {
        updateTitles(false);
    }

    /**
     * Update the tab titles for all players.
     *
     * @param playerLeft set to true if a player logout triggered the refresh to substract one
     *                   from the total player count. This is necessary, as the player who logged
     *                   out still counts as online during the logout event.
     */
    public void updateTitles(boolean playerLeft) {

        int playersOnline = server.getOnlinePlayers().size();
        if (playerLeft) {
            playersOnline--;
        }

        boolean isGameRunning = haroStorage.isGameRunning();
        for (Player onlinePlayer : server.getOnlinePlayers()) {
            HaroPlayer haroPlayer = haroStorage.getHaroPlayer(onlinePlayer);

            int timeLeftMinutes = haroPlayer.getTimeLeftSeconds() / 60;
            String timeLeftString = (isGameRunning && haroPlayer.isAdded()) ? String.valueOf(timeLeftMinutes) : "-";

            String titleMessage = MessageBuilder.create("taskTitleUpdateTitle" + (isGameRunning ? "" : "NotStarted"), hmh)
                    .togglePrefix()
                    .addPlaceholder("PLAYERCOUNT", playersOnline)
                    .addPlaceholder("MINUTES", timeLeftString)
                    .returnMessage();

            hms.getTitlesHandler().setTablistHeaderFooter(onlinePlayer, titleMessage);
        }
    }
}
