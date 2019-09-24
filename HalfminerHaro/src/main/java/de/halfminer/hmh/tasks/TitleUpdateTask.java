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
        server.getScheduler().runTaskTimer(hmh, this::updateTitles, 0, intervalSeconds * 20L);
    }

    public void updateTitles() {
        int playersOnline = server.getOnlinePlayers().size();
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
