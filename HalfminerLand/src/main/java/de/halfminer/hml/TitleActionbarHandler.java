package de.halfminer.hml;

import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.land.Land;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class TitleActionbarHandler extends LandClass {

    private final LandStorage landStorage = hml.getLandStorage();

    Map<Player, BukkitTask> tasks = new HashMap<>();
    Map<Player, String> titles = new HashMap<>();


    public void updateLocation(Player player, Land land) {

        String newTitle = null;
        if (land.hasTitle()) {
            newTitle = land.getTitle();
        } else if (land.hasOwner()) {
            LandPlayer landPlayer = landStorage.getLandPlayer(land.getOwner());
            if (landPlayer.hasTitle()) {
                newTitle = landPlayer.getTitle();
            }
        }

        if (tasks.containsKey(player)) {

            if (newTitle == null) {
                hms.getTitlesHandler().sendActionBar(player, " ");
            }

            String previousTitle = titles.get(player);
            if (previousTitle.equals(newTitle)) {
                return;
            }

            tasks.remove(player).cancel();
        }

        if (newTitle == null) {
            return;
        }

        final String newTitleFinal = newTitle;
        BukkitTask refreshTask = server.getScheduler().runTaskTimer(hml,
                () -> hms.getTitlesHandler().sendActionBar(player, newTitleFinal, 10),
                0L, 100L
        );

        tasks.put(player, refreshTask);
        titles.put(player, newTitle);
    }

    public void playerLeft(Player player) {
        if (tasks.containsKey(player)) {
            tasks.remove(player).cancel();
            titles.remove(player);
        }
    }
}
