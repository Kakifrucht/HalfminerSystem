package de.halfminer.hmh.tasks;

import de.halfminer.hmh.HaroClass;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.HaroStorage;
import de.halfminer.hms.util.Message;
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

            if (!haroStorage.isGameRunning() || haroStorage.isGameOver()) {
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
                        String kickMessage = Message.returnMessage("taskTimeCheckNoTimeLeft", hmh, false);
                        onlinePlayer.kickPlayer(kickMessage);
                    } else if (timeLeft <= notifyAtEverySecond || (timeLeft <= notifyAt && timeLeft % 5 == 0)) {
                        Message.create("taskTimeCheckCountdown", hmh)
                                .addPlaceholder("TIMELEFT", timeLeft)
                                .send(onlinePlayer);
                    }
                }
            }
        }, 20L, 20L);
    }
}
