package de.halfminer.hmh.tasks;

import de.halfminer.hmh.HaroClass;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.HaroStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Class spawns a scheduled task that runs every second and checks if a joined player still has time left.
 * Also handles a chat countdown to notify a player that the time is running out.
 * Immediately returns if game is not running.
 */
public class TimeCheckTask extends HaroClass {

    public TimeCheckTask() {
        super(false);

        HaroStorage haroStorage = hmh.getHaroStorage();
        server.getScheduler().runTaskTimer(hmh, () -> {

            if (!haroStorage.isGameRunning()) {
                return;
            }

            List<Player> onlinePlayersCopy = new ArrayList<>(server.getOnlinePlayers());
            for (Player onlinePlayer : onlinePlayersCopy) {
                HaroPlayer haroPlayer = haroStorage.getHaroPlayer(onlinePlayer);
                if (haroPlayer.isAdded()) {
                    int timeLeft = haroPlayer.getTimeLeftSeconds();
                    int notifyAt = haroStorage.getHaroConfig().getTimeLeftNotify();
                    int notifyAtEverySecond = 5;

                    if (timeLeft <= 0) {
                        String kickMessage = MessageBuilder.returnMessage("taskTimeCheckNoTimeLeft", hmh, false);
                        onlinePlayer.kickPlayer(kickMessage);
                    } else if (timeLeft <= notifyAtEverySecond || (timeLeft <= notifyAt && timeLeft % 5 == 0)) {
                        MessageBuilder.create("taskTimeCheckCountdown", hmh)
                                .addPlaceholder("TIMELEFT", timeLeft)
                                .sendMessage(onlinePlayer);
                    }
                }
            }
        }, 20L, 20L);
    }
}
